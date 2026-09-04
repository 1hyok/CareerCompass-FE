package com.cambridge.careercompass_fe.session

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cambridge.careercompass_fe.navigation.AppDeepLink
import com.cambridge.core.common.reporting.ErrorReporter
import com.cambridge.core.common.reporting.recordStagedFailure
import com.cambridge.core.domain.repository.AuthRepository
import com.cambridge.core.domain.repository.UserProfileRepository
import com.cambridge.core.domain.settings.AppSettingsRepository
import com.cambridge.core.domain.usecase.auth.ResolveSessionEntryUseCase
import com.cambridge.core.domain.usecase.auth.SessionEntry
import com.cambridge.core.domain.usecase.auth.SessionEntryDestination
import com.cambridge.core.model.settings.ThemeMode
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * 시작 목적지 결정 — 기능 스펙 F1-1.
 *
 * - 세션 없음 → [AppStartDestination.Login]
 * - 세션 있음 + 이 계정이 이 기기에서 지문 로그인을 켬 → [AppStartDestination.BiometricLogin] (지문 확인 뒤 세션 검증과
 *   온보딩/메인 분기는 지문 화면이 같은 [ResolveSessionEntryUseCase] 로 한다)
 * - 세션 있음 → 마지막으로 알려진 온보딩 완료 여부([UserProfileRepository.lastKnownOnboardingDone])로 **네트워크를
 *   기다리지 않고** 가른다. 완료면 [AppStartDestination.Main], 미완료면 [AppStartDestination.Onboarding] — 온보딩 진입
 *   판정이 서버를 다시 확인하므로 그 사이 완료된 사용자는 거기서 피드로 간다. 프로필도 힌트도 없을 때(세션은 있는데
 *   아무것도 모름)만 [ResolveSessionEntryUseCase] 를 기다린다.
 * - 캐시로 메인에 들어간 경우에만 같은 use case 를 백그라운드로 한 번 돌려 서버와 맞춰 본다([verifyProfileBehindMain]).
 *
 * 콜드 스타트의 시스템 스플래시는 이 값이 확정될 때까지 유지되므로, 시작 경로에 네트워크 왕복을 두지 않는 것이
 * 목적이다(#74). 지문 경로는 그렇지 않다 — 지문 성공은 저장된 세션을 그대로 쓰겠다는 확인이라 서버가 그 세션을 아직
 * 받아 주는지 **먼저** 확인해야 피드에 들어갔다가 401 로 튕겨 나오는 두 번 이동이 없다(#81).
 *
 * 결과는 [AppShellLaunch] 로 흘린다 — 세션 종료 뒤 같은 목적지가 나와도 [AppShellLaunch.revision] 이 올라 NavHost 가
 * 새로 만들어진다.
 *
 * 세션이 **왜** 끝났는지도 여기서 정한다(#128). 화면은 사실만 알려 주고([onSessionEnded]) 판정과 안내 여부는 셸이
 * 갖는다 — 만료로 로그인 화면에 닿았을 때만 [AppShellLaunch.sessionExpiryNotice] 를 켠다. 셸이 스스로 401 을 만나는
 * 자리(콜드 스타트의 세션 진입 판정·메인 뒤 백그라운드 확인)도 같은 만료다.
 */
@HiltViewModel
public class MainViewModel
    @Inject
    constructor(
        private val authRepository: AuthRepository,
        private val userProfileRepository: UserProfileRepository,
        private val resolveSessionEntry: ResolveSessionEntryUseCase,
        appSettingsRepository: AppSettingsRepository,
        private val errorReporter: ErrorReporter,
        private val savedStateHandle: SavedStateHandle,
    ) : ViewModel() {
        private val _launch = MutableStateFlow<AppShellLaunch?>(null)

        /** 초기 진입 시 null(로딩)이며, 세션·프로필 확인 뒤 확정된다. */
        public val launch: StateFlow<AppShellLaunch?> = _launch.asStateFlow()

        /**
         * 이 기기에서 고른 화면 테마(#210). 첫 값은 [ThemeMode.System] 이고 저장소를 읽는 즉시 갈린다.
         *
         * `Eagerly` 인 이유 — 구독은 첫 컴포지션에서 시작하는데, 그때 이미 값이 있어야 반대 테마가 한 프레임
         * 스쳤다 바뀌지 않는다. 시스템 스플래시가 [launch] 확정까지 화면을 붙들고 있고 그쪽은 세션·프로필을
         * 보므로, 같은 저장소 계열의 값 하나를 읽는 이 흐름이 늦을 일은 사실상 없다.
         */
        public val themeMode: StateFlow<ThemeMode> =
            appSettingsRepository.themeMode
                .stateIn(viewModelScope, SharingStarted.Eagerly, ThemeMode.System)

        private val _pendingDeepLink = MutableStateFlow<AppDeepLink?>(null)

        /**
         * 아직 적용하지 않은 딥링크(`careercompass://postings/{id}`) — `MainActivity` 가 intent 에서 파싱해 싣고,
         * `AppNavigation` 이 피드 그래프 안에서 이동한 뒤 [consumeDeepLink] 로 비운다. 로그인·온보딩 중에 들어온 것은
         * 인증을 마칠 때까지 여기 머문다.
         */
        public val pendingDeepLink: StateFlow<AppDeepLink?> = _pendingDeepLink.asStateFlow()

        /**
         * NavHost 세대 번호. 액티비티 저장 상태에 실려 프로세스를 건넌다.
         *
         * 예전에는 `System.nanoTime()` 으로 시작해 프로세스마다 달랐다 — 되살아난 앱이 이전 NavController 저장
         * 상태를 **영영 못 찾게** 만들어 백스택 복원을 막는 장치였다. 그런데 그 저장 상태에는 백스택뿐 아니라
         * 거기 매달린 그래프 스코프 `SavedStateHandle` 이 전부 들어 있다. 온보딩 입력 초안을 거기 남겨도
         * 프로세스가 죽으면 함께 버려져, 「중단된 단계부터 재개」가 단계만 지키고 입력은 못 지켰다(#133).
         *
         * 그래서 값은 살리고 **버려야 할 때만** 올린다 — [emit] 의 판정이 그것이다.
         */
        private var revision: Long = savedStateHandle[KEY_REVISION] ?: 0L
        private var resolveJob: Job? = null
        private var profileVerifyJob: Job? = null

        /** 아직 [emit] 이 소비하지 않은 세션 종료 사유. null 이면 세션이 끝난 적 없는 계산(콜드 스타트)이다. */
        private var pendingCause: SessionEndCause? = null

        init {
            refresh()
        }

        /** intent 의 딥링크를 보관한다. 계약에 맞지 않아 파싱이 null 이면 무시한다 — 보관 중인 것도 지우지 않는다. */
        public fun onDeepLink(link: AppDeepLink?) {
            if (link != null) _pendingDeepLink.value = link
        }

        public fun consumeDeepLink() {
            _pendingDeepLink.value = null
        }

        /**
         * 화면이 알려 온 세션 종료 — 사유를 싣고 시작 목적지를 다시 계산한다.
         *
         * 피드·상세·게시판의 401 은 [SessionEndCause.Expired], 마이 탭 로그아웃은 [SessionEndCause.LoggedOut] 이다.
         * 계산이 진행 중이면 합류하되 사유는 남아 이번 계산이 소비한다 — 두 화면이 같은 종료를 동시에 알려도
         * 초기화는 한 번이고, 늦게 도착한 401 이 방금 한 로그아웃을 만료로 바꾸지 않는다.
         */
        public fun onSessionEnded(cause: SessionEndCause) {
            reportCause(cause)
            refresh()
        }

        /**
         * 지문 확인 뒤 세션 검증이 만료를 알렸다 — 시작 목적지는 다시 계산하지 않고 안내만 켠다.
         *
         * 온보딩 그래프가 스스로 지문 화면을 걷어내고 로그인 화면으로 옮기므로 NavHost 를 새로 만들 이유가 없다.
         * 더 중요한 이유는 재계산이 이 경로를 가둘 수 있다는 것이다: 세션 정리가 실패해 토큰이 남은 기기에서는
         * 다시 계산해도 지문 화면이 나와 로그인 화면에 영영 닿지 못한다. 지문 화면 자체에는 알리지 않는다 — 그
         * 화면은 세션이 살아 있다고 믿고 뜬 자리라 그 시점엔 알릴 만료가 아직 없다.
         */
        public fun raiseSessionExpiryNotice() {
            _launch.update { it?.copy(sessionExpiryNotice = true) }
        }

        /**
         * 만료 안내를 끈다 — 닫기를 눌렀거나 다시 로그인을 시도했을 때.
         *
         * [AppShellLaunch.revision] 은 그대로라 NavHost 를 다시 만들지 않는다. 안내는 상태라 회전으로 사라지지도,
         * 두 번 뜨지도 않고, 여기로 꺼야 비로소 없어진다.
         */
        public fun consumeSessionExpiryNotice() {
            _launch.update { if (it?.sessionExpiryNotice == true) it.copy(sessionExpiryNotice = false) else it }
        }

        /**
         * 시작 목적지를 다시 계산하고 NavHost 를 새로 만들게 한다.
         *
         * 계산이 진행 중이면 합류한다 — 여러 화면이 같은 세션 종료를 동시에 알려도 초기화는 한 번이고, 계산이
         * 겹치지 않으니 늦게 끝난 옛 결과가 새 결과를 덮어쓰지도 않는다. 같은 이유로 새 계산이 시작되면 이전
         * 백그라운드 프로필 확인은 버린다 — 옛 세션의 결과가 새 목적지를 뒤집지 않는다.
         */
        private fun refresh() {
            if (resolveJob?.isActive == true) return
            profileVerifyJob?.cancel()
            resolveJob =
                viewModelScope.launch {
                    val resolution = resolve()
                    emit(resolution.destination)
                    if (resolution.verifyProfile) verifyProfileBehindMain()
                }
        }

        /**
         * 새 시작 목적지를 흘린다.
         *
         * [revision] 이 오르면 NavHost 가 새로 만들어지고 이전 백스택이 버려진다. 올리는 경우는 둘이다.
         * - **다시 계산**([launch] 가 이미 있다): 세션이 끝났다는 뜻이라 언제나 버린다. 목적지가 같아도(로그인 →
         *   만료 → 다시 로그인) 이전 백스택은 남으면 안 되는데, 목적지 값만 키로 쓰면 같은 값이라 아무 일도 없다.
         * - **콜드 스타트인데 인증이 다시 필요하다**([AppStartDestination.requiresAuthentication]): 되살아난
         *   백스택이 로그인·지문 게이트를 건너뛴다.
         *
         * 그 밖의 콜드 스타트(세션이 그대로인 온보딩·메인)는 **올리지 않는다** — 이전 프로세스가 남긴 NavController
         * 저장 상태를 그대로 찾아, 보던 화면과 거기 매달린 입력 초안이 함께 돌아온다(#133).
         *
         * 사유는 여기서 소비한다 — 만료로 끝난 세션이 **실제로 로그인 화면에 닿았을 때만** 안내가 붙는다. 만료를
         * 알려 왔어도 목적지가 로그인이 아니면(그 사이 다른 경로가 새 세션을 열었다) 설명할 일이 없고, 사유를
         * 남겨 두면 한참 뒤의 계산에 엉뚱하게 붙으므로 목적지와 무관하게 비운다.
         *
         * 첫 계산이 아니면 소비되지 않은 딥링크를 버린다 — 다른 계정으로 로그인해 남의 알림 공고가 열리지 않게.
         * 첫 계산([launch] 가 아직 null)에서는 지킨다: 앱이 뜨기 전에 받은 딥링크가 거기 있다.
         */
        private fun emit(destination: AppStartDestination) {
            if (_launch.value != null || destination.requiresAuthentication) revision += 1
            savedStateHandle[KEY_REVISION] = revision
            val cause = pendingCause
            pendingCause = null
            if (_launch.value != null) _pendingDeepLink.value = null
            _launch.value =
                AppShellLaunch(
                    revision = revision,
                    destination = destination,
                    sessionExpiryNotice = cause == SessionEndCause.Expired && destination == AppStartDestination.Login,
                )
        }

        /** 로그아웃이 이긴다 — 사용자가 끝낸 세션에 뒤늦게 돌아온 401 이 만료 안내를 붙이지 않는다. */
        private fun reportCause(cause: SessionEndCause) {
            if (pendingCause != SessionEndCause.LoggedOut) pendingCause = cause
        }

        private suspend fun resolve(): Resolution {
            if (!authRepository.isLoggedIn.first()) return Resolution(AppStartDestination.Login)
            if (authRepository.isBiometricEnabled.first()) return Resolution(AppStartDestination.BiometricLogin)
            return resolveAfterSession()
        }

        private suspend fun resolveAfterSession(): Resolution =
            when (userProfileRepository.lastKnownOnboardingDone()) {
                true -> Resolution(AppStartDestination.Main, verifyProfile = true)
                false -> Resolution(AppStartDestination.Onboarding)
                null -> Resolution(resolveBySessionEntry())
            }

        /** 세션은 있는데 프로필도 힌트도 없다 — 이때만 서버를 기다린다. */
        private suspend fun resolveBySessionEntry(): AppStartDestination {
            val entry = resolveSessionEntry()
            entry.fallbackCause?.let(::recordStartFailure)
            entry.reportSessionEnd()
            return entry.destination.toStartDestination()
        }

        /**
         * 셸이 직접 확인한 401 — 저장해 둔 세션이 서버에서 끝났다는 뜻이라 만료로 남긴다.
         *
         * [ResolveSessionEntryUseCase] 는 401 에서만 로그인을 돌려주고 로컬 세션도 함께 정리한다. 서버 확인이
         * 실패해 캐시로 판단한 경우는 로그인이 아니므로 여기 걸리지 않는다(오프라인 시작에 만료 안내를 띄우지 않는다).
         */
        private fun SessionEntry.reportSessionEnd() {
            if (destination == SessionEntryDestination.Login) reportCause(SessionEndCause.Expired)
        }

        /**
         * 캐시로 메인에 들어간 뒤 서버와 한 번 맞춰 본다.
         *
         * - 서버가 확정했고 온보딩 미완료거나 세션이 끝났으면 그 목적지를 그대로 반영한다(use case 가 401 에서 로컬
         *   세션을 이미 정리했다). 피드가 잠깐 보였다가 온보딩·로그인으로 바뀌는데, 서버가 완료를 되돌렸거나 세션이
         *   만료된 드문 경우라 허용한다 — 캐시를 믿고 먼저 들어가는 대가다.
         * - 서버 확인 자체가 실패해 캐시로 판단한 결과([SessionEntry.fallbackCause])면 기록만 남기고 목적지를 유지한다.
         *   이미 그 캐시로 들어와 있으므로 화면을 흔들 이유가 없다(오프라인 시작).
         */
        private fun verifyProfileBehindMain() {
            profileVerifyJob?.cancel()
            profileVerifyJob =
                viewModelScope.launch {
                    val entry = resolveSessionEntry()
                    val fallbackCause = entry.fallbackCause
                    if (fallbackCause != null) {
                        recordStartFailure(fallbackCause)
                        return@launch
                    }
                    // 서버가 확정한 답이므로 **다시 계산하지 않고 그대로 반영한다.**
                    //
                    // 여기서 refresh() 를 부르면 두 가지가 깨진다. (1) 재진입 가드가 이 호출을 삼킨다 —
                    // 이 코루틴은 resolveJob 본문 안에서 돌기 때문이다. (2) 삼키지 않게 고치면 무한 재귀가
                    // 된다: 서버가 「미완료」라 해도 로컬 캐시는 여전히 「완료」라 재계산이 또 메인으로 가고
                    // 또 확인을 건다. 0903 에 이 루프로 테스트 워커가 코루틴 1억 4천만 개를 만들며 CPU 를
                    // 태웠다. 캐시 갱신이 성공하면 멎지만 그 쓰기가 실패하면 앱이 영원히 돈다.
                    if (entry.destination != SessionEntryDestination.Feed) {
                        entry.reportSessionEnd()
                        emit(entry.destination.toStartDestination())
                    }
                }
        }

        /** 시작 프로필 조회 실패. 오프라인 콜드 스타트마다 나므로 공통 규칙에 맡겨 세션 표본만 남긴다. */
        private fun recordStartFailure(cause: Throwable) {
            errorReporter.recordStagedFailure(stageKey = KEY_STAGE, stage = STAGE_START_PROFILE, throwable = cause)
        }

        private fun SessionEntryDestination.toStartDestination(): AppStartDestination =
            when (this) {
                SessionEntryDestination.Login -> AppStartDestination.Login
                SessionEntryDestination.Onboarding -> AppStartDestination.Onboarding
                SessionEntryDestination.Feed -> AppStartDestination.Main
            }

        /** 시작 목적지와, 캐시로 확정해 서버 확인이 아직 남았는지. */
        private data class Resolution(
            val destination: AppStartDestination,
            val verifyProfile: Boolean = false,
        )

        private companion object {
            const val KEY_STAGE = "app_stage"
            const val STAGE_START_PROFILE = "start_profile"

            /** 액티비티 저장 상태에 실리는 [revision] 키 — 프로세스를 건너 NavHost 세대를 잇는다. */
            const val KEY_REVISION = "app_shell.navHostRevision"
        }
    }
