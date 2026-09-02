package com.cambridge.feature.onboarding.presentation

import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.android.tools.screenshot.PreviewTest
import com.cambridge.core.ui.theme.CareerCompassTheme
import com.cambridge.feature.onboarding.presentation.login.LoginScreen
import com.cambridge.feature.onboarding.presentation.login.LoginUiState

@PreviewTest
@Preview(name = "Login default", widthDp = 360, heightDp = 800)
@Composable
public fun LoginDefaultPreview() {
    LoginPreviewHost(state = LoginUiState())
}

@PreviewTest
@Preview(name = "Login loading", widthDp = 360, heightDp = 800)
@Composable
public fun LoginLoadingPreview() {
    LoginPreviewHost(state = LoginUiState(isLoading = true))
}

@PreviewTest
@Preview(name = "Login error", widthDp = 360, heightDp = 800)
@Composable
public fun LoginErrorPreview() {
    LoginPreviewHost(state = LoginUiState(errorMessage = "카카오 로그인에 실패했어요. 다시 시도해 주세요."))
}

@Composable
private fun LoginPreviewHost(state: LoginUiState) {
    CareerCompassTheme {
        Surface(color = CareerCompassTheme.colors.subtleSurface) {
            LoginScreen(state = state, onEvent = {})
        }
    }
}
