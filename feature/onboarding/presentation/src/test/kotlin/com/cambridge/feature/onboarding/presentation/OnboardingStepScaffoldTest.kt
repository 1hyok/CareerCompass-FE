package com.cambridge.feature.onboarding.presentation

import androidx.compose.material3.Text
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.careercompass.core.ui.theme.CareerCompassTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
public class OnboardingStepScaffoldTest {
    @get:Rule
    public val composeRule = createComposeRule()

    @Test
    public fun sharedChromeAndSlots_renderStepSpecificContent() {
        setScaffold()

        composeRule.onNodeWithText("STEP 3 / 4").assertIsDisplayed()
        composeRule.onNodeWithText("관심 직무를 선택해 주세요").assertIsDisplayed()
        composeRule.onNodeWithText("여러 개 선택할 수 있어요").assertIsDisplayed()
        composeRule.onNodeWithText("직무 선택 영역").assertIsDisplayed()
        composeRule
            .onNodeWithContentDescription("4단계 중 3단계")
            .assert(
                SemanticsMatcher.expectValue(
                    SemanticsProperties.ProgressBarRangeInfo,
                    ProgressBarRangeInfo(
                        current = 3f,
                        range = 0f..4f,
                        steps = 3,
                    ),
                ),
            )
        composeRule
            .onNode(hasText("다음") and hasClickAction())
            .assertIsDisplayed()
    }

    @Test
    public fun sharedActions_forwardTheirCallbacks() {
        val actions = mutableListOf<String>()

        setScaffold(
            onBackClick = { actions += "back" },
            onPrimaryActionClick = { actions += "next" },
        )

        composeRule
            .onNodeWithContentDescription("뒤로가기")
            .performClick()
        composeRule
            .onNode(hasText("다음") and hasClickAction())
            .performClick()

        assertEquals(listOf("back", "next"), actions)
    }

    private fun setScaffold(
        onBackClick: () -> Unit = { },
        onPrimaryActionClick: () -> Unit = { },
    ) {
        composeRule.setContent {
            CareerCompassTheme {
                OnboardingStepScaffold(
                    currentStep = 3,
                    totalSteps = 4,
                    title = "관심 직무를 선택해 주세요",
                    description = "여러 개 선택할 수 있어요",
                    onBackClick = onBackClick,
                    footerContent = {
                        OnboardingPrimaryActionFooter(
                            text = "다음",
                            enabled = true,
                            onClick = onPrimaryActionClick,
                        )
                    },
                ) {
                    Text("직무 선택 영역")
                }
            }
        }
    }
}
