package com.careercompass.feature.onboarding.presentation.biometric

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.disabled
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.fragment.app.FragmentActivity
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.careercompass.core.ui.component.CareerCompassButton
import com.careercompass.core.ui.component.CareerCompassButtonSize
import com.careercompass.core.ui.component.CareerCompassButtonVariant
import com.careercompass.core.ui.mvi.ObserveSignal
import com.careercompass.core.ui.theme.CareerCompassTheme
import com.careercompass.feature.onboarding.presentation.R
import com.careercompass.feature.onboarding.presentation.shared.component.OnboardingBrandMark
import com.careercompass.feature.onboarding.presentation.shared.component.OnboardingCenteredLayout
import com.careercompass.feature.onboarding.presentation.shared.component.OnboardingErrorCard

/**
 * 지문 빠른 로그인 화면 — stateful 층.
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
public fun BiometricLoginScreen(
    onLoginSuccess: () -> Unit,
    onOnboardingRequired: () -> Unit,
    onOtherMethodLogin: () -> Unit,
    onSessionExpired: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: BiometricLoginViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    ObserveSignal(
        signal = state.pendingNavigation,
        consumed = BiometricLoginIntent.ConsumeNavigation,
        onIntent = viewModel::onIntent,
    ) { destination ->
        when (destination) {
            BiometricDestination.Feed -> onLoginSuccess()
            BiometricDestination.Onboarding -> onOnboardingRequired()
            BiometricDestination.Login -> onOtherMethodLogin()
            BiometricDestination.SessionExpired -> onSessionExpired()
        }
    }

    val launchPrompt =
        rememberBiometricPromptLauncher(
            title = stringResource(R.string.onboarding_biometric_prompt_title),
            negativeButtonText = stringResource(R.string.onboarding_biometric_prompt_negative),
            allowedAuthenticators = BIOMETRIC_LOGIN_AUTHENTICATORS,
            listener = remember(viewModel) { BiometricLoginPromptListener(viewModel::onIntent) },
        )

    BiometricLoginContent(
        state = state,
        onIntent = viewModel::onIntent,
        onBiometricClick = {
            // 이 계정이 이 기기에 등록해 둔 적이 없다면 프롬프트를 띄워도 열 세션이 없다.
            if (state.isBiometricEnabled) {
                launchPrompt()
            } else {
                viewModel.onIntent(
                    BiometricLoginIntent.AuthenticationFailed(
                        BiometricFailureReason.Unavailable,
                        IllegalStateException("biometric login is not enabled on this device"),
                    ),
                )
            }
        },
        modifier = modifier,
    )
}

/**
 * 지문 빠른 로그인 화면 — stateless 층. 프리뷰·골든·Robolectric 이 그리는 진입점이다.
 *
 * @param onBiometricClick 프롬프트는 [FragmentActivity] 에 매여 있어 stateful 층이 띄운다 — Intent 가 아니라 콜백으로
 *   남는 유일한 상호작용이다(`docs/convention/mvi.md`).
 */
@Composable
internal fun BiometricLoginContent(
    state: BiometricLoginUiState,
    onIntent: (BiometricLoginIntent) -> Unit,
    onBiometricClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val userName = state.userName ?: stringResource(R.string.onboarding_biometric_default_user_name)
    val errorMessage = state.failure?.toMessage()

    OnboardingCenteredLayout(
        topContent = { BiometricGreeting(userName = userName) },
        modifier = modifier,
        centerContent = {
            BiometricPrompt(
                isAuthenticating = state.isAuthenticating,
                enabled = state.isActionEnabled,
                onClick = onBiometricClick,
            )
        },
        bottomContent = {
            if (errorMessage != null) {
                OnboardingErrorCard(
                    message = errorMessage,
                    onDismissClick = { onIntent(BiometricLoginIntent.ConsumeFailure) },
                )
            }
            CareerCompassButton(
                text = stringResource(R.string.onboarding_biometric_other_method),
                onClick = { onIntent(BiometricLoginIntent.ChooseOtherMethod) },
                modifier = Modifier.fillMaxWidth(),
                variant = CareerCompassButtonVariant.Secondary,
                size = CareerCompassButtonSize.Large,
            )
        },
    )
}

private class BiometricLoginPromptListener(
    private val onIntent: (BiometricLoginIntent) -> Unit,
) : BiometricPromptListener {
    override fun onStarted(): Unit = onIntent(BiometricLoginIntent.AuthenticationStarted)

    override fun onSucceeded(): Unit = onIntent(BiometricLoginIntent.AuthenticationSucceeded)

    override fun onCancelled(): Unit = onIntent(BiometricLoginIntent.AuthenticationCancelled)

    override fun onFailed(
        reason: BiometricFailureReason,
        cause: Throwable,
    ): Unit = onIntent(BiometricLoginIntent.AuthenticationFailed(reason, cause))
}

@Composable
private fun BiometricGreeting(userName: String) {
    val colors = CareerCompassTheme.colors
    val spacing = CareerCompassTheme.spacing

    OnboardingBrandMark(
        size = 40.dp,
        contentDescription = stringResource(R.string.onboarding_app_name),
    )
    Spacer(modifier = Modifier.height(spacing.large))
    Text(
        text = stringResource(R.string.onboarding_biometric_greeting, userName),
        modifier = Modifier.semantics { heading() },
        color = colors.onSurface,
        textAlign = TextAlign.Center,
        style = CareerCompassTheme.typography.headline2,
    )
}

@Composable
private fun BiometricPrompt(
    isAuthenticating: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    val colors = CareerCompassTheme.colors
    val spacing = CareerCompassTheme.spacing

    BiometricActionButton(
        isAuthenticating = isAuthenticating,
        enabled = enabled,
        onClick = onClick,
    )
    Spacer(modifier = Modifier.height(spacing.xLarge))
    Text(
        text = stringResource(R.string.onboarding_biometric_title),
        color = colors.onSurface,
        textAlign = TextAlign.Center,
        style = CareerCompassTheme.typography.headline4,
    )
    Spacer(modifier = Modifier.height(spacing.small))
    Text(
        text = stringResource(R.string.onboarding_biometric_description),
        color = colors.mutedContent,
        textAlign = TextAlign.Center,
        style = CareerCompassTheme.typography.bodyMedium,
    )
}

@Composable
private fun BiometricActionButton(
    isAuthenticating: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    val colors = CareerCompassTheme.colors
    val fontScale = LocalDensity.current.fontScale
    val actionDescription = stringResource(R.string.onboarding_biometric_action_description)
    val authenticatingState = stringResource(R.string.onboarding_biometric_authenticating_state)

    Box(
        modifier =
            Modifier
                .size(BIOMETRIC_ACTION_SIZE)
                .clip(CircleShape)
                .background(colors.primaryContainer)
                .clickable(
                    enabled = enabled,
                    role = Role.Button,
                    onClick = onClick,
                ).semantics(mergeDescendants = true) {
                    contentDescription = actionDescription
                    role = Role.Button
                    if (!enabled) disabled()
                    if (isAuthenticating) stateDescription = authenticatingState
                },
        contentAlignment = Alignment.Center,
    ) {
        if (isAuthenticating) {
            CircularProgressIndicator(
                modifier =
                    Modifier
                        .size(36.dp)
                        .clearAndSetSemantics {},
                color = colors.primaryEmphasis,
                strokeWidth = 3.dp,
            )
        } else {
            Text(
                text = stringResource(R.string.onboarding_biometric_action_icon),
                modifier = Modifier.clearAndSetSemantics {},
                fontSize = (BIOMETRIC_ICON_SIZE_SP / fontScale).sp,
                lineHeight = (BIOMETRIC_ICON_LINE_HEIGHT_SP / fontScale).sp,
            )
        }
    }
}

private val BIOMETRIC_ACTION_SIZE = 96.dp

private const val BIOMETRIC_ICON_SIZE_SP: Float = 40f

private const val BIOMETRIC_ICON_LINE_HEIGHT_SP: Float = 48f
