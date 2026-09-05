package com.careercompass.feature.onboarding.presentation

import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.android.tools.screenshot.PreviewTest
import com.careercompass.core.ui.theme.CareerCompassTheme
import com.careercompass.feature.onboarding.presentation.biometric.BiometricEnrollFailureReason
import com.careercompass.feature.onboarding.presentation.biometric.BiometricEnrollSheet
import com.careercompass.feature.onboarding.presentation.biometric.BiometricEnrollUiState

@PreviewTest
@Preview(name = "Biometric enroll offer", widthDp = 360, heightDp = 420)
@Composable
public fun BiometricEnrollOfferPreview() {
    BiometricEnrollPreviewHost(state = BiometricEnrollUiState())
}

@PreviewTest
@Preview(name = "Biometric enroll registering", widthDp = 360, heightDp = 420)
@Composable
public fun BiometricEnrollRegisteringPreview() {
    BiometricEnrollPreviewHost(state = BiometricEnrollUiState(isRegistering = true))
}

@PreviewTest
@Preview(name = "Biometric enroll error", widthDp = 360, heightDp = 420)
@Composable
public fun BiometricEnrollErrorPreview() {
    BiometricEnrollPreviewHost(
        state = BiometricEnrollUiState(failure = BiometricEnrollFailureReason.Registration),
    )
}

@Composable
private fun BiometricEnrollPreviewHost(state: BiometricEnrollUiState) {
    CareerCompassTheme {
        Surface(color = CareerCompassTheme.colors.surface) {
            BiometricEnrollSheet(state = state, onIntent = {}, onEnrollClick = {})
        }
    }
}
