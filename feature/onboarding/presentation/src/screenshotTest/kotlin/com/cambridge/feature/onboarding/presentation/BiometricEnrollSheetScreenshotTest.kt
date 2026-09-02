package com.cambridge.feature.onboarding.presentation

import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.android.tools.screenshot.PreviewTest
import com.cambridge.core.ui.theme.CareerCompassTheme
import com.cambridge.feature.onboarding.presentation.biometric.BiometricEnrollSheet
import com.cambridge.feature.onboarding.presentation.biometric.BiometricEnrollUiState

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
        state = BiometricEnrollUiState(errorMessage = "지금은 지문 로그인을 켜지 못했어요. 다시 시도해 주세요"),
    )
}

@Composable
private fun BiometricEnrollPreviewHost(state: BiometricEnrollUiState) {
    CareerCompassTheme {
        Surface(color = CareerCompassTheme.colors.surface) {
            BiometricEnrollSheet(state = state, onEvent = {})
        }
    }
}
