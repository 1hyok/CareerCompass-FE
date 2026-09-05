package com.careercompass.feature.onboarding.presentation.login

import androidx.lifecycle.viewModelScope
import com.careercompass.core.common.reporting.ErrorReporter
import com.careercompass.core.common.result.runCatchingCancellable
import com.careercompass.core.domain.error.CoreAuthFailure
import com.careercompass.core.domain.error.CoreDataFailure
import com.careercompass.core.domain.usecase.auth.SocialLoginUseCase
import com.careercompass.core.model.auth.SocialProvider
import com.careercompass.core.ui.mvi.MviViewModel
import com.careercompass.feature.onboarding.presentation.reporting.OnboardingFailureStage
import com.careercompass.feature.onboarding.presentation.reporting.recordOnboardingFailure
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import java.io.IOException
import javax.inject.Inject

/**
 * 소셜 로그인 화면 상태. 진입점은 [onIntent] 하나, 전이는 [reduce] 한 곳이다(#245, `docs/convention/mvi.md`).
 *
 * ### 진행 중인 시도의 주인은 ViewModel 이다 (#147)
 * 예전에는 화면이 `rememberCoroutineScope()` 로 SDK 를 띄웠다. 그 스코프는 컴포지션과 함께 죽으므로 SDK 화면이 떠
 * 있는 동안 설정 변경으로 액티비티가 재생성되면 코루틴만 조용히 취소되고 [LoginUiState.isLoading] 을 되돌릴
 * 사람이 아무도 없었다 — 돌아온 화면의 로그인 버튼이 영영 잠겼고, 로그인은 앱의 유일한 입구라 앱 전체가 막혔다.
 *
 * 그래서 시도는 [viewModelScope] 에서 돈다. 화면은 「토큰을 어떻게 가져오는가」만 [LoginIntent.RequestSocialLogin] 의
 * `requestToken` 으로 넘긴다 — SDK 가 요구하는 Activity 는 그 람다 안에만 살고 ViewModel 의 필드가 되지 않는다.
 * 시도가 끝나면 람다와 함께 참조도 사라진다. 발사대는 [rememberSocialLoginLauncher] 다.
 *
 * ### 단계를 둘로 가른 이유
 * 토큰 단계는 Activity 에 매여 있고, 토큰을 손에 넣은 뒤의 서버 로그인은 그렇지 않다. 그래서 화면이 사라지면
 * [LoginIntent.DetachLoginHost] 가 **토큰 단계만** 끊는다. 이미 받아 낸 토큰까지 화면 재생성 때문에 버리면 사용자는
 * 멀쩡히 끝난 소셜 인증을 처음부터 다시 해야 한다.
 *
 * ### 취소·실패·이탈을 가른다
 * - **사용자가 SDK 화면에서 뒤로 나옴**([CoreAuthFailure.UserCancelledAuth]) — 조용히 잠금만 푼다. 자기가 방금
 *   한 일이라 안내가 필요 없고, 기록해야 할 고장도 아니다.
 * - **재생성·이탈로 끊김**([LoginIntent.DetachLoginHost]) — 마찬가지로 조용히 되돌린다. 사용자가 한 일이 아니고, 새로
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
    ) : MviViewModel<LoginIntent, LoginUiState, LoginReducerEvent>(LoginUiState()) {
        /** Activity 에 매인 토큰 단계. 화면이 사라지면 이것만 끊는다 — 서버 로그인은 별도 잡으로 이어 달린다. */
        private var tokenRequest: Job? = null

        override fun onIntent(intent: LoginIntent) {
            when (intent) {
                is LoginIntent.RequestSocialLogin -> requestSocialLogin(intent.provider, intent.requestToken)
                LoginIntent.DetachLoginHost -> detachLoginHost()
                LoginIntent.ConsumeFailure -> dispatch(LoginReducerEvent.FailureConsumed)
                LoginIntent.ConsumeNavigation -> dispatch(LoginReducerEvent.NavigationConsumed)
            }
        }

        override fun reduce(
            state: LoginUiState,
            event: LoginReducerEvent,
        ): LoginUiState =
            when (event) {
                LoginReducerEvent.AttemptStarted -> state.copy(isLoading = true, failure = null)
                LoginReducerEvent.AttemptAbandoned -> state.copy(isLoading = false)
                is LoginReducerEvent.LoggedIn -> state.copy(isLoading = false, pendingNavigation = event.destination)
                is LoginReducerEvent.LoginFailed -> state.copy(isLoading = false, failure = event.reason)
                LoginReducerEvent.FailureConsumed -> state.copy(failure = null)
                LoginReducerEvent.NavigationConsumed -> state.copy(pendingNavigation = null)
            }

        /** SDK 로그인 UI 가 뜨는 동안 버튼을 잠가 두 개의 SDK 세션이 열리는 것을 막는다. */
        private fun requestSocialLogin(
            provider: SocialProvider,
            requestToken: suspend () -> Result<String>,
        ) {
            if (currentState.isLoading) return
            dispatch(LoginReducerEvent.AttemptStarted)
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
         * 토큰 단계가 아직 진행 중이면 끊고 잠금을 푼다. 안내는 내지 않는다 — 사용자가 실패한 것이 아니라 화면이
         * 다시 그려진 것뿐이고, 돌아온 화면에서 필요한 것은 「다시 누를 수 있는 버튼」이다.
         *
         * 토큰을 이미 받아 서버 로그인 중이라면 아무것도 하지 않는다. 그 단계는 Activity 가 필요 없어 끝까지 간다.
         */
        private fun detachLoginHost() {
            val inFlight = tokenRequest ?: return
            if (!inFlight.isActive) return
            tokenRequest = null
            inFlight.cancel()
            dispatch(LoginReducerEvent.AttemptAbandoned)
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
                        dispatch(LoginReducerEvent.LoggedIn(destination))
                    }.onFailure { throwable ->
                        errorReporter.recordOnboardingFailure(OnboardingFailureStage.SocialLogin, throwable, provider)
                        dispatch(LoginReducerEvent.LoginFailed(throwable.toLoginFailureReason()))
                    }
            }
        }

        /** SDK 단계 실패. 사용자 취소는 표시도 기록도 하지 않는다. */
        private fun handleTokenFailure(
            provider: SocialProvider,
            throwable: Throwable,
        ) {
            if (throwable is CoreAuthFailure.UserCancelledAuth) {
                dispatch(LoginReducerEvent.AttemptAbandoned)
                return
            }
            errorReporter.recordOnboardingFailure(OnboardingFailureStage.SocialTokenRequest, throwable, provider)
            dispatch(LoginReducerEvent.LoginFailed(throwable.toLoginFailureReason()))
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
