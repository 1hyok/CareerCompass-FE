package com.careercompass.feature.onboarding.presentation

import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.android.tools.screenshot.PreviewTest
import com.careercompass.core.ui.theme.CareerCompassTheme
import com.careercompass.feature.onboarding.presentation.biometric.BiometricFailureReason
import com.careercompass.feature.onboarding.presentation.biometric.BiometricLoginContent
import com.careercompass.feature.onboarding.presentation.biometric.BiometricLoginUiState

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
    BiometricLoginPreviewHost(state = biometricLoginPreviewState().copy(failure = BiometricFailureReason.Failed))
}

@Composable
private fun BiometricLoginPreviewHost(state: BiometricLoginUiState) {
    CareerCompassTheme {
        Surface(color = CareerCompassTheme.colors.subtleSurface) {
            BiometricLoginContent(state = state, onIntent = {}, onBiometricClick = {})
        }
    }
}

private fun biometricLoginPreviewState(): BiometricLoginUiState = BiometricLoginUiState(userName = "일혁", isBiometricEnabled = true)
