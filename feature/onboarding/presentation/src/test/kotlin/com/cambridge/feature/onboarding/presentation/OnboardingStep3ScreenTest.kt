package com.cambridge.feature.onboarding.presentation

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertHeightIsEqualTo
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasStateDescription
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.width
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
@Config(sdk = [35], qualifiers = "w360dp-h800dp")
public class OnboardingStep3ScreenTest {
    @get:Rule
    public val composeRule = createComposeRule()

    @Test
    public fun malformedExperienceData_isRejectedByContract() {
        assertThrows(IllegalArgumentException::class.java) {
            completeState.copy(
                experienceTypes = experienceTypes + experienceTypes.first(),
            )
        }
        assertThrows(IllegalArgumentException::class.java) {
            completeState.copy(selectedExperienceTypeId = "missing")
        }
        assertThrows(IllegalArgumentException::class.java) {
            completeState.copy(
                experiences = experiences + experiences.first().copy(id = "library"),
            )
        }
        assertThrows(IllegalArgumentException::class.java) {
            experiences.first().copy(tags = listOf("Kotlin", "Kotlin"))
        }
    }

    @Test
    public fun contract_rejectsDifferentExperienceTypeIdsWithTheSameLabel() {
        assertThrows(IllegalArgumentException::class.java) {
            completeState.copy(
                experienceTypes =
                    experienceTypes +
                        OnboardingExperienceType(
                            id = "side-project",
                            label = "프로젝트",
                        ),
            )
        }
    }

    @Test
    public fun contract_acceptsExperienceTypesWithUniqueLabels() {
        assertEquals(experienceTypes, completeState.experienceTypes)
    }

    @Test
    public fun experiencePresence_controlsNextAction() {
        assertFalse(completeState.copy(experiences = emptyList()).isNextEnabled)
        assertFalse(completeState.copy(isInputEnabled = false).isNextEnabled)
        assertTrue(completeState.isNextEnabled)

        setScreen(state = completeState.copy(experiences = emptyList()))

        nextButton().assertIsNotEnabled()
    }

    @Test
    public fun selectedFilter_exposesRadioStateAndFiltersCards() {
        setScreen(state = completeState)

        composeRule
            .onNode(SemanticsMatcher.keyIsDefined(SemanticsProperties.SelectableGroup))
            .assertExists()
        composeRule
            .onNode(hasText("프로젝트") and hasStateDescription("선택됨"))
            .assertIsSelected()
            .assert(
                SemanticsMatcher.expectValue(
                    SemanticsProperties.Role,
                    Role.RadioButton,
                ),
            )
        composeRule
            .onNode(hasText("수상") and hasStateDescription("선택 안 됨"))
            .assertIsNotSelected()
        composeRule.onNodeWithText("CareerCompass - 졸업 프로젝트").assertExists()
        composeRule.onNodeWithText("학교 도서관 좌석 알리미").assertExists()
        composeRule.onAllNodesWithText("교내 해커톤 최우수상").assertCountEquals(0)
    }

    @Test
    public fun compactFigmaControls_keepFiltersOnOneRowAndAddActionContentWidth() {
        setScreen(state = completeState)

        val filterBounds =
            experienceTypes.map { type ->
                composeRule
                    .onNodeWithText(type.label)
                    .getUnclippedBoundsInRoot()
            }
        val firstFilterTop = filterBounds.first().top
        assertTrue(
            "All five experience filters must remain on one row at 360dp",
            filterBounds.all { it.top == firstFilterTop },
        )

        val addAction =
            composeRule
                .onNode(hasText("경험 추가하기") and hasClickAction())
                .performScrollTo()
                .assertHeightIsEqualTo(52.dp)
        val addActionWidth = addAction.getUnclippedBoundsInRoot().width
        assertTrue(
            "Add action width was $addActionWidth; expected the compact Figma content width",
            addActionWidth >= 98.dp && addActionWidth <= 110.dp,
        )
    }

    @Test
    public fun controls_emitSeparateExplicitEvents() {
        val events = mutableListOf<OnboardingStep3Event>()
        setScreen(state = completeState, onEvent = events::add)

        composeRule
            .onNode(hasText("수상") and hasStateDescription("선택 안 됨"))
            .performClick()
        composeRule
            .onNode(hasText("CareerCompass - 졸업 프로젝트") and hasClickAction())
            .performClick()
        composeRule
            .onNode(hasText("경험 추가하기") and hasClickAction())
            .performScrollTo()
            .performClick()
        composeRule.onNodeWithContentDescription("뒤로가기").performClick()
        nextButton().performClick()

        composeRule.runOnIdle {
            assertEquals(
                listOf(
                    OnboardingStep3Event.ExperienceTypeSelected("award"),
                    OnboardingStep3Event.ExperienceSelected("career-compass"),
                    OnboardingStep3Event.AddExperienceClicked,
                    OnboardingStep3Event.BackClicked,
                    OnboardingStep3Event.NextClicked,
                ),
                events,
            )
        }
    }

    @Test
    public fun disabledState_disablesEveryMutatingAction() {
        val events = mutableListOf<OnboardingStep3Event>()
        setScreen(
            state = completeState.copy(isInputEnabled = false),
            onEvent = events::add,
        )

        composeRule
            .onNode(hasText("수상") and hasStateDescription("선택 안 됨"))
            .assertIsNotEnabled()
            .performClick()
        composeRule
            .onNodeWithText("CareerCompass - 졸업 프로젝트")
            .assertIsNotEnabled()
            .performClick()
        composeRule
            .onNodeWithText("경험 추가하기")
            .performScrollTo()
            .assertIsNotEnabled()
            .performClick()
        nextButton()
            .assertIsNotEnabled()
            .performClick()

        assertTrue(events.isEmpty())
    }

    @Test
    public fun largeFontScale_keepsCardsAddActionAndFooterReachable() {
        setScreen(state = completeState, fontScale = 2f)

        composeRule
            .onNodeWithText("학교 도서관 좌석 알리미")
            .performScrollTo()
            .assertIsDisplayed()
        composeRule
            .onNodeWithText("경험 추가하기")
            .performScrollTo()
            .assertIsDisplayed()
        nextButton()
            .assertIsDisplayed()
            .assertIsEnabled()
    }

    private fun setScreen(
        state: OnboardingStep3UiState,
        onEvent: (OnboardingStep3Event) -> Unit = {},
        fontScale: Float = 1f,
    ) {
        composeRule.setContent {
            val currentDensity = LocalDensity.current
            CompositionLocalProvider(
                LocalDensity provides Density(currentDensity.density, fontScale),
            ) {
                CareerCompassTheme {
                    OnboardingStep3Screen(state = state, onEvent = onEvent)
                }
            }
        }
    }

    private fun nextButton() =
        composeRule.onNode(
            hasText("다음") and hasClickAction(),
        )

    private companion object {
        val experienceTypes =
            listOf(
                OnboardingExperienceType(id = "project", label = "프로젝트"),
                OnboardingExperienceType(id = "award", label = "수상"),
                OnboardingExperienceType(id = "internship", label = "인턴"),
                OnboardingExperienceType(id = "activity", label = "대외활동"),
                OnboardingExperienceType(id = "certificate", label = "자격증"),
            )

        val experiences =
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
                OnboardingExperience(
                    id = "hackathon",
                    typeId = "award",
                    title = "교내 해커톤 최우수상",
                    period = "2024.11",
                    role = "팀 리드",
                    tags = listOf("기획", "발표"),
                ),
            )

        val completeState =
            OnboardingStep3UiState(
                experienceTypes = experienceTypes,
                selectedExperienceTypeId = "project",
                experiences = experiences,
            )
    }
}
