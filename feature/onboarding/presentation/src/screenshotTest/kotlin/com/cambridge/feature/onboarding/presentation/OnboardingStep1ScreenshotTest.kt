package com.cambridge.feature.onboarding.presentation

import android.content.res.Configuration
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.android.tools.screenshot.PreviewTest
import com.cambridge.core.ui.theme.CareerCompassTheme

@PreviewTest
@Preview(name = "Onboarding Step 1 filled", widthDp = 360, heightDp = 800)
@Composable
public fun OnboardingStep1FilledPreview() {
    CareerCompassTheme {
        OnboardingStep1Screen(
            state = onboardingStep1FilledState(),
            onEvent = {},
        )
    }
}

@PreviewTest
@Preview(name = "Onboarding Step 1 filled - Dark", uiMode = Configuration.UI_MODE_NIGHT_YES, widthDp = 360, heightDp = 800)
@Composable
public fun OnboardingStep1FilledDarkPreview() {
    CareerCompassTheme(darkTheme = true) {
        OnboardingStep1Screen(
            state = onboardingStep1FilledState(),
            onEvent = {},
        )
    }
}

@PreviewTest
@Preview(name = "Onboarding Step 1 errors", widthDp = 360, heightDp = 800)
@Composable
public fun OnboardingStep1ErrorPreview() {
    CareerCompassTheme {
        OnboardingStep1Screen(
            state =
                OnboardingStep1UiState(
                    name = "",
                    school = "건국대학교",
                    major = "컴퓨터공학부",
                    gradePointAverage = "5.0",
                    graduationDate = "2027.02",
                    nameError = "이름을 입력해 주세요",
                    gradePointAverageError = "4.5 이하로 입력해 주세요",
                ),
            onEvent = {},
        )
    }
}

@PreviewTest
@Preview(name = "Onboarding Step 1 disabled", widthDp = 360, heightDp = 800)
@Composable
public fun OnboardingStep1DisabledPreview() {
    CareerCompassTheme {
        OnboardingStep1Screen(
            state =
                OnboardingStep1UiState(
                    name = "정일혁",
                    school = "건국대학교",
                    major = "컴퓨터공학부",
                    gradePointAverage = "3.87",
                    graduationDate = "2027.02",
                    isInputEnabled = false,
                ),
            onEvent = {},
        )
    }
}

private fun onboardingStep1FilledState(): OnboardingStep1UiState =
    OnboardingStep1UiState(
        name = "정일혁",
        school = "건국대학교",
        major = "컴퓨터공학부",
        gradePointAverage = "3.87",
        graduationDate = "2027.02",
    )
