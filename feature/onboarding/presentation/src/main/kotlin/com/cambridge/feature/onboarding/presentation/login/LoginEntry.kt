package com.cambridge.feature.onboarding.presentation.login

import android.app.Activity
import androidx.activity.compose.LocalActivity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.cambridge.core.model.auth.SocialProvider
import com.cambridge.feature.onboarding.presentation.BuildConfig
import com.cambridge.feature.onboarding.presentation.login.util.GoogleLoginHelper
import com.cambridge.feature.onboarding.presentation.login.util.KakaoLoginHelper
import kotlinx.coroutines.launch

/**
 * 소셜 로그인 화면의 상태 배선.
 *
 * 카카오·Google SDK 는 Activity 를 요구하므로 토큰 획득은 여기서 하고, [LoginViewModel] 에는 토큰만 넘긴다.
 * [LocalActivity] 가 없는 호스트(프리뷰 등)에서는 SDK 를 부르지 않고 실패로 처리한다.
 *
 * Kakao SDK 초기화(`KakaoSdk.init`)와 `GOOGLE_WEB_CLIENT_ID` 주입은 앱 셸 몫이다.
 */
@Composable
public fun LoginEntry(
    onLoginSuccess: () -> Unit,
    onNewUserOnboarding: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: LoginViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val activity = LocalActivity.current
    val scope = rememberCoroutineScope()
    val currentOnLoginSuccess by rememberUpdatedState(onLoginSuccess)
    val currentOnNewUserOnboarding by rememberUpdatedState(onNewUserOnboarding)

    LaunchedEffect(state.pendingNavigation) {
        when (state.pendingNavigation) {
            LoginDestination.Feed -> currentOnLoginSuccess()
            LoginDestination.Onboarding -> currentOnNewUserOnboarding()
            null -> return@LaunchedEffect
        }
        viewModel.onNavigationConsumed()
    }

    fun requestSocialToken(provider: SocialProvider) {
        if (activity == null) {
            viewModel.onSocialTokenRequestFailed(provider, IllegalStateException("social login requires an Activity host"))
            return
        }
        viewModel.onSocialTokenRequestStarted()
        scope.launch {
            requestProviderToken(provider, activity)
                .onSuccess { token ->
                    when (provider) {
                        SocialProvider.Kakao -> viewModel.loginWithKakao(token)
                        SocialProvider.Google -> viewModel.loginWithGoogle(token)
                    }
                }.onFailure { throwable -> viewModel.onSocialTokenRequestFailed(provider, throwable) }
        }
    }

    val errorMessage = state.failure?.let { it.toMessage() }
    LoginScreen(
        state = LoginUiState(isLoading = state.isLoading, errorMessage = errorMessage),
        onEvent = { event ->
            when (event) {
                LoginEvent.KakaoLoginClicked -> requestSocialToken(SocialProvider.Kakao)
                LoginEvent.GoogleLoginClicked -> requestSocialToken(SocialProvider.Google)
                LoginEvent.ErrorDismissed -> viewModel.onFailureConsumed()
            }
        },
        modifier = modifier,
    )
}

private suspend fun requestProviderToken(
    provider: SocialProvider,
    activity: Activity,
): Result<String> =
    when (provider) {
        SocialProvider.Kakao -> KakaoLoginHelper.requestKakaoAccessToken(activity)
        SocialProvider.Google -> GoogleLoginHelper.requestGoogleIdToken(activity, BuildConfig.GOOGLE_WEB_CLIENT_ID)
    }
