package com.cambridge.feature.onboarding.presentation.biometric

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cambridge.core.common.reporting.ErrorReporter
import com.cambridge.core.domain.repository.AuthRepository
import com.cambridge.core.domain.repository.UserProfileRepository
import com.cambridge.core.domain.usecase.auth.ResolveSessionEntryUseCase
import com.cambridge.core.domain.usecase.auth.SessionEntryDestination
import com.cambridge.feature.onboarding.presentation.reporting.OnboardingFailureStage
import com.cambridge.feature.onboarding.presentation.reporting.recordOnboardingFailure
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
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

/** 지문 확인 뒤 갈 곳 — 세션 검증 결과에 따라 피드·온보딩, 세션이 끝났거나 다른 방법을 골랐으면 로그인. */
public enum class BiometricDestination {
    Feed,
    Onboarding,
    Login,
}

/**
 * [BiometricLoginViewModel] 상태. [failure]·[pendingNavigation] 은 단발 신호다.
 *
 * @property isAuthenticating 프롬프트가 떠 있는 동안과, 인증 성공 뒤 세션을 검증하는 동안 true — 둘 다 「지문 버튼을
 *   다시 누르면 안 되는」 같은 상태라 화면은 구분하지 않는다.
 */
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
 * 인증 성공은 새 세션 발급이 아니라 **이미 저장된 세션을 그대로 쓴다는 확인**이다. 다만 그 세션을 서버가 아직
 * 받아 주는지는 여기서 모르므로, 성공 뒤 [ResolveSessionEntryUseCase] 로 검증해 목적지를 정한다 — 곧장 피드로
 * 보내면 죽은 세션이 피드 → 401 → 로그인으로 두 번 이동하며 피드 셸이 잠깐 드러났다(#81). 생체 인증 자체는
 * [BiometricLoginEntry] 가 `BiometricPrompt` 로 수행하고 결과만 여기로 온다.
 */
@HiltViewModel
public class BiometricLoginViewModel
    @Inject
    constructor(
        authRepository: AuthRepository,
        userProfileRepository: UserProfileRepository,
        private val resolveSessionEntry: ResolveSessionEntryUseCase,
        private val errorReporter: ErrorReporter,
    ) : ViewModel() {
        private val _uiState = MutableStateFlow(BiometricLoginViewState())
        public val uiState: StateFlow<BiometricLoginViewState> = _uiState.asStateFlow()

        private var verifyJob: Job? = null

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

        /**
         * 지문이 맞았다 — 인증 중 표시를 유지한 채 세션을 검증하고 목적지를 정한다. 검증이 이미 진행 중이면 합류한다.
         * 서버 확인에 실패해 마지막으로 알려진 온보딩 상태로 판단했으면 그 실패를 기록만 하고 진행한다.
         */
        public fun onAuthenticationSucceeded() {
            if (verifyJob?.isActive == true) return
            _uiState.update { it.copy(isAuthenticating = true, failure = null) }
            verifyJob =
                viewModelScope.launch {
                    val entry = resolveSessionEntry()
                    entry.fallbackCause?.let { errorReporter.recordOnboardingFailure(OnboardingFailureStage.BiometricSessionVerify, it) }
                    val destination =
                        when (entry.destination) {
                            SessionEntryDestination.Feed -> BiometricDestination.Feed
                            SessionEntryDestination.Onboarding -> BiometricDestination.Onboarding
                            SessionEntryDestination.Login -> BiometricDestination.Login
                        }
                    _uiState.update { it.copy(isAuthenticating = false, pendingNavigation = destination) }
                }
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
