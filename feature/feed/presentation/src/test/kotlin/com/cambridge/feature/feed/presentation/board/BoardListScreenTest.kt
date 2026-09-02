package com.cambridge.feature.feed.presentation.board

import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.ComposeContentTestRule
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import com.cambridge.core.ui.theme.CareerCompassTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class BoardListScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun loaded_showsCountStatusAndActivationSwitches() {
        composeRule.setListContent(state = loadedState())

        composeRule.onNodeWithText("2/20개 등록").assertIsDisplayed()
        composeRule.onNodeWithText("수집 중").assertIsDisplayed()
        composeRule.onNodeWithText("수집 실패 3회").performScrollTo().assertIsDisplayed()
        composeRule
            .onNodeWithContentDescription("$ACTIVE_NAME 수집 활성화")
            .assertIsOn()
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Switch))
        composeRule.onNodeWithContentDescription("$FAILING_NAME 수집 활성화").assertIsOff()
        composeRule.onNodeWithText("마지막 수집 방금").assertIsDisplayed()
        composeRule.onNodeWithText("공고 12건").assertIsDisplayed()
    }

    @Test
    fun unknownPostingCount_hidesCountRow() {
        val loaded = loadedState().content as BoardListContentState.Loaded
        composeRule.setListContent(
            state = BoardListUiState(content = BoardListContentState.Loaded(loaded.boards.map { it.copy(postingCount = null) })),
        )

        composeRule.onAllNodesWithText("공고 12건").assertCountEquals(0)
        composeRule.onNodeWithText("마지막 수집 방금").assertIsDisplayed()
    }

    @Test
    fun retry_isOfferedOnlyForFailingBoards() {
        composeRule.setListContent(state = loadedState())

        composeRule.onAllNodesWithContentDescription("$ACTIVE_NAME 재시도").assertCountEquals(0)
        composeRule.onNodeWithContentDescription("$FAILING_NAME 재시도").performScrollTo().assertIsDisplayed()
    }

    @Test
    fun boardControls_emitSeparateIntentsWithBoardIds() {
        val events = mutableListOf<BoardListEvent>()
        composeRule.setListContent(state = loadedState(), onEvent = events::add)

        composeRule.onNodeWithContentDescription("$ACTIVE_NAME 수집 활성화").performClick()
        composeRule.onNodeWithContentDescription("$FAILING_NAME 재시도").performScrollTo().performClick()
        composeRule.onNodeWithContentDescription("$ACTIVE_NAME 삭제").performScrollTo().performClick()
        composeRule.onNodeWithText(ACTIVE_NAME).performScrollTo().performClick()
        composeRule.onNodeWithContentDescription("게시판 추가").performClick()
        composeRule.onNodeWithContentDescription("뒤로가기").performClick()

        composeRule.runOnIdle {
            assertEquals(
                listOf(
                    BoardListEvent.BoardToggled(ACTIVE_ID),
                    BoardListEvent.RetryClicked(FAILING_ID),
                    BoardListEvent.DeleteClicked(ACTIVE_ID),
                    BoardListEvent.BoardSelected(ACTIVE_ID),
                    BoardListEvent.AddBoardClicked,
                    BoardListEvent.BackClicked,
                ),
                events,
            )
        }
    }

    @Test
    fun empty_showsGuidanceAndEmitsAddBoard() {
        val events = mutableListOf<BoardListEvent>()
        composeRule.setListContent(
            state = BoardListUiState(content = BoardListContentState.Empty),
            onEvent = events::add,
        )

        composeRule.onNodeWithText("등록된 게시판이 없어요").assertIsDisplayed()
        composeRule.onNodeWithText("학교 공지·채용 사이트 URL 을 등록하면 자동으로 공고를 모아요").assertIsDisplayed()
        composeRule.onAllNodesWithText(ACTIVE_NAME).assertCountEquals(0)
        composeRule.onNode(hasText("게시판 등록하기") and hasClickAction()).performClick()

        composeRule.runOnIdle {
            assertEquals(listOf(BoardListEvent.AddBoardClicked), events)
        }
    }

    @Test
    fun loading_showsProgressCopyWithoutBoards() {
        composeRule.setListContent(state = BoardListUiState(content = BoardListContentState.Loading))

        composeRule.onNodeWithText("게시판을 불러오는 중이에요").assertIsDisplayed()
        composeRule.onAllNodesWithText(ACTIVE_NAME).assertCountEquals(0)
    }
}

private fun ComposeContentTestRule.setListContent(
    state: BoardListUiState,
    onEvent: (BoardListEvent) -> Unit = {},
) {
    setContent {
        CareerCompassTheme {
            BoardListScreen(state = state, onEvent = onEvent)
        }
    }
}

private fun loadedState(): BoardListUiState =
    BoardListUiState(
        content =
            BoardListContentState.Loaded(
                listOf(
                    BoardUiModel(
                        id = ACTIVE_ID,
                        name = ACTIVE_NAME,
                        url = "https://konkuk.ac.kr/board/notice",
                        type = BoardType.Employment,
                        typeLabel = "채용",
                        status = BoardStatus.Active,
                        isActive = true,
                        failCount = 0,
                        lastCollectedLabel = "방금",
                        postingCount = 12,
                    ),
                    BoardUiModel(
                        id = FAILING_ID,
                        name = FAILING_NAME,
                        url = "https://wettheidol.com/contest",
                        type = BoardType.Contest,
                        typeLabel = "공모전",
                        status = BoardStatus.Failing,
                        isActive = false,
                        failCount = 3,
                        lastCollectedLabel = "6시간 전",
                        postingCount = 4,
                    ),
                ),
            ),
    )

private const val ACTIVE_ID = "konkuk"
private const val ACTIVE_NAME = "건국대 공지사항"
private const val FAILING_ID = "wettheidol"
private const val FAILING_NAME = "공모전 사이트"
