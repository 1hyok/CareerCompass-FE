package com.cambridge.feature.onboarding.presentation

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.android.tools.screenshot.PreviewTest
import com.careercompass.core.ui.theme.CareerCompassTheme

@PreviewTest
@Preview(name = "Onboarding Step 3 populated", widthDp = 360, heightDp = 800)
@Composable
public fun OnboardingStep3PopulatedPreview() {
    CareerCompassTheme {
        OnboardingStep3Screen(
            state = onboardingStep3PreviewState(),
            onEvent = {},
        )
    }
}

@PreviewTest
@Preview(name = "Onboarding Step 3 empty", widthDp = 360, heightDp = 800)
@Composable
public fun OnboardingStep3EmptyPreview() {
    CareerCompassTheme {
        OnboardingStep3Screen(
            state = onboardingStep3PreviewState().copy(experiences = emptyList()),
            onEvent = {},
        )
    }
}

@PreviewTest
@Preview(name = "Onboarding Step 3 disabled", widthDp = 360, heightDp = 800)
@Composable
public fun OnboardingStep3DisabledPreview() {
    CareerCompassTheme {
        OnboardingStep3Screen(
            state = onboardingStep3PreviewState().copy(isInputEnabled = false),
            onEvent = {},
        )
    }
}

private fun onboardingStep3PreviewState(): OnboardingStep3UiState =
    OnboardingStep3UiState(
        experienceTypes =
            listOf(
                OnboardingExperienceType(id = "project", label = "프로젝트"),
                OnboardingExperienceType(id = "award", label = "수상"),
                OnboardingExperienceType(id = "internship", label = "인턴"),
                OnboardingExperienceType(id = "activity", label = "대외활동"),
                OnboardingExperienceType(id = "certificate", label = "자격증"),
            ),
        selectedExperienceTypeId = "project",
        experiences =
            listOf(
                OnboardingExperience(
                    id = "career-compass",
                    typeId = "project",
                    title = "CareerCompass - 졸업 프로젝트",
                    period = "2025.09 — 진행 중",
                    role = "프론트엔드",
                    tags = listOf("Android", "Kotlin", "Compose"),
                ),
                OnboardingExperience(
                    id = "library",
                    typeId = "project",
                    title = "학교 도서관 좌석 알리미",
                    period = "2024.06 — 2024.08",
                    role = "백엔드",
                    tags = listOf("Spring", "JPA", "Redis"),
                ),
            ),
    )
