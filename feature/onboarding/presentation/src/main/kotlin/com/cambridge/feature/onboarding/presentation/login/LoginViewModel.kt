package com.cambridge.feature.onboarding.presentation.login

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cambridge.feature.onboarding.presentation.reporting.OnboardingFailureStage
import com.cambridge.feature.onboarding.presentation.reporting.recordOnboardingFailure
import com.careercompass.core.common.reporting.ErrorReporter
import com.careercompass.core.common.result.runCatchingCancellable
import com.careercompass.core.domain.error.CoreAuthFailure
import com.careercompass.core.domain.error.CoreDataFailure
import com.careercompass.core.domain.usecase.auth.SocialLoginUseCase
import com.careercompass.core.model.auth.SocialProvider
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
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
 * 소셜 로그인 화면 상태.
 *
 * ### 진행 중인 시도의 주인은 ViewModel 이다 (#147)
 * 예전에는 화면이 `rememberCoroutineScope()` 로 SDK 를 띄웠다. 그 스코프는 컴포지션과 함께 죽으므로 SDK 화면이 떠
 * 있는 동안 설정 변경으로 액티비티가 재생성되면 코루틴만 조용히 취소되고 [LoginViewState.isLoading] 을 되돌릴
 * 사람이 아무도 없었다 — 돌아온 화면의 로그인 버튼이 영영 잠겼고, 로그인은 앱의 유일한 입구라 앱 전체가 막혔다.
 *
 * 그래서 시도는 [viewModelScope] 에서 돈다. 화면은 「토큰을 어떻게 가져오는가」만 [onSocialLoginRequested] 의
 * `requestToken` 으로 넘긴다 — SDK 가 요구하는 Activity 는 그 람다 안에만 살고 ViewModel 의 필드가 되지 않는다.
 * 시도가 끝나면 람다와 함께 참조도 사라진다. 발사대는 [rememberSocialLoginLauncher] 다.
 *
 * ### 단계를 둘로 가른 이유
 * 토큰 단계는 Activity 에 매여 있고, 토큰을 손에 넣은 뒤의 서버 로그인은 그렇지 않다. 그래서 화면이 사라지면
 * [onLoginHostDetached] 가 **토큰 단계만** 끊는다. 이미 받아 낸 토큰까지 화면 재생성 때문에 버리면 사용자는
 * 멀쩡히 끝난 소셜 인증을 처음부터 다시 해야 한다.
 *
 * ### 취소·실패·이탈을 가른다
 * - **사용자가 SDK 화면에서 뒤로 나옴**([CoreAuthFailure.UserCancelledAuth]) — 조용히 잠금만 푼다. 자기가 방금
 *   한 일이라 안내가 필요 없고, 기록해야 할 고장도 아니다.
 * - **재생성·이탈로 끊김**([onLoginHostDetached]) — 마찬가지로 조용히 되돌린다. 사용자가 한 일이 아니고, 새로
 *   그려진 화면에 뜨는 「로그인에 실패했어요」는 맥락 없는 놀람이다. 다만 취소와 달리 아직 응답을 기다리는
 *   SDK 요청을 우리 쪽에서 끊는다 — 사라진 Activity 를 붙잡은 채 영영 안 올 콜백을 기다리지 않으려는 것이다.
 * - **그 밖의 실패** — 사유를 카드로 보이고 [ErrorReporter] 에 기록한다.
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

        /** Activity 에 매인 토큰 단계. 화면이 사라지면 이것만 끊는다 — 서버 로그인은 별도 잡으로 이어 달린다. */
        private var tokenRequest: Job? = null

        /**
         * 소셜 로그인 시도 하나. SDK 로그인 UI 가 뜨는 동안 버튼을 잠가 두 개의 SDK 세션이 열리는 것을 막는다.
         *
         * @param requestToken SDK 에서 토큰을 받아 오는 일. Activity 가 필요한 유일한 조각이라 화면이 만들어 넘긴다.
         */
        public fun onSocialLoginRequested(
            provider: SocialProvider,
            requestToken: suspend () -> Result<String>,
        ) {
            if (_uiState.value.isLoading) return
            _uiState.update { it.copy(isLoading = true, failure = null) }
            tokenRequest =
                viewModelScope.launch {
                    // SDK 진입점은 초기화가 안 됐을 때 동기 예외를 던진다 — 버튼 하나로 앱이 죽지 않게 여기서 받는다.
                    runCatchingCancellable { requestToken() }
                        .getOrElse { Result.failure(it) }
                        .onSuccess { token -> startServerLogin(provider, token) }
                        .onFailure { throwable -> handleTokenFailure(provider, throwable) }
                }
        }

        /**
         * SDK 를 띄운 화면이 사라졌다(설정 변경에 따른 재생성·화면 이탈).
         *
         * 토큰 단계가 아직 진행 중이면 끊고 잠금을 푼다. 안내는 내지 않는다 — 사용자가 실패한 것이 아니라 화면이
         * 다시 그려진 것뿐이고, 돌아온 화면에서 필요한 것은 「다시 누를 수 있는 버튼」이다.
         *
         * 토큰을 이미 받아 서버 로그인 중이라면 아무것도 하지 않는다. 그 단계는 Activity 가 필요 없어 끝까지 간다.
         */
        public fun onLoginHostDetached() {
            val inFlight = tokenRequest ?: return
            if (!inFlight.isActive) return
            tokenRequest = null
            inFlight.cancel()
            _uiState.update { it.copy(isLoading = false) }
        }

        public fun onNavigationConsumed() {
            _uiState.update { it.copy(pendingNavigation = null) }
        }

        public fun onFailureConsumed() {
            _uiState.update { it.copy(failure = null) }
        }

        /** 토큰을 손에 넣은 뒤로는 Activity 가 필요 없다 — 토큰 단계와 다른 잡에 실어 화면이 사라져도 끝까지 간다. */
        private fun startServerLogin(
            provider: SocialProvider,
            providerToken: String,
        ) {
            if (providerToken.isBlank()) {
                handleTokenFailure(provider, IllegalStateException("social SDK returned a blank token"))
                return
            }
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

        /** SDK 단계 실패. 사용자 취소는 표시도 기록도 하지 않는다. */
        private fun handleTokenFailure(
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
