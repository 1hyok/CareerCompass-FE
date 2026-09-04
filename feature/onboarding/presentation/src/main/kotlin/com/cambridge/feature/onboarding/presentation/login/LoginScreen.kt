package com.cambridge.feature.onboarding.presentation.login

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.cambridge.feature.onboarding.presentation.R
import com.cambridge.feature.onboarding.presentation.login.component.GoogleLoginButton
import com.cambridge.feature.onboarding.presentation.login.component.KakaoLoginButton
import com.cambridge.feature.onboarding.presentation.shared.component.OnboardingBrandMark
import com.cambridge.feature.onboarding.presentation.shared.component.OnboardingCenteredLayout
import com.cambridge.feature.onboarding.presentation.shared.component.OnboardingErrorCard
import com.careercompass.core.ui.theme.CareerCompassTheme

/** Stateless social login entry screen shown before onboarding. */
@Composable
public fun LoginScreen(
    state: LoginUiState,
    onEvent: (LoginEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    OnboardingCenteredLayout(
        topContent = null,
        modifier = modifier,
        centerContent = { LoginBrandHeader() },
        bottomContent = {
            if (state.errorMessage != null) {
                OnboardingErrorCard(
                    message = state.errorMessage,
                    onDismissClick = { onEvent(LoginEvent.ErrorDismissed) },
                )
            }
            if (state.isLoading) {
                LoginProgress()
            }
            SocialLoginButtons(
                enabled = state.isActionEnabled,
                onKakaoClick = { onEvent(LoginEvent.KakaoLoginClicked) },
                onGoogleClick = { onEvent(LoginEvent.GoogleLoginClicked) },
            )
            Text(
                text = stringResource(R.string.onboarding_login_terms_notice),
                color = CareerCompassTheme.colors.mutedContent,
                textAlign = TextAlign.Center,
                style = CareerCompassTheme.typography.caption,
            )
        },
    )
}

@Composable
private fun LoginBrandHeader() {
    val colors = CareerCompassTheme.colors
    val spacing = CareerCompassTheme.spacing

    Column(
        modifier = Modifier.semantics(mergeDescendants = true) {},
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(spacing.medium),
    ) {
        OnboardingBrandMark(size = 56.dp, contentDescription = null)
        Text(
            text = stringResource(R.string.onboarding_app_name),
            color = colors.onSurface,
            textAlign = TextAlign.Center,
            style = CareerCompassTheme.typography.headline1,
        )
        Text(
            text = stringResource(R.string.onboarding_login_tagline),
            color = colors.mutedContent,
            textAlign = TextAlign.Center,
            style = CareerCompassTheme.typography.bodyMedium,
        )
    }
}

@Composable
private fun LoginProgress() {
    val colors = CareerCompassTheme.colors
    val spacing = CareerCompassTheme.spacing

    Row(
        modifier =
            Modifier.semantics(mergeDescendants = true) {
                liveRegion = LiveRegionMode.Polite
            },
        horizontalArrangement = Arrangement.spacedBy(spacing.small),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        CircularProgressIndicator(
            modifier =
                Modifier
                    .size(20.dp)
                    .clearAndSetSemantics {},
            color = colors.primaryEmphasis,
            strokeWidth = 2.dp,
        )
        Text(
            text = stringResource(R.string.onboarding_login_loading),
            color = colors.onSurfaceVariant,
            style = CareerCompassTheme.typography.bodyMedium,
        )
    }
}

@Composable
private fun SocialLoginButtons(
    enabled: Boolean,
    onKakaoClick: () -> Unit,
    onGoogleClick: () -> Unit,
) {
    val spacing = CareerCompassTheme.spacing

    Column(verticalArrangement = Arrangement.spacedBy(spacing.medium)) {
        KakaoLoginButton(onClick = onKakaoClick, enabled = enabled)
        GoogleLoginButton(onClick = onGoogleClick, enabled = enabled)
    }
}
