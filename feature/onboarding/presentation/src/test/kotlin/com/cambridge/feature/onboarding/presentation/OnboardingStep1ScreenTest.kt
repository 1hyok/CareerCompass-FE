package com.cambridge.feature.onboarding.presentation

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextReplacement
import com.cambridge.core.ui.theme.CareerCompassTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
public class OnboardingStep1ScreenTest {
    @get:Rule
    public val composeRule = createComposeRule()

    @Test
    public fun missingRequiredValue_disablesNextAction() {
        val events = mutableListOf<OnboardingStep1Event>()

        setScreen(
            state = completeState.copy(name = "  "),
            onEvent = events::add,
        )

        nextButton()
            .assertIsNotEnabled()
            .performClick()

        assertTrue(events.isEmpty())
    }

    @Test
    public fun eachRequiredValue_participatesInNextActionValidation() {
        assertFalse(completeState.copy(name = "").isNextEnabled)
        assertFalse(completeState.copy(school = "").isNextEnabled)
        assertFalse(completeState.copy(major = "").isNextEnabled)
        assertFalse(
            completeState
                .copy(gradePointAverageError = "학점을 확인해 주세요")
                .isNextEnabled,
        )
    }

    @Test
    public fun blankErrorMessage_isRejectedByContract() {
        val exception =
            assertThrows(IllegalArgumentException::class.java) {
                OnboardingStep1UiState(nameError = "   ")
            }

        assertEquals("Field errors must be null or non-blank", exception.message)
    }

    @Test
    public fun completeRequiredValues_enableNextAction() {
        val events = mutableListOf<OnboardingStep1Event>()

        setScreen(state = completeState, onEvent = events::add)

        nextButton()
            .assertIsEnabled()
            .performClick()

        assertEquals(listOf(OnboardingStep1Event.NextClicked), events)
    }

    @Test
    public fun validationError_isAnnouncedAndDisablesNextAction() {
        setScreen(
            state = completeState.copy(majorError = "학과를 확인해 주세요"),
        )

        composeRule
            .onNodeWithContentDescription("학과 *")
            .assert(
                SemanticsMatcher.expectValue(
                    SemanticsProperties.Error,
                    "학과를 확인해 주세요",
                ),
            )
        nextButton()
            .assertIsDisplayed()
            .assertIsNotEnabled()
    }

    @Test
    public fun multipleValidationErrors_keepNextActionVisible() {
        setScreen(
            state =
                completeState.copy(
                    name = "",
                    gradePointAverage = "5.0",
                    nameError = "이름을 입력해 주세요",
                    gradePointAverageError = "4.5 이하로 입력해 주세요",
                ),
        )

        nextButton()
            .assertIsDisplayed()
            .assertIsNotEnabled()
    }

    @Test
    public fun disabledInputState_disablesFieldsAndNextAction() {
        setScreen(
            state = completeState.copy(isInputEnabled = false),
        )

        composeRule
            .onNodeWithContentDescription("이름 *")
            .assertIsNotEnabled()
        composeRule
            .onNodeWithContentDescription("학교 *")
            .assertIsNotEnabled()
        nextButton().assertIsNotEnabled()
    }

    @Test
    public fun disabledPickerFields_doNotEmitClickEvents() {
        val events = mutableListOf<OnboardingStep1Event>()

        setScreen(
            state = completeState.copy(isInputEnabled = false),
            onEvent = events::add,
        )

        composeRule
            .onNodeWithContentDescription("학교 *")
            .performClick()
        composeRule
            .onNodeWithContentDescription("졸업 예정")
            .performScrollTo()
            .performClick()

        assertTrue(events.isEmpty())
    }

    @Test
    public fun progress_exposesCurrentStepToAccessibilityServices() {
        setScreen(
            state = completeState.copy(currentStep = 2),
        )

        composeRule
            .onNodeWithContentDescription("4단계 중 2단계")
            .assert(
                SemanticsMatcher.expectValue(
                    SemanticsProperties.ProgressBarRangeInfo,
                    ProgressBarRangeInfo(
                        current = 2f,
                        range = 0f..4f,
                        steps = 3,
                    ),
                ),
            )
    }

    @Test
    public fun formControls_forwardExplicitEvents() {
        val events = mutableListOf<OnboardingStep1Event>()

        composeRule.setContent {
            var state by remember { mutableStateOf(completeState) }

            CareerCompassTheme {
                OnboardingStep1Screen(
                    state = state,
                    onEvent = { event ->
                        events += event
                        state = state.reduce(event)
                    },
                )
            }
        }

        composeRule
            .onNodeWithContentDescription("이름 *")
            .performTextReplacement("김일혁")
        composeRule
            .onNodeWithContentDescription("학과 *")
            .performTextReplacement("소프트웨어학부")
        composeRule
            .onNodeWithContentDescription("학점")
            .performTextReplacement("4.0")
        composeRule
            .onNodeWithContentDescription("학교 *")
            .performClick()
        composeRule
            .onNodeWithContentDescription("졸업 예정")
            .performScrollTo()
            .performClick()
        composeRule
            .onNodeWithContentDescription("뒤로가기")
            .performClick()

        assertEquals(
            listOf(
                OnboardingStep1Event.NameChanged("김일혁"),
                OnboardingStep1Event.MajorChanged("소프트웨어학부"),
                OnboardingStep1Event.GradePointAverageChanged("4.0"),
                OnboardingStep1Event.SchoolPickerClicked,
                OnboardingStep1Event.GraduationDatePickerClicked,
                OnboardingStep1Event.BackClicked,
            ),
            events,
        )
    }

    private fun setScreen(
        state: OnboardingStep1UiState,
        onEvent: (OnboardingStep1Event) -> Unit = {},
    ) {
        composeRule.setContent {
            CareerCompassTheme {
                OnboardingStep1Screen(
                    state = state,
                    onEvent = onEvent,
                )
            }
        }
    }

    private fun nextButton() =
        composeRule.onNode(
            hasText("다음") and hasClickAction(),
        )

    private fun OnboardingStep1UiState.reduce(event: OnboardingStep1Event): OnboardingStep1UiState =
        when (event) {
            is OnboardingStep1Event.NameChanged -> {
                copy(name = event.value)
            }

            is OnboardingStep1Event.MajorChanged -> {
                copy(major = event.value)
            }

            is OnboardingStep1Event.GradePointAverageChanged -> {
                copy(gradePointAverage = event.value)
            }

            else -> {
                this
            }
        }

    private companion object {
        val completeState =
            OnboardingStep1UiState(
                name = "정일혁",
                school = "건국대학교",
                major = "컴퓨터공학부",
                gradePointAverage = "3.87",
                graduationDate = "2027.02",
            )
    }
}
