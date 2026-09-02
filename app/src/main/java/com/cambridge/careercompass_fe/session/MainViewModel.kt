package com.cambridge.careercompass_fe.session

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cambridge.core.common.reporting.ErrorReporter
import com.cambridge.core.domain.repository.AuthRepository
import com.cambridge.core.domain.repository.UserProfileRepository
import com.cambridge.core.domain.usecase.auth.ResolveSessionEntryUseCase
import com.cambridge.core.domain.usecase.auth.SessionEntryDestination
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
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
 */
@HiltViewModel
public class MainViewModel
    @Inject
    constructor(
        private val authRepository: AuthRepository,
        private val userProfileRepository: UserProfileRepository,
        private val resolveSessionEntry: ResolveSessionEntryUseCase,
        private val errorReporter: ErrorReporter,
    ) : ViewModel() {
        private val _launch = MutableStateFlow<AppShellLaunch?>(null)

        /** 초기 진입 시 null(로딩)이며, 세션·프로필 확인 뒤 확정된다. */
        public val launch: StateFlow<AppShellLaunch?> = _launch.asStateFlow()

        // 프로세스마다 다른 시작값 — 이전 프로세스의 NavController 저장 상태와 키가 겹치지 않는다.
        private var revision: Long = System.nanoTime()
        private var resolveJob: Job? = null
        private var profileVerifyJob: Job? = null

        init {
            refresh()
        }

        /**
         * 로그아웃·세션 만료 뒤 시작 목적지를 다시 계산하고 NavHost 를 새로 만들게 한다.
         *
         * 계산이 진행 중이면 합류한다 — 여러 화면이 같은 세션 종료를 동시에 알려도 초기화는 한 번이고, 계산이
         * 겹치지 않으니 늦게 끝난 옛 결과가 새 결과를 덮어쓰지도 않는다. 같은 이유로 새 계산이 시작되면 이전
         * 백그라운드 프로필 확인은 버린다 — 옛 세션의 결과가 새 목적지를 뒤집지 않는다.
         */
        public fun refresh() {
            if (resolveJob?.isActive == true) return
            profileVerifyJob?.cancel()
            resolveJob =
                viewModelScope.launch {
                    val resolution = resolve()
                    emit(resolution.destination)
                    if (resolution.verifyProfile) verifyProfileBehindMain()
                }
        }

        /** 새 시작 목적지를 흘린다. revision 이 올라 NavHost 가 새로 만들어진다. */
        private fun emit(destination: AppStartDestination) {
            revision += 1
            _launch.value = AppShellLaunch(revision = revision, destination = destination)
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
            return entry.destination.toStartDestination()
        }

        /**
         * 캐시로 메인에 들어간 뒤 서버와 한 번 맞춰 본다.
         *
         * - 서버가 확정했고 온보딩 미완료거나 세션이 끝났으면 다시 계산한다(use case 가 401 에서 로컬 세션을 이미
         *   정리했다). 피드가 잠깐 보였다가 온보딩·로그인으로 바뀌는데, 서버가 완료를 되돌렸거나 세션이 만료된 드문
         *   경우라 허용한다 — 캐시를 믿고 먼저 들어가는 대가다.
         * - 서버 확인 자체가 실패해 캐시로 판단한 결과([SessionEntry.fallbackCause])면 기록만 남기고 목적지를 유지한다.
         *   이미 그 캐시로 들어와 있으므로 화면을 흔들 이유가 없다(오프라인 시작).
         *
         * 다시 계산을 부르면 [refresh] 가 이 잡을 취소하지만 남은 작업이 없어 무해하다.
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
                        emit(entry.destination.toStartDestination())
                    }
                }
        }

        private fun recordStartFailure(cause: Throwable) {
            errorReporter.recordFailure(cause, attributes = mapOf(KEY_STAGE to STAGE_START_PROFILE))
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
        }
    }
