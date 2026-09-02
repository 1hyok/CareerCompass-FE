package com.cambridge.feature.onboarding.presentation

import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.android.tools.screenshot.PreviewTest
import com.cambridge.core.ui.theme.CareerCompassTheme
import com.cambridge.feature.onboarding.presentation.complete.OnboardingCompleteScreen
import com.cambridge.feature.onboarding.presentation.complete.OnboardingCompleteUiState

@PreviewTest
@Preview(name = "Onboarding complete named", widthDp = 360, heightDp = 800)
@Composable
public fun OnboardingCompleteNamedPreview() {
    OnboardingCompletePreviewHost(state = OnboardingCompleteUiState(userName = "일혁"))
}

@PreviewTest
@Preview(name = "Onboarding complete anonymous", widthDp = 360, heightDp = 800)
@Composable
public fun OnboardingCompleteAnonymousPreview() {
    OnboardingCompletePreviewHost(state = OnboardingCompleteUiState())
}

@Composable
private fun OnboardingCompletePreviewHost(state: OnboardingCompleteUiState) {
    CareerCompassTheme {
        Surface(color = CareerCompassTheme.colors.subtleSurface) {
            OnboardingCompleteScreen(state = state, onEvent = {})
        }
    }
}
