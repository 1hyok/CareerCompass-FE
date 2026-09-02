package com.cambridge.feature.onboarding.presentation.experience

import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextReplacement
import com.cambridge.core.model.experience.ExperienceType
import com.cambridge.core.ui.theme.CareerCompassTheme
import com.cambridge.feature.onboarding.presentation.shared.model.OnboardingFieldError
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], qualifiers = "w360dp-h800dp")
public class ExperienceQuickAddSheetTest {
    @get:Rule
    public val composeRule = createComposeRule()

    @Test
    public fun projectType_showsRoleAndSummaryWithRequiredStart() {
        setSheet(ExperienceEditorState())

        composeRule.onNodeWithText("경험 추가").assertIsDisplayed()
        composeRule.onNodeWithText("프로젝트").assertIsOn()
        composeRule.onNodeWithContentDescription("시작 (YYYY.MM) *").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("종료 (YYYY.MM)").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("역할").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("요약").performScrollTo().assertIsDisplayed()
        submitButton().assertIsNotEnabled()
    }

    @Test
    public fun internType_relabelsFieldsAsRequired() {
        setSheet(ExperienceEditorState(type = ExperienceType.Intern, title = "카카오 인턴"))

        composeRule.onNodeWithContentDescription("회사명 *").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithContentDescription("직무 *").performScrollTo().assertIsDisplayed()
        submitButton().performScrollTo().assertIsEnabled()
    }

    @Test
    public fun certificateType_hidesEndDateAndSecondary() {
        setSheet(ExperienceEditorState(type = ExperienceType.Certificate))

        composeRule.onNodeWithContentDescription("취득 연월 (YYYY.MM)").assertIsDisplayed()
        composeRule.onAllNodesWithContentDescription("종료 (YYYY.MM)").assertCountEquals(0)
        composeRule.onNodeWithContentDescription("발급 기관").performScrollTo().assertIsDisplayed()
        composeRule.onAllNodesWithContentDescription("성과 요약").assertCountEquals(0)
    }

    @Test
    public fun fieldErrors_areRendered() {
        setSheet(
            ExperienceEditorState(
                type = ExperienceType.Intern,
                title = "카카오 인턴",
                startDateError = OnboardingFieldError.Required,
                primaryError = OnboardingFieldError.TooLong(100),
            ),
        )

        composeRule.onNodeWithText("필수 입력이에요").assertIsDisplayed()
        composeRule.onNodeWithText("100자 이내로 입력해 주세요").performScrollTo().assertIsDisplayed()
    }

    @Test
    public fun controls_emitDistinctEvents() {
        val events = mutableListOf<ExperienceQuickAddEvent>()
        setSheet(ExperienceEditorState(title = "제목"), onEvent = events::add)

        composeRule.onNodeWithText("수상").performClick()
        composeRule.onNodeWithContentDescription("제목 *").performTextReplacement("제목!")
        composeRule.onNodeWithContentDescription("시작 (YYYY.MM) *").performTextReplacement("2025.09")
        composeRule.onNodeWithContentDescription("역할").performScrollTo().performTextReplacement("안드로이드")
        submitButton().performScrollTo().performClick()
        composeRule.onNode(hasText("취소") and hasClickAction()).performClick()

        // stateless 필드는 호스트가 값을 되돌리지 않으므로 입력 이벤트는 첫 발생만 본다.
        composeRule.runOnIdle {
            assertEquals(
                ExperienceQuickAddEvent.TitleChanged("제목!"),
                events.filterIsInstance<ExperienceQuickAddEvent.TitleChanged>().first(),
            )
            assertEquals(
                ExperienceQuickAddEvent.StartDateChanged("2025.09"),
                events.filterIsInstance<ExperienceQuickAddEvent.StartDateChanged>().first(),
            )
            assertEquals(
                ExperienceQuickAddEvent.PrimaryChanged("안드로이드"),
                events.filterIsInstance<ExperienceQuickAddEvent.PrimaryChanged>().first(),
            )
            assertEquals(
                listOf(
                    ExperienceQuickAddEvent.TypeSelected(ExperienceType.Award),
                    ExperienceQuickAddEvent.Submitted,
                    ExperienceQuickAddEvent.Dismissed,
                ),
                events.filterNot { it.isTextChange() },
            )
        }
    }

    @Test
    public fun editingState_locksTypeAndRelabelsTitleAndSubmit() {
        setSheet(
            ExperienceEditorState(
                experienceId = 3L,
                type = ExperienceType.Intern,
                title = "카카오 인턴",
                startDate = "2025.01",
                primary = "카카오",
                secondary = "안드로이드 개발",
            ),
        )

        composeRule.onNodeWithText("경험 수정").assertIsDisplayed()
        composeRule.onAllNodesWithText("경험 추가").assertCountEquals(0)
        composeRule.onNodeWithText("인턴").assertIsNotEnabled()
        composeRule.onAllNodesWithText("프로젝트").assertCountEquals(0)
        composeRule.onNodeWithText("유형은 바꿀 수 없어요. 바꾸려면 삭제하고 다시 추가해 주세요").assertIsDisplayed()
        composeRule
            .onNode(hasText("저장하기") and hasClickAction())
            .performScrollTo()
            .assertIsEnabled()
    }

    @Test
    public fun editingWhileSaving_showsSavingLabel() {
        setSheet(ExperienceEditorState(experienceId = 3L, title = "카카오 인턴", isSubmitting = true))

        composeRule.onNodeWithText("저장하는 중").performScrollTo().assertIsNotEnabled()
    }

    @Test
    public fun submittingState_locksInputs() {
        setSheet(ExperienceEditorState(title = "제목", isSubmitting = true))

        composeRule.onNodeWithText("추가하는 중").performScrollTo().assertIsNotEnabled()
        composeRule.onNode(hasText("취소") and hasClickAction()).assertIsNotEnabled()
        composeRule.onNodeWithText("프로젝트").assertIsNotEnabled()
    }

    private fun submitButton() = composeRule.onNode(hasText("추가하기") and hasClickAction())

    private fun ExperienceQuickAddEvent.isTextChange(): Boolean =
        this is ExperienceQuickAddEvent.TitleChanged ||
            this is ExperienceQuickAddEvent.StartDateChanged ||
            this is ExperienceQuickAddEvent.EndDateChanged ||
            this is ExperienceQuickAddEvent.PrimaryChanged ||
            this is ExperienceQuickAddEvent.SecondaryChanged

    private fun setSheet(
        state: ExperienceEditorState,
        onEvent: (ExperienceQuickAddEvent) -> Unit = {},
    ) {
        composeRule.setContent {
            CareerCompassTheme {
                ExperienceQuickAddSheet(state = state, onEvent = onEvent)
            }
        }
    }
}
