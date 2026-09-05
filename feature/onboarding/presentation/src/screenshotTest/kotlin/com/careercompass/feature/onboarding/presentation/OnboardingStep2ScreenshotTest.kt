package com.careercompass.feature.onboarding.presentation

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.android.tools.screenshot.PreviewTest
import com.careercompass.core.ui.theme.CareerCompassTheme

@PreviewTest
@Preview(name = "Onboarding Step 2 selected", widthDp = 360, heightDp = 800)
@Composable
public fun OnboardingStep2SelectedPreview() {
    CareerCompassTheme {
        OnboardingStep2Content(
            state =
                previewState(
                    selectedJobIds = setOf("backend", "frontend"),
                    interestTags = listOf("AI", "스타트업", "환경"),
                ),
            onEvent = {},
        )
    }
}

@PreviewTest
@Preview(name = "Onboarding Step 2 empty", widthDp = 360, heightDp = 800)
@Composable
public fun OnboardingStep2EmptyPreview() {
    CareerCompassTheme {
        OnboardingStep2Content(
            state = previewState(),
            onEvent = {},
        )
    }
}

@PreviewTest
@Preview(name = "Onboarding Step 2 selection limit", widthDp = 360, heightDp = 800)
@Composable
public fun OnboardingStep2SelectionLimitPreview() {
    CareerCompassTheme {
        OnboardingStep2Content(
            state =
                previewState(
                    selectedJobIds = setOf("backend", "frontend", "data"),
                    interestTags = listOf("AI"),
                ),
            onEvent = {},
        )
    }
}

private fun previewState(
    selectedJobIds: Set<String> = emptySet(),
    interestTags: List<String> = emptyList(),
): OnboardingStep2UiState =
    OnboardingStep2UiState(
        jobOptions =
            listOf(
                OnboardingJobOption(id = "backend", label = "백엔드 개발"),
                OnboardingJobOption(id = "frontend", label = "프론트엔드"),
                OnboardingJobOption(id = "data", label = "데이터 분석"),
                OnboardingJobOption(id = "ai", label = "AI/ML"),
                OnboardingJobOption(id = "design", label = "디자인"),
                OnboardingJobOption(id = "pm", label = "기획/PM"),
                OnboardingJobOption(id = "marketing", label = "마케팅"),
            ),
        selectedJobIds = selectedJobIds,
        interestTags = interestTags,
    )
