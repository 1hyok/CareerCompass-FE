package com.cambridge.feature.onboarding.presentation.biometric

import androidx.activity.compose.LocalActivity
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.cambridge.feature.onboarding.presentation.R

/**
 * 지문 빠른 로그인 화면의 상태 배선.
 *
 * `BiometricPrompt` 는 [FragmentActivity] 를 요구한다. 호스트 Activity 가 그렇지 않으면(현재 `MainActivity` 는
 * `ComponentActivity` 다 — 전환은 앱 셸 #65 몫) 프롬프트를 띄우지 않고 [BiometricFailureReason.Unavailable] 을
 * 보여 「다른 방법으로 로그인」 만 남긴다. 사용자 취소(`ERROR_USER_CANCELED`·`ERROR_NEGATIVE_BUTTON`·`ERROR_CANCELED`)
 * 는 표시하지 않는다.
 *
 * 인증 성공 뒤 목적지는 ViewModel 이 세션 검증으로 정한다 — 피드([onLoginSuccess]), 온보딩 미완료([onOnboardingRequired]),
 * 세션 만료([onOtherMethodLogin] 과 같은 로그인 화면).
 */
@Composable
public fun BiometricLoginEntry(
    onLoginSuccess: () -> Unit,
    onOnboardingRequired: () -> Unit,
    onOtherMethodLogin: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: BiometricLoginViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val activity = LocalActivity.current as? FragmentActivity
    val currentOnLoginSuccess by rememberUpdatedState(onLoginSuccess)
    val currentOnOnboardingRequired by rememberUpdatedState(onOnboardingRequired)
    val currentOnOtherMethodLogin by rememberUpdatedState(onOtherMethodLogin)
    val promptTitle = stringResource(R.string.onboarding_biometric_prompt_title)
    val promptNegative = stringResource(R.string.onboarding_biometric_prompt_negative)
    val fallbackUserName = stringResource(R.string.onboarding_biometric_default_user_name)

    LaunchedEffect(state.pendingNavigation) {
        when (state.pendingNavigation) {
            BiometricDestination.Feed -> currentOnLoginSuccess()
            BiometricDestination.Onboarding -> currentOnOnboardingRequired()
            BiometricDestination.Login -> currentOnOtherMethodLogin()
            null -> return@LaunchedEffect
        }
        viewModel.onNavigationConsumed()
    }

    val prompt =
        remember(activity, viewModel) {
            activity?.let { host ->
                BiometricPrompt(host, ContextCompat.getMainExecutor(host), BiometricResultCallback(viewModel))
            }
        }

    fun authenticate() {
        if (!state.isBiometricEnabled) {
            viewModel.onAuthenticationFailed(
                BiometricFailureReason.Unavailable,
                IllegalStateException("biometric login is not enabled on this device"),
            )
            return
        }
        if (activity == null || prompt == null) {
            viewModel.onAuthenticationFailed(
                BiometricFailureReason.Unavailable,
                IllegalStateException("BiometricPrompt requires a FragmentActivity host"),
            )
            return
        }
        val availability = BiometricManager.from(activity).canAuthenticate(ALLOWED_AUTHENTICATORS)
        if (availability != BiometricManager.BIOMETRIC_SUCCESS) {
            viewModel.onAuthenticationFailed(
                BiometricFailureReason.Unavailable,
                BiometricAuthenticationException(availability, "biometric unavailable: $availability"),
            )
            return
        }
        viewModel.onAuthenticationStarted()
        prompt.authenticate(
            BiometricPrompt.PromptInfo
                .Builder()
                .setTitle(promptTitle)
                .setNegativeButtonText(promptNegative)
                .setAllowedAuthenticators(ALLOWED_AUTHENTICATORS)
                .build(),
        )
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

/** 프롬프트 오류 코드를 리포팅에 남기기 위한 타입 — 문구는 버려지고 타입·코드만 남는다. */
internal class BiometricAuthenticationException(
    val errorCode: Int,
    message: String,
) : Exception(message)

private class BiometricResultCallback(
    private val viewModel: BiometricLoginViewModel,
) : BiometricPrompt.AuthenticationCallback() {
    override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
        viewModel.onAuthenticationSucceeded()
    }

    override fun onAuthenticationError(
        errorCode: Int,
        errString: CharSequence,
    ) {
        when (errorCode) {
            BiometricPrompt.ERROR_USER_CANCELED,
            BiometricPrompt.ERROR_NEGATIVE_BUTTON,
            BiometricPrompt.ERROR_CANCELED,
            -> viewModel.onAuthenticationCancelled()

            BiometricPrompt.ERROR_LOCKOUT,
            BiometricPrompt.ERROR_LOCKOUT_PERMANENT,
            -> viewModel.onAuthenticationFailed(BiometricFailureReason.Lockout, exception(errorCode))

            BiometricPrompt.ERROR_HW_UNAVAILABLE,
            BiometricPrompt.ERROR_HW_NOT_PRESENT,
            BiometricPrompt.ERROR_NO_BIOMETRICS,
            BiometricPrompt.ERROR_NO_DEVICE_CREDENTIAL,
            BiometricPrompt.ERROR_SECURITY_UPDATE_REQUIRED,
            -> viewModel.onAuthenticationFailed(BiometricFailureReason.Unavailable, exception(errorCode))

            else -> viewModel.onAuthenticationFailed(BiometricFailureReason.Failed, exception(errorCode))
        }
    }

    /** 단일 시도 실패 — 프롬프트가 계속 떠 있으므로 화면 상태는 바꾸지 않는다. */
    override fun onAuthenticationFailed() = Unit

    private fun exception(errorCode: Int) = BiometricAuthenticationException(errorCode, "biometric prompt error: $errorCode")
}

private const val ALLOWED_AUTHENTICATORS =
    BiometricManager.Authenticators.BIOMETRIC_STRONG or BiometricManager.Authenticators.BIOMETRIC_WEAK
