package com.careercompass.feature.onboarding.presentation.biometric

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.fragment.app.FragmentActivity
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.careercompass.feature.onboarding.presentation.R

/**
 * 지문 빠른 로그인 화면의 상태 배선.
 *
 * 프롬프트 호출은 [rememberBiometricPromptLauncher] 가 맡는다 — [FragmentActivity] 요구와 오류 코드 분류가
 * 지문 등록 제안([BiometricEnrollGate])과 같아야 하기 때문이다. 호스트가 FragmentActivity 가 아니면 프롬프트 대신
 * [BiometricFailureReason.Unavailable] 이 와서 「다른 방법으로 로그인」 만 남는다.
 *
 * 인증 성공 뒤 목적지는 ViewModel 이 세션 검증으로 정한다 — 피드([onLoginSuccess]), 온보딩 미완료([onOnboardingRequired]),
 * 세션 만료([onSessionExpired]). 만료는 [onOtherMethodLogin] 과 같은 로그인 화면으로 가지만 사용자가 고른 것이
 * 아니라 이유를 알려야 해서 길을 나눠 둔다 — 판정도 안내도 앱 셸 몫이고 여기서는 사실만 넘긴다(#128).
 */
@Composable
public fun BiometricLoginEntry(
    onLoginSuccess: () -> Unit,
    onOnboardingRequired: () -> Unit,
    onOtherMethodLogin: () -> Unit,
    onSessionExpired: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: BiometricLoginViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val currentOnLoginSuccess by rememberUpdatedState(onLoginSuccess)
    val currentOnOnboardingRequired by rememberUpdatedState(onOnboardingRequired)
    val currentOnOtherMethodLogin by rememberUpdatedState(onOtherMethodLogin)
    val currentOnSessionExpired by rememberUpdatedState(onSessionExpired)
    val fallbackUserName = stringResource(R.string.onboarding_biometric_default_user_name)

    LaunchedEffect(state.pendingNavigation) {
        when (state.pendingNavigation) {
            BiometricDestination.Feed -> currentOnLoginSuccess()
            BiometricDestination.Onboarding -> currentOnOnboardingRequired()
            BiometricDestination.Login -> currentOnOtherMethodLogin()
            BiometricDestination.SessionExpired -> currentOnSessionExpired()
            null -> return@LaunchedEffect
        }
        viewModel.onNavigationConsumed()
    }

    val launchPrompt =
        rememberBiometricPromptLauncher(
            title = stringResource(R.string.onboarding_biometric_prompt_title),
            negativeButtonText = stringResource(R.string.onboarding_biometric_prompt_negative),
            allowedAuthenticators = BIOMETRIC_LOGIN_AUTHENTICATORS,
            listener = remember(viewModel) { BiometricLoginPromptListener(viewModel) },
        )

    fun authenticate() {
        // 이 계정이 이 기기에 등록해 둔 적이 없다면 프롬프트를 띄워도 열 세션이 없다.
        if (!state.isBiometricEnabled) {
            viewModel.onAuthenticationFailed(
                BiometricFailureReason.Unavailable,
                IllegalStateException("biometric login is not enabled on this device"),
            )
            return
        }
        launchPrompt()
    }

    val errorMessage = state.failure?.let { it.toMessage() }
    BiometricLoginScreen(
        state =
            BiometricLoginUiState(
                userName = state.userName ?: fallbackUserName,
                accountLabel = null,
                isAuthenticating = state.isAuthenticating,
                errorMessage = errorMessage,
            ),
        onEvent = { event ->
            when (event) {
                BiometricLoginEvent.BiometricClicked -> authenticate()
                BiometricLoginEvent.OtherMethodClicked -> viewModel.onOtherMethodClicked()
                BiometricLoginEvent.ErrorDismissed -> viewModel.onFailureConsumed()
            }
        },
        modifier = modifier,
    )
}

private class BiometricLoginPromptListener(
    private val viewModel: BiometricLoginViewModel,
) : BiometricPromptListener {
    override fun onStarted(): Unit = viewModel.onAuthenticationStarted()

    override fun onSucceeded(): Unit = viewModel.onAuthenticationSucceeded()

    override fun onCancelled(): Unit = viewModel.onAuthenticationCancelled()

    override fun onFailed(
        reason: BiometricFailureReason,
        cause: Throwable,
    ): Unit = viewModel.onAuthenticationFailed(reason, cause)
}
