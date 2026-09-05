package com.careercompass.feature.onboarding.presentation

import android.content.res.Configuration
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.android.tools.screenshot.PreviewTest
import com.careercompass.core.ui.theme.CareerCompassTheme
import com.careercompass.feature.onboarding.presentation.login.LoginContent
import com.careercompass.feature.onboarding.presentation.login.LoginFailureReason
import com.careercompass.feature.onboarding.presentation.login.LoginUiState

@PreviewTest
@Preview(name = "Login default", widthDp = 360, heightDp = 800)
@Composable
public fun LoginDefaultPreview() {
    LoginPreviewHost(state = LoginUiState())
}

@PreviewTest
@Preview(name = "Login default - Dark", uiMode = Configuration.UI_MODE_NIGHT_YES, widthDp = 360, heightDp = 800)
@Composable
public fun LoginDefaultDarkPreview() {
    LoginPreviewHost(state = LoginUiState(), darkTheme = true)
}

/**
 * 앱의 첫 화면 — 여기서 버튼 문구가 잘리면 사용자가 로그인 방법을 못 고른다.
 *
 * 단말 높이를 그대로 둔다. 큰 글꼴에서 실제로 화면 밖으로 밀리는 것이 이 골든의 관측 대상이다.
 */
@PreviewTest
@Preview(name = "Login default - Large font", widthDp = 360, heightDp = 800, fontScale = LARGE_FONT_SCALE)
@Composable
public fun LoginDefaultLargeFontPreview() {
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
    LoginPreviewHost(state = LoginUiState(failure = LoginFailureReason.Rejected))
}

@Composable
private fun LoginPreviewHost(
    state: LoginUiState,
    darkTheme: Boolean = false,
) {
    CareerCompassTheme(darkTheme = darkTheme) {
        Surface(color = CareerCompassTheme.colors.subtleSurface) {
            LoginContent(
                state = state,
                isSessionExpiryNoticeVisible = false,
                onIntent = {},
                onSocialLoginClick = {},
                onSessionExpiryNoticeDismissed = {},
            )
        }
    }
}
