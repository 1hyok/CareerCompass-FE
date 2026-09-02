package com.cambridge.feature.onboarding.presentation.biometric

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cambridge.core.common.reporting.ErrorReporter
import com.cambridge.core.domain.repository.AuthRepository
import com.cambridge.core.domain.repository.UserProfileRepository
import com.cambridge.feature.onboarding.presentation.reporting.OnboardingFailureStage
import com.cambridge.feature.onboarding.presentation.reporting.recordOnboardingFailure
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/** 지문 로그인 실패 사유. 문구는 [BiometricLoginEntry] 가 리소스로 만든다. */
public enum class BiometricFailureReason {
    /** 이 기기·호스트에서는 지문 인증을 쓸 수 없다(미등록·하드웨어 없음·FragmentActivity 아님). */
    Unavailable,

    /** 인증이 오류로 끝났다. */
    Failed,

    /** 시도 횟수 초과로 잠겼다. */
    Lockout,
}

public enum class BiometricDestination {
    Feed,
    Login,
}

/** [BiometricLoginViewModel] 상태. [failure]·[pendingNavigation] 은 단발 신호다. */
public data class BiometricLoginViewState(
    val userName: String? = null,
    val isBiometricEnabled: Boolean = false,
    val isAuthenticating: Boolean = false,
    val failure: BiometricFailureReason? = null,
    val pendingNavigation: BiometricDestination? = null,
)

/**
 * 지문 빠른 로그인 화면 상태 — F1-1 「로그인 시 지문 인식」.
 *
 * 인증 성공은 새 세션 발급이 아니라 **이미 저장된 세션을 그대로 쓴다는 확인**이다. 그래서 성공 시 서버 호출 없이
 * 피드로 보낸다. 생체 인증 자체는 [BiometricLoginEntry] 가 `BiometricPrompt` 로 수행하고 결과만 여기로 온다.
 */
@HiltViewModel
public class BiometricLoginViewModel
    @Inject
    constructor(
        authRepository: AuthRepository,
        userProfileRepository: UserProfileRepository,
        private val errorReporter: ErrorReporter,
    ) : ViewModel() {
        private val _uiState = MutableStateFlow(BiometricLoginViewState())
        public val uiState: StateFlow<BiometricLoginViewState> = _uiState.asStateFlow()

        init {
            viewModelScope.launch {
                combine(authRepository.isBiometricEnabled, userProfileRepository.profile) { enabled, profile ->
                    enabled to profile?.name
                }.collect { (enabled, name) ->
                    _uiState.update { it.copy(isBiometricEnabled = enabled, userName = name) }
                }
            }
        }

        public fun onAuthenticationStarted() {
            _uiState.update { it.copy(isAuthenticating = true, failure = null) }
        }

        public fun onAuthenticationSucceeded() {
            _uiState.update { it.copy(isAuthenticating = false, pendingNavigation = BiometricDestination.Feed) }
        }

        /** 사용자가 프롬프트를 닫았다 — 표시도 기록도 하지 않는다. */
        public fun onAuthenticationCancelled() {
            _uiState.update { it.copy(isAuthenticating = false) }
        }

        public fun onAuthenticationFailed(
            reason: BiometricFailureReason,
            cause: Throwable,
        ) {
            errorReporter.recordOnboardingFailure(OnboardingFailureStage.BiometricAuth, cause)
            _uiState.update { it.copy(isAuthenticating = false, failure = reason) }
        }

        public fun onOtherMethodClicked() {
            _uiState.update { it.copy(pendingNavigation = BiometricDestination.Login) }
        }

        public fun onNavigationConsumed() {
            _uiState.update { it.copy(pendingNavigation = null) }
        }

        public fun onFailureConsumed() {
            _uiState.update { it.copy(failure = null) }
        }
    }
