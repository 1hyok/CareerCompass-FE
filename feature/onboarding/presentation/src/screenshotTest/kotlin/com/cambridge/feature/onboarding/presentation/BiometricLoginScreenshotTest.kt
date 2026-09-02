package com.cambridge.feature.onboarding.presentation

import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.android.tools.screenshot.PreviewTest
import com.cambridge.core.ui.theme.CareerCompassTheme
import com.cambridge.feature.onboarding.presentation.biometric.BiometricLoginScreen
import com.cambridge.feature.onboarding.presentation.biometric.BiometricLoginUiState

@PreviewTest
@Preview(name = "Biometric login default", widthDp = 360, heightDp = 800)
@Composable
public fun BiometricLoginDefaultPreview() {
    BiometricLoginPreviewHost(state = biometricLoginPreviewState())
}

@PreviewTest
@Preview(name = "Biometric login authenticating", widthDp = 360, heightDp = 800)
@Composable
public fun BiometricLoginAuthenticatingPreview() {
    BiometricLoginPreviewHost(state = biometricLoginPreviewState().copy(isAuthenticating = true))
}

@PreviewTest
@Preview(name = "Biometric login error", widthDp = 360, heightDp = 800)
@Composable
public fun BiometricLoginErrorPreview() {
    BiometricLoginPreviewHost(
        state = biometricLoginPreviewState().copy(errorMessage = "지문을 인식하지 못했어요. 다시 시도해 주세요."),
    )
}

@Composable
private fun BiometricLoginPreviewHost(state: BiometricLoginUiState) {
    CareerCompassTheme {
        Surface(color = CareerCompassTheme.colors.subtleSurface) {
            BiometricLoginScreen(state = state, onEvent = {})
        }
    }
}

private fun biometricLoginPreviewState(): BiometricLoginUiState =
    BiometricLoginUiState(
        userName = "일혁",
        accountLabel = "1hyok@konkuk.ac.kr",
    )
