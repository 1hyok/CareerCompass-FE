package com.cambridge.feature.onboarding.presentation.biometric

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
import com.cambridge.core.ui.component.CareerCompassButton
import com.cambridge.core.ui.component.CareerCompassButtonSize
import com.cambridge.core.ui.component.CareerCompassButtonVariant
import com.cambridge.core.ui.theme.CareerCompassTheme
import com.cambridge.feature.onboarding.presentation.R
import com.cambridge.feature.onboarding.presentation.shared.component.OnboardingBrandMark
import com.cambridge.feature.onboarding.presentation.shared.component.OnboardingCenteredLayout
import com.cambridge.feature.onboarding.presentation.shared.component.OnboardingErrorCard

/** Stateless fingerprint quick-login screen for returning users. */
@Composable
public fun BiometricLoginScreen(
    state: BiometricLoginUiState,
    onEvent: (BiometricLoginEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    OnboardingCenteredLayout(
        topContent = {
            BiometricGreeting(
                userName = state.userName,
                accountLabel = state.accountLabel,
            )
        },
        modifier = modifier,
        centerContent = {
            BiometricPrompt(
                isAuthenticating = state.isAuthenticating,
                enabled = state.isBiometricEnabled,
                onClick = { onEvent(BiometricLoginEvent.BiometricClicked) },
            )
        },
        bottomContent = {
            if (state.errorMessage != null) {
                OnboardingErrorCard(
                    message = state.errorMessage,
                    onDismissClick = { onEvent(BiometricLoginEvent.ErrorDismissed) },
                )
            }
            CareerCompassButton(
                text = stringResource(R.string.onboarding_biometric_other_method),
                onClick = { onEvent(BiometricLoginEvent.OtherMethodClicked) },
                modifier = Modifier.fillMaxWidth(),
                variant = CareerCompassButtonVariant.Secondary,
                size = CareerCompassButtonSize.Large,
            )
        },
    )
}

@Composable
private fun BiometricGreeting(
    userName: String,
    accountLabel: String?,
) {
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
    if (accountLabel != null) {
        Spacer(modifier = Modifier.height(spacing.xxSmall))
        Text(
            text = accountLabel,
            color = colors.mutedContent,
            textAlign = TextAlign.Center,
            style = CareerCompassTheme.typography.caption,
        )
    }
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
