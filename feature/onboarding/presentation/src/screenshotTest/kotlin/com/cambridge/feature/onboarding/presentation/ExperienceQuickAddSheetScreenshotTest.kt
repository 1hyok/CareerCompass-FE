package com.cambridge.feature.onboarding.presentation

import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.android.tools.screenshot.PreviewTest
import com.cambridge.core.model.experience.ExperienceType
import com.cambridge.core.ui.theme.CareerCompassTheme
import com.cambridge.feature.onboarding.presentation.experience.ExperienceEditorState
import com.cambridge.feature.onboarding.presentation.experience.ExperienceQuickAddSheet
import com.cambridge.feature.onboarding.presentation.shared.model.OnboardingFieldError

@PreviewTest
@Preview(name = "Experience quick add project", widthDp = 360, heightDp = 800)
@Composable
public fun ExperienceQuickAddProjectPreview() {
    ExperienceQuickAddPreviewHost(
        state =
            ExperienceEditorState(
                type = ExperienceType.Project,
                title = "CareerCompass - 졸업 프로젝트",
                startDate = "2025.09",
                primary = "안드로이드 개발",
                secondary = "공고 자동 분석 + AI 자소서 생성 서비스",
            ),
    )
}

@PreviewTest
@Preview(name = "Experience quick add intern errors", widthDp = 360, heightDp = 800)
@Composable
public fun ExperienceQuickAddInternErrorPreview() {
    ExperienceQuickAddPreviewHost(
        state =
            ExperienceEditorState(
                type = ExperienceType.Intern,
                title = "카카오 인턴",
                startDateError = OnboardingFieldError.Required,
                primaryError = OnboardingFieldError.Required,
                secondaryError = OnboardingFieldError.Required,
            ),
    )
}

@PreviewTest
@Preview(name = "Experience quick add certificate submitting", widthDp = 360, heightDp = 800)
@Composable
public fun ExperienceQuickAddCertificateSubmittingPreview() {
    ExperienceQuickAddPreviewHost(
        state =
            ExperienceEditorState(
                type = ExperienceType.Certificate,
                title = "정보처리기사",
                startDate = "2025.06",
                primary = "한국산업인력공단",
                isSubmitting = true,
            ),
    )
}

@PreviewTest
@Preview(name = "Experience edit intern", widthDp = 360, heightDp = 800)
@Composable
public fun ExperienceEditInternPreview() {
    ExperienceQuickAddPreviewHost(
        state =
            ExperienceEditorState(
                experienceId = 3L,
                type = ExperienceType.Intern,
                title = "카카오 인턴",
                startDate = "2025.01",
                endDate = "2025.02",
                primary = "카카오",
                secondary = "안드로이드 개발",
            ),
    )
}

@Composable
private fun ExperienceQuickAddPreviewHost(state: ExperienceEditorState) {
    CareerCompassTheme {
        Surface(color = CareerCompassTheme.colors.surface) {
            ExperienceQuickAddSheet(state = state, onEvent = {})
        }
    }
}
