package com.careercompass.feature.onboarding.presentation

import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.android.tools.screenshot.PreviewTest
import com.careercompass.core.model.application.UploadFile
import com.careercompass.core.ui.theme.CareerCompassTheme
import com.careercompass.feature.onboarding.presentation.pastapplication.UploadLabelSheet
import com.careercompass.feature.onboarding.presentation.pastapplication.UploadLabelState
import com.careercompass.feature.onboarding.presentation.shared.model.OnboardingFieldError
import java.io.ByteArrayInputStream

@PreviewTest
@Preview(name = "Upload label default", widthDp = 360, heightDp = 480)
@Composable
public fun UploadLabelDefaultPreview() {
    UploadLabelPreviewHost(state = UploadLabelState(file = previewFile(), label = "이력서_최종_v3(2)"))
}

@PreviewTest
@Preview(name = "Upload label edited", widthDp = 360, heightDp = 480)
@Composable
public fun UploadLabelEditedPreview() {
    UploadLabelPreviewHost(state = UploadLabelState(file = previewFile(), label = "2024 카카오 인턴 자소서"))
}

@PreviewTest
@Preview(name = "Upload label error", widthDp = 360, heightDp = 480)
@Composable
public fun UploadLabelErrorPreview() {
    UploadLabelPreviewHost(
        state =
            UploadLabelState(
                file = previewFile(),
                label = "   ",
                labelError = OnboardingFieldError.Required,
            ),
    )
}

@Composable
private fun UploadLabelPreviewHost(state: UploadLabelState) {
    CareerCompassTheme {
        Surface(color = CareerCompassTheme.colors.surface) {
            UploadLabelSheet(state = state, onEvent = {})
        }
    }
}

private fun previewFile() = UploadFile(fileName = "이력서_최종_v3(2).pdf", sizeBytes = 1_024L) { ByteArrayInputStream(ByteArray(0)) }
