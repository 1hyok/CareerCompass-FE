package com.cambridge.careercompass_fe.session

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cambridge.core.common.reporting.ErrorReporter
import com.cambridge.core.domain.error.CoreDataFailure
import com.cambridge.core.domain.repository.AuthRepository
import com.cambridge.core.domain.repository.UserProfileRepository
import dagger.hilt.android.lifecycle.HiltViewModel
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
 * - 세션 있음 → `GET /users/me` 의 `onboardingDone` 으로 온보딩/메인 분기. 조회 실패는 로컬 캐시 프로필로
 *   대신하고, 그것도 없으면 메인으로 보낸다 — 온보딩을 이미 마친 사용자를 네트워크 오류 때문에 온보딩에
 *   가두면 안 된다(온보딩 그래프 진입 시 다시 확인한다). 세션 만료(401)는 로그인으로 보낸다.
 */
@HiltViewModel
public class MainViewModel
    @Inject
    constructor(
        private val authRepository: AuthRepository,
        private val userProfileRepository: UserProfileRepository,
        private val errorReporter: ErrorReporter,
    ) : ViewModel() {
        private val _startDestination = MutableStateFlow<AppStartDestination?>(null)

        /** 초기 진입 시 null(로딩)이며, 세션·프로필 확인 뒤 확정된다. */
        public val startDestination: StateFlow<AppStartDestination?> = _startDestination.asStateFlow()

        init {
            viewModelScope.launch { _startDestination.value = resolve() }
        }

        /** 로그아웃·세션 만료 뒤 앱 셸이 다시 계산할 때 쓴다. */
        public fun refresh() {
            viewModelScope.launch { _startDestination.value = resolve() }
        }

        private suspend fun resolve(): AppStartDestination {
            if (!authRepository.isLoggedIn.first()) return AppStartDestination.Login
            if (authRepository.isBiometricEnabled.first()) return AppStartDestination.BiometricLogin
            return resolveAfterSession()
        }

        internal suspend fun resolveAfterSession(): AppStartDestination {
            val refreshed = userProfileRepository.refreshProfile()
            val profile =
                refreshed.getOrElse { failure ->
                    if (failure is CoreDataFailure.Unauthorized) return AppStartDestination.Login
                    errorReporter.recordFailure(failure, attributes = mapOf(KEY_STAGE to STAGE_START_PROFILE))
                    userProfileRepository.profile.first()
                }
            return if (profile != null && !profile.onboardingDone) AppStartDestination.Onboarding else AppStartDestination.Main
        }

        private companion object {
            const val KEY_STAGE = "app_stage"
            const val STAGE_START_PROFILE = "start_profile"
        }
    }
