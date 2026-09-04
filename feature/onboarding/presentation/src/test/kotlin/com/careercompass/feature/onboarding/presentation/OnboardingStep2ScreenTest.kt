package com.careercompass.feature.onboarding.presentation

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasStateDescription
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.ComposeContentTestRule
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performImeAction
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.unit.Density
import com.careercompass.core.ui.theme.CareerCompassTheme
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
public class OnboardingStep2ScreenTest {
    @get:Rule
    public val composeRule = createComposeRule()

    @Test
    public fun contract_rejectsUnknownOrExcessSelectionsAndDuplicateTags() {
        assertThrows(IllegalArgumentException::class.java) {
            completeState.copy(selectedJobIds = setOf("unknown"))
        }
        assertThrows(IllegalArgumentException::class.java) {
            completeState.copy(
                selectedJobIds = setOf("backend", "frontend", "data"),
                maxJobSelections = 2,
            )
        }
        assertThrows(IllegalArgumentException::class.java) {
            completeState.copy(interestTags = listOf("AI", "AI"))
        }
    }

    @Test
    public fun contract_rejectsDifferentJobIdsWithTheSameLabel() {
        assertThrows(IllegalArgumentException::class.java) {
            completeState.copy(
                jobOptions =
                    jobOptions +
                        OnboardingJobOption(
                            id = "server",
                            label = "백엔드 개발",
                        ),
            )
        }
    }

    @Test
    public fun contract_acceptsJobOptionsWithUniqueLabels() {
        assertEquals(jobOptions, completeState.jobOptions)
    }

    @Test
    public fun bothRequiredSections_participateInNextActionValidation() {
        assertFalse(completeState.copy(selectedJobIds = emptySet()).isNextEnabled)
        assertFalse(completeState.copy(interestTags = emptyList()).isNextEnabled)
        assertFalse(completeState.copy(isInputEnabled = false).isNextEnabled)
        assertTrue(completeState.isNextEnabled)
    }

    @Test
    public fun selectedAndUnselectedJobs_exposeCheckboxAccessibilityState() {
        setScreen(state = completeState)

        composeRule
            .onNode(hasText("백엔드 개발") and hasStateDescription("선택됨"))
            .assertIsOn()
            .assert(
                SemanticsMatcher.expectValue(
                    SemanticsProperties.Role,
                    Role.Checkbox,
                ),
            )
        composeRule
            .onNode(hasText("데이터 분석") and hasStateDescription("선택 안 됨"))
            .assertIsOff()
    }

    @Test
    public fun selectionLimit_disablesOnlyUnselectedJobs() {
        val events = mutableListOf<OnboardingStep2Event>()
        setScreen(
            state =
                completeState.copy(
                    selectedJobIds = setOf("backend", "frontend"),
                    maxJobSelections = 2,
                ),
            onEvent = events::add,
        )

        composeRule
            .onNode(hasText("데이터 분석") and hasStateDescription("선택 안 됨"))
            .assertIsNotEnabled()
            .performClick()
        composeRule
            .onNode(hasText("백엔드 개발") and hasStateDescription("선택됨"))
            .assertIsEnabled()
            .performClick()

        assertEquals(
            listOf(OnboardingStep2Event.JobSelectionToggled("backend")),
            events,
        )
    }

    @Test
    public fun interestControlsAndNavigation_forwardExplicitEvents() {
        val events = mutableListOf<OnboardingStep2Event>()
        setScreen(
            state = completeState.copy(interestInput = "AI"),
            onEvent = events::add,
        )

        val interestField = composeRule.onNodeWithContentDescription("관심 분야 *")
        interestField.performTextReplacement("클라우드")
        interestField.performImeAction()
        val removableTag = composeRule.onNodeWithContentDescription("AI 태그 삭제")
        removableTag.performScrollTo()
        removableTag.assertIsDisplayed()
        removableTag.performClick()
        composeRule
            .onNodeWithContentDescription("뒤로가기")
            .performClick()
        nextButton().performClick()

        assertEquals(
            listOf(
                OnboardingStep2Event.InterestInputChanged("클라우드"),
                OnboardingStep2Event.InterestTagSubmitted,
                OnboardingStep2Event.InterestTagRemoved("AI"),
                OnboardingStep2Event.BackClicked,
                OnboardingStep2Event.NextClicked,
            ),
            events,
        )
    }

    @Test
    public fun incompleteState_keepsNextActionVisibleButDisabled() {
        val events = mutableListOf<OnboardingStep2Event>()
        setScreen(
            state = completeState.copy(interestTags = emptyList()),
            onEvent = events::add,
        )

        nextButton()
            .assertIsDisplayed()
            .assertIsNotEnabled()
            .performClick()

        assertTrue(events.isEmpty())
    }

    @Test
    public fun largeFontScale_wrapsOptionsAndKeepsPrimaryActionVisible() {
        composeRule.setScreenContent(state = completeState, fontScale = 2f)

        composeRule.onNode(hasText("데이터 분석")).assertIsDisplayed()
        nextButton().assertIsDisplayed()
    }

    private fun setScreen(
        state: OnboardingStep2UiState,
        onEvent: (OnboardingStep2Event) -> Unit = {},
    ) {
        composeRule.setScreenContent(state = state, onEvent = onEvent)
    }

    private fun ComposeContentTestRule.setScreenContent(
        state: OnboardingStep2UiState,
        onEvent: (OnboardingStep2Event) -> Unit = {},
        fontScale: Float = 1f,
    ) {
        setContent {
            val currentDensity = LocalDensity.current
            CompositionLocalProvider(
                LocalDensity provides Density(currentDensity.density, fontScale),
            ) {
                CareerCompassTheme {
                    OnboardingStep2Screen(state = state, onEvent = onEvent)
                }
            }
        }
    }

    private fun nextButton() =
        composeRule.onNode(
            hasText("다음") and hasClickAction(),
        )

    private companion object {
        val jobOptions =
            listOf(
                OnboardingJobOption(id = "backend", label = "백엔드 개발"),
                OnboardingJobOption(id = "frontend", label = "프론트엔드"),
                OnboardingJobOption(id = "data", label = "데이터 분석"),
            )

        val completeState =
            OnboardingStep2UiState(
                jobOptions = jobOptions,
                selectedJobIds = setOf("backend"),
                interestTags = listOf("AI", "스타트업", "환경"),
            )
    }
}
