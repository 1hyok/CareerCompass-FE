package com.cambridge.careercompass_fe.session

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cambridge.core.common.reporting.ErrorReporter
import com.cambridge.core.domain.error.CoreDataFailure
import com.cambridge.core.domain.repository.AuthRepository
import com.cambridge.core.domain.repository.UserProfileRepository
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
 * - 세션 있음 + 지문 로그인 켬 → [AppStartDestination.BiometricLogin] (지문 확인 뒤 온보딩/메인 분기는 지문 화면이 한다)
 * - 세션 있음 → 마지막으로 알려진 온보딩 완료 여부(영속 프로필 → 로그인 힌트)로 **네트워크를 기다리지 않고** 가른다.
 *   완료면 [AppStartDestination.Main], 미완료면 [AppStartDestination.Onboarding] — 온보딩 진입 판정이 서버를 다시
 *   확인하므로 그 사이 완료된 사용자는 거기서 피드로 간다. 프로필도 힌트도 없을 때(세션은 있는데 아무것도 모름)만
 *   `GET /users/me` 를 기다린다: 세션 만료(401)는 로그인, 그 밖의 실패는 온보딩이다 — 모를 때 메인으로 보내면
 *   신규 사용자가 온보딩 없이 들어가고 아무도 되돌리지 않는다.
 * - 캐시로 메인에 들어간 경우에만 프로필을 백그라운드로 한 번 맞춰 본다([verifyProfileBehindMain]).
 *
 * 콜드 스타트의 시스템 스플래시는 이 값이 확정될 때까지 유지되므로, 시작 경로에 네트워크 왕복을 두지 않는 것이
 * 목적이다(#74). 캐시가 「미완료」인데 서버가 「완료」인 경우는 온보딩 그래프가 스스로 바로잡는다.
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
                    revision += 1
                    _launch.value = AppShellLaunch(revision = revision, destination = resolution.destination)
                    if (resolution.verifyProfile) verifyProfileBehindMain()
                }
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
                null -> Resolution(resolveByFetchingProfile())
            }

        /** 세션은 있는데 프로필도 힌트도 없다 — 이때만 서버를 기다린다. */
        private suspend fun resolveByFetchingProfile(): AppStartDestination {
            val refreshed = userProfileRepository.refreshProfile()
            refreshed.onSuccess { profile -> return profile.toDestination() }
            val failure = checkNotNull(refreshed.exceptionOrNull())
            if (failure is CoreDataFailure.Unauthorized) return AppStartDestination.Login
            errorReporter.recordFailure(failure, attributes = mapOf(KEY_STAGE to STAGE_START_PROFILE))
            return AppStartDestination.Onboarding
        }

        /**
         * 캐시로 메인에 들어간 뒤 서버 프로필을 한 번 맞춰 본다.
         *
         * - 세션 만료(401): 세션을 지우고 다시 계산한다 → 로그인.
         * - 성공했는데 온보딩 미완료: 다시 계산한다 → 온보딩. 피드가 잠깐 보였다가 온보딩으로 바뀌는데, 서버가 온보딩
         *   완료를 되돌린 드문 경우라 허용한다 — 캐시를 믿고 먼저 들어가는 대가다.
         * - 그 밖의 실패: 기록만 남기고 캐시 목적지를 유지한다(오프라인 시작).
         *
         * 다시 계산을 부르면 [refresh] 가 이 잡을 취소하지만 남은 작업이 없어 무해하다.
         */
        private fun verifyProfileBehindMain() {
            profileVerifyJob?.cancel()
            profileVerifyJob =
                viewModelScope.launch {
                    val refreshed = userProfileRepository.refreshProfile()
                    refreshed.onSuccess { profile ->
                        if (!profile.onboardingDone) refresh()
                        return@launch
                    }
                    val failure = checkNotNull(refreshed.exceptionOrNull())
                    if (failure !is CoreDataFailure.Unauthorized) {
                        errorReporter.recordFailure(failure, attributes = mapOf(KEY_STAGE to STAGE_START_PROFILE))
                        return@launch
                    }
                    // 세션 정리에 실패하면 다시 계산해도 같은 세션으로 메인에 들어가 401 을 반복하므로 기록만 남긴다.
                    authRepository
                        .clearSession()
                        .onSuccess { refresh() }
                        .onFailure { errorReporter.recordFailure(it, attributes = mapOf(KEY_STAGE to STAGE_START_CLEAR_SESSION)) }
                }
        }

        private fun com.cambridge.core.model.user.UserProfile.toDestination(): AppStartDestination =
            if (onboardingDone) AppStartDestination.Main else AppStartDestination.Onboarding

        /** 시작 목적지와, 캐시로 확정해 서버 확인이 아직 남았는지. */
        private data class Resolution(
            val destination: AppStartDestination,
            val verifyProfile: Boolean = false,
        )

        private companion object {
            const val KEY_STAGE = "app_stage"
            const val STAGE_START_PROFILE = "start_profile"
            const val STAGE_START_CLEAR_SESSION = "start_clear_session"
        }
    }
