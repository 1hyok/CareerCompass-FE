package com.cambridge.feature.feed.presentation.board

import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasStateDescription
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.ComposeContentTestRule
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextReplacement
import com.cambridge.core.ui.theme.CareerCompassTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class BoardEditSheetContentTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun urlIsReadOnly_andTypeCycleFormRadioGroups() {
        composeRule.setEditContent(state = sampleState())

        composeRule.onNodeWithText("게시판 수정").assertIsDisplayed()
        composeRule.onNode(hasText(BOARD_NAME) and !hasSetTextAction()).assertIsDisplayed()
        composeRule.onNodeWithContentDescription("게시판 이름 *").assert(hasText(BOARD_NAME))
        composeRule.onNodeWithText(BOARD_URL).assertIsDisplayed()
        composeRule.onNodeWithText("URL 은 바꿀 수 없어요. 주소가 달라졌다면 게시판을 새로 등록해 주세요").assertIsDisplayed()
        composeRule
            .onAllNodes(SemanticsMatcher.keyIsDefined(SemanticsProperties.SelectableGroup))
            .assertCountEquals(2)
        composeRule
            .onNode(hasText("채용") and hasStateDescription("선택됨"))
            .assertIsOn()
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.RadioButton))
        composeRule.onNode(hasText("1일 1회") and hasStateDescription("선택됨")).assertIsOn()
    }

    @Test
    fun saveButton_followsIsSaveEnabled() {
        composeRule.setEditContent(state = sampleState(hasChanges = false))

        composeRule.onNodeWithContentDescription("$BOARD_NAME 수정 저장").assertIsNotEnabled()
        composeRule.onNode(hasText("취소") and hasClickAction()).assertIsEnabled()
    }

    @Test
    fun nameError_isShownUnderField() {
        composeRule.setEditContent(state = sampleState(name = "", nameError = "게시판 이름을 입력해 주세요", hasChanges = false))

        composeRule.onNodeWithText("게시판 이름을 입력해 주세요").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("$BOARD_NAME 수정 저장").assertIsNotEnabled()
    }

    @Test
    fun saving_locksInputsAndAnnouncesProgress() {
        composeRule.setEditContent(state = sampleState(isSaving = true))

        composeRule.onNodeWithText("변경 내용을 저장하고 있어요").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithContentDescription("게시판 이름 *").assertIsNotEnabled()
        composeRule.onNodeWithText("장학금").assertIsNotEnabled()
        composeRule.onNodeWithText("주 1회").assertIsNotEnabled()
        composeRule.onNodeWithContentDescription("$BOARD_NAME 수정 저장").assertIsNotEnabled()
        composeRule.onNode(hasText("취소") and hasClickAction()).assertIsNotEnabled()
    }

    @Test
    fun controls_emitSeparateIntents() {
        val events = mutableListOf<BoardEditEvent>()
        composeRule.setEditContent(state = sampleState(), onEvent = events::add)

        composeRule.onNodeWithContentDescription("게시판 이름 *").performTextReplacement("건국대 취업 공지")
        composeRule.onNode(hasText("장학금") and hasStateDescription("선택 안 됨")).performClick()
        composeRule.onNode(hasText("주 1회") and hasStateDescription("선택 안 됨")).performScrollTo().performClick()
        composeRule.onNodeWithContentDescription("$BOARD_NAME 수정 저장").performClick()
        composeRule.onNode(hasText("취소") and hasClickAction()).performClick()
        composeRule.onNodeWithContentDescription("수정 닫기").performClick()

        composeRule.runOnIdle {
            assertEquals(
                listOf(
                    BoardEditEvent.NameChanged("건국대 취업 공지"),
                    BoardEditEvent.TypeSelected(BoardType.Scholarship),
                    BoardEditEvent.CycleSelected(BoardCollectCycle.Weekly),
                    BoardEditEvent.SaveClicked,
                    BoardEditEvent.DismissClicked,
                    BoardEditEvent.DismissClicked,
                ),
                events,
            )
        }
    }
}

private const val BOARD_NAME = "건국대 공지사항"
private const val BOARD_URL = "https://konkuk.ac.kr/board/notice"

private fun ComposeContentTestRule.setEditContent(
    state: BoardEditUiState,
    onEvent: (BoardEditEvent) -> Unit = {},
) {
    setContent {
        CareerCompassTheme {
            BoardEditSheetContent(state = state, onEvent = onEvent)
        }
    }
}

private fun sampleState(
    name: String = BOARD_NAME,
    nameError: String? = null,
    isSaving: Boolean = false,
    hasChanges: Boolean = true,
): BoardEditUiState =
    BoardEditUiState(
        boardName = BOARD_NAME,
        url = BOARD_URL,
        name = name,
        nameError = nameError,
        type = BoardType.Employment,
        cycle = BoardCollectCycle.Daily,
        isSaving = isSaving,
        hasChanges = hasChanges,
    )
