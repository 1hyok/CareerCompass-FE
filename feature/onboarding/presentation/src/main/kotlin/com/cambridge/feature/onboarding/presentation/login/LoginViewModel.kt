package com.cambridge.feature.onboarding.presentation.login

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cambridge.core.common.reporting.ErrorReporter
import com.cambridge.core.domain.error.CoreAuthFailure
import com.cambridge.core.domain.error.CoreDataFailure
import com.cambridge.core.domain.usecase.auth.SocialLoginUseCase
import com.cambridge.core.model.auth.SocialProvider
import com.cambridge.feature.onboarding.presentation.reporting.OnboardingFailureStage
import com.cambridge.feature.onboarding.presentation.reporting.recordOnboardingFailure
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.IOException
import javax.inject.Inject

/** 로그인 실패 사유. 문구는 [LoginEntry] 가 리소스로 만든다. */
public enum class LoginFailureReason {
    /** 서버에 닿지 못했다. */
    Network,

    /** 서버가 소셜 토큰을 거절했다. */
    Rejected,

    /** 사유를 확인하지 못한 실패. */
    Unknown,
}

/** 로그인 성공 뒤 갈 곳 — 신규 가입이면 온보딩, 아니면 피드(F1-1). */
public enum class LoginDestination {
    Onboarding,
    Feed,
}

/**
 * [LoginViewModel] 상태. [failure]·[pendingNavigation] 은 단발 신호다 — UI 가 소비한 뒤
 * [LoginViewModel.onFailureConsumed]·[LoginViewModel.onNavigationConsumed] 로 비운다.
 */
public data class LoginViewState(
    val isLoading: Boolean = false,
    val failure: LoginFailureReason? = null,
    val pendingNavigation: LoginDestination? = null,
)

/**
 * 소셜 로그인 화면 상태. SDK 토큰 획득(Activity 필요)은 [LoginEntry] 가 맡고, 여기는 토큰만 받아 서버 로그인을 한다.
 */
@HiltViewModel
public class LoginViewModel
    @Inject
    constructor(
        private val socialLogin: SocialLoginUseCase,
        private val errorReporter: ErrorReporter,
    ) : ViewModel() {
        private val _uiState = MutableStateFlow(LoginViewState())
        public val uiState: StateFlow<LoginViewState> = _uiState.asStateFlow()

        /** SDK 로그인 UI 가 뜨는 동안 버튼을 잠근다 — 이중 탭으로 두 개의 SDK 세션이 열리는 것을 막는다. */
        public fun onSocialTokenRequestStarted() {
            _uiState.update { it.copy(isLoading = true, failure = null) }
        }

        public fun loginWithKakao(accessToken: String) {
            login(SocialProvider.Kakao, accessToken)
        }

        public fun loginWithGoogle(idToken: String) {
            login(SocialProvider.Google, idToken)
        }

        /** SDK 단계 실패. 사용자 취소([CoreAuthFailure.UserCancelledAuth])는 표시도 기록도 하지 않는다. */
        public fun onSocialTokenRequestFailed(
            provider: SocialProvider,
            throwable: Throwable,
        ) {
            if (throwable is CoreAuthFailure.UserCancelledAuth) {
                _uiState.update { it.copy(isLoading = false) }
                return
            }
            errorReporter.recordOnboardingFailure(OnboardingFailureStage.SocialTokenRequest, throwable, provider)
            _uiState.update { it.copy(isLoading = false, failure = throwable.toLoginFailureReason()) }
        }

        public fun onNavigationConsumed() {
            _uiState.update { it.copy(pendingNavigation = null) }
        }

        public fun onFailureConsumed() {
            _uiState.update { it.copy(failure = null) }
        }

        private fun login(
            provider: SocialProvider,
            providerToken: String,
        ) {
            if (providerToken.isBlank()) {
                onSocialTokenRequestFailed(provider, IllegalStateException("social SDK returned a blank token"))
                return
            }
            _uiState.update { it.copy(isLoading = true, failure = null) }
            viewModelScope.launch {
                socialLogin(provider = provider, providerToken = providerToken)
                    .onSuccess { outcome ->
                        val destination = if (outcome.isNewUser) LoginDestination.Onboarding else LoginDestination.Feed
                        _uiState.update { it.copy(isLoading = false, pendingNavigation = destination) }
                    }.onFailure { throwable ->
                        errorReporter.recordOnboardingFailure(OnboardingFailureStage.SocialLogin, throwable, provider)
                        _uiState.update { it.copy(isLoading = false, failure = throwable.toLoginFailureReason()) }
                    }
            }
        }
    }

internal fun Throwable.toLoginFailureReason(): LoginFailureReason =
    when (this) {
        is CoreAuthFailure.NetworkUnavailable,
        is CoreDataFailure.NetworkUnavailable,
        is IOException,
        -> LoginFailureReason.Network

        is CoreAuthFailure.SocialLoginRejected -> LoginFailureReason.Rejected

        else -> LoginFailureReason.Unknown
    }
