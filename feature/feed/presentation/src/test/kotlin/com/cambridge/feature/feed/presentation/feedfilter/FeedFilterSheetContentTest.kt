package com.cambridge.feature.feed.presentation.feedfilter

import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasStateDescription
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
import com.cambridge.feature.feed.presentation.FeedFilterUiModel
import com.cambridge.feature.feed.presentation.FeedListingCategory
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.LocalDate

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class FeedFilterSheetContentTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun categoryTags_formRadioGroupWithSelectedState() {
        composeRule.setFilterContent(state = sampleState())

        composeRule
            .onAllNodes(SemanticsMatcher.keyIsDefined(SemanticsProperties.SelectableGroup))
            .assertCountEquals(3)
        composeRule
            .onNode(hasText("채용") and hasStateDescription("선택됨"))
            .assertIsOn()
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.RadioButton))
        composeRule
            .onNode(hasText("장학금") and hasStateDescription("선택 안 됨"))
            .assertIsOff()
    }

    @Test
    fun boardTags_areCheckboxesReflectingSelection() {
        composeRule.setFilterContent(state = sampleState(selectedBoardIds = setOf("school")))

        composeRule
            .onNodeWithText("학교 게시판")
            .assertIsOn()
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Checkbox))
        composeRule.onNodeWithText("네이버 채용").assertIsOff()
    }

    @Test
    fun emptyBoards_showEmptyCaption() {
        composeRule.setFilterContent(state = sampleState(boards = emptyList()))

        composeRule.onNodeWithText("등록된 게시판이 없어요").assertIsDisplayed()
    }

    /** 이슈 #155 — 목록에 없는 게시판도 켜진 태그로 보여야 끌 수 있다. */
    @Test
    fun missingBoards_areShownAsAnOnTagThatCanBeCleared() {
        val events = mutableListOf<FeedFilterEvent>()
        composeRule.setFilterContent(
            state =
                sampleState(
                    selectedBoardIds = setOf("school"),
                    missingBoards = FeedMissingBoardsUiModel(count = 2, reason = FeedMissingBoardsReason.Deleted),
                ),
            onEvent = events::add,
        )

        composeRule
            .onNodeWithText("사라진 게시판 2개")
            .assertIsOn()
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Checkbox))
        composeRule.onNodeWithText("지워진 게시판이라 목록에 없어요. 눌러서 조건에서 뺄 수 있어요.").assertIsDisplayed()
        composeRule.onNodeWithText("사라진 게시판 2개").performClick()

        composeRule.runOnIdle { assertEquals(listOf(FeedFilterEvent.MissingBoardsCleared), events) }
    }

    /** 목록 조회가 실패한 것과 정말 지워진 것은 다른 말을 한다 — 없는 사실을 알리지 않는다. */
    @Test
    fun unverifiedBoards_sayTheListWasNotLoadedInsteadOfDeleted() {
        composeRule.setFilterContent(
            state = sampleState(missingBoards = FeedMissingBoardsUiModel(count = 1, reason = FeedMissingBoardsReason.Unverified)),
        )

        composeRule.onNodeWithText("확인 못 한 게시판 1개").assertIsOn()
        composeRule.onNodeWithText("게시판 목록을 못 받아 조건을 그대로 뒀어요. 눌러서 뺄 수 있어요.").assertIsDisplayed()
    }

    /**
     * 목록이 비어 있어도 「등록된 게시판이 없어요」로 덮지 않는다 — 그 한 줄이 태그를 가리면 조건을 끄는
     * 손잡이가 사라져, 게시판 조회가 실패한 사용자가 필터를 영영 못 푼다.
     */
    @Test
    fun missingBoards_surviveAnEmptyBoardList() {
        composeRule.setFilterContent(
            state =
                sampleState(
                    boards = emptyList(),
                    missingBoards = FeedMissingBoardsUiModel(count = 1, reason = FeedMissingBoardsReason.Unverified),
                ),
        )

        composeRule.onAllNodesWithText("등록된 게시판이 없어요").assertCountEquals(0)
        composeRule.onNodeWithText("확인 못 한 게시판 1개").assertIsOn()
    }

    @Test
    fun unreadSwitch_isNamedByLabelAndReflectsState() {
        composeRule.setFilterContent(state = sampleState(unreadOnly = true))

        composeRule
            .onNodeWithContentDescription("읽지 않은 공고만 보기")
            .performScrollTo()
            .assertIsOn()
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Switch))
    }

    @Test
    fun deadlineRangeEditor_isHiddenWhileAPresetIsSelected() {
        composeRule.setFilterContent(state = sampleState())

        composeRule.onNodeWithText("직접 지정").performScrollTo().assertIsOff()
        composeRule.onAllNodesWithContentDescription("시작일, 선택 안 함").assertCountEquals(0)
    }

    @Test
    fun deadlineRangeEditor_isDrawnForTheRangeOption() {
        composeRule.setFilterContent(state = rangeState(FeedDeadlineRange()))

        composeRule.onNode(hasText("직접 지정") and hasStateDescription("선택됨")).performScrollTo().assertIsOn()
        composeRule.onNodeWithContentDescription("시작일, 선택 안 함").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithContentDescription("종료일, 선택 안 함").assertIsDisplayed()
    }

    @Test
    fun deadlineRangeFields_showPickedDatesAndAskForTheEndpointTheyOwn() {
        val events = mutableListOf<FeedFilterEvent>()
        composeRule.setFilterContent(
            state = rangeState(FeedDeadlineRange(start = NOVEMBER_FIRST, end = NOVEMBER_LAST)),
            onEvent = events::add,
        )

        composeRule.onNodeWithText("2026.11.01").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithContentDescription("종료일, 2026.11.30").performScrollTo().performClick()
        composeRule.onNodeWithContentDescription("시작일, 2026.11.01").performClick()

        composeRule.runOnIdle {
            assertEquals(
                listOf(
                    FeedFilterEvent.DeadlineRangeEndpointClicked(FeedDeadlineRangeEndpoint.End),
                    FeedFilterEvent.DeadlineRangeEndpointClicked(FeedDeadlineRangeEndpoint.Start),
                ),
                events,
            )
        }
    }

    @Test
    fun emptyRange_blocksApplyWithAReason() {
        val events = mutableListOf<FeedFilterEvent>()
        composeRule.setFilterContent(state = rangeState(FeedDeadlineRange()), onEvent = events::add)

        composeRule.onNodeWithText("시작일이나 종료일 중 하나는 골라야 해요").performScrollTo().assertIsDisplayed()
        composeRule.onNode(hasText("12개 공고 보기") and hasClickAction()).assertIsNotEnabled().performClick()

        composeRule.runOnIdle { assertEquals(emptyList<FeedFilterEvent>(), events) }
    }

    @Test
    fun invertedRange_blocksApplyWithAReason() {
        val events = mutableListOf<FeedFilterEvent>()
        composeRule.setFilterContent(
            state = rangeState(FeedDeadlineRange(start = NOVEMBER_LAST, end = NOVEMBER_FIRST)),
            onEvent = events::add,
        )

        composeRule.onNodeWithText("시작일이 종료일보다 늦어요").performScrollTo().assertIsDisplayed()
        composeRule.onNode(hasText("12개 공고 보기") and hasClickAction()).assertIsNotEnabled().performClick()

        composeRule.runOnIdle { assertEquals(emptyList<FeedFilterEvent>(), events) }
    }

    @Test
    fun validRange_leavesApplyEnabled() {
        val events = mutableListOf<FeedFilterEvent>()
        composeRule.setFilterContent(
            state = rangeState(FeedDeadlineRange(start = NOVEMBER_FIRST, end = NOVEMBER_LAST)),
            onEvent = events::add,
        )

        composeRule.onNode(hasText("12개 공고 보기") and hasClickAction()).performClick()

        composeRule.runOnIdle { assertEquals(listOf(FeedFilterEvent.ApplyClicked), events) }
    }

    @Test
    fun datePicker_opensForTheEditedEndpointAndCanBeCancelled() {
        val events = mutableListOf<FeedFilterEvent>()
        composeRule.setFilterContent(
            state = rangeState(FeedDeadlineRange(editing = FeedDeadlineRangeEndpoint.Start)),
            onEvent = events::add,
        )

        composeRule.onNodeWithText("취소").assertIsDisplayed()
        composeRule.onNodeWithText("취소").performClick()

        composeRule.runOnIdle {
            assertEquals(listOf(FeedFilterEvent.DeadlineRangePickerDismissed), events)
        }
    }

    @Test
    fun applyButton_showsMatchingCountOrFallback() {
        composeRule.setFilterContent(state = sampleState(matchingCount = 12))
        composeRule.onNode(hasText("12개 공고 보기") and hasClickAction()).assertIsDisplayed()
    }

    @Test
    fun applyButton_fallsBackToApplyWhenCountUnknown() {
        composeRule.setFilterContent(state = sampleState(matchingCount = null))
        composeRule.onNode(hasText("적용") and hasClickAction()).assertIsDisplayed()
    }

    @Test
    fun controls_emitSeparateIntents() {
        val events = mutableListOf<FeedFilterEvent>()
        composeRule.setFilterContent(state = sampleState(), onEvent = events::add)

        composeRule.onNode(hasText("장학금") and hasStateDescription("선택 안 됨")).performClick()
        composeRule.onNodeWithText("학교 게시판").performClick()
        composeRule.onNode(hasText("7일 이내") and hasStateDescription("선택 안 됨")).performScrollTo().performClick()
        composeRule.onNode(hasText("70점 이상") and hasStateDescription("선택 안 됨")).performScrollTo().performClick()
        composeRule.onNodeWithContentDescription("읽지 않은 공고만 보기").performScrollTo().performClick()
        composeRule.onNode(hasText("초기화") and hasClickAction()).performClick()
        composeRule.onNode(hasText("12개 공고 보기") and hasClickAction()).performClick()
        composeRule.onNodeWithContentDescription("닫기").performClick()

        composeRule.runOnIdle {
            assertEquals(
                listOf(
                    FeedFilterEvent.CategorySelected(FeedListingCategory.Scholarship),
                    FeedFilterEvent.BoardToggled("school"),
                    FeedFilterEvent.DeadlineSelected(FeedDeadlineFilter.WithinWeek),
                    FeedFilterEvent.MinScoreSelected(FeedMinScoreFilter.AtLeast70),
                    FeedFilterEvent.UnreadOnlyToggled,
                    FeedFilterEvent.ResetClicked,
                    FeedFilterEvent.ApplyClicked,
                    FeedFilterEvent.DismissClicked,
                ),
                events,
            )
        }
    }
}

private fun ComposeContentTestRule.setFilterContent(
    state: FeedFilterUiState,
    onEvent: (FeedFilterEvent) -> Unit = {},
) {
    setContent {
        CareerCompassTheme {
            FeedFilterSheetContent(state = state, onEvent = onEvent)
        }
    }
}

private fun sampleState(
    boards: List<FeedBoardFilterUiModel> =
        listOf(
            FeedBoardFilterUiModel(id = "school", name = "학교 게시판"),
            FeedBoardFilterUiModel(id = "naver", name = "네이버 채용"),
        ),
    selectedBoardIds: Set<String> = emptySet(),
    missingBoards: FeedMissingBoardsUiModel? = null,
    unreadOnly: Boolean = false,
    matchingCount: Int? = 12,
    deadline: FeedDeadlineFilter = FeedDeadlineFilter.All,
    deadlineRange: FeedDeadlineRange? = null,
): FeedFilterUiState =
    FeedFilterUiState(
        categories =
            listOf(
                FeedFilterUiModel(FeedListingCategory.All, "전체"),
                FeedFilterUiModel(FeedListingCategory.Employment, "채용"),
                FeedFilterUiModel(FeedListingCategory.Scholarship, "장학금"),
            ),
        selectedCategory = FeedListingCategory.Employment,
        boards = boards,
        selectedBoardIds = selectedBoardIds,
        missingBoards = missingBoards,
        deadline = deadline,
        deadlineRange = deadlineRange,
        minScore = FeedMinScoreFilter.All,
        unreadOnly = unreadOnly,
        matchingCount = matchingCount,
    )

private fun rangeState(range: FeedDeadlineRange): FeedFilterUiState =
    sampleState(deadline = FeedDeadlineFilter.Range, deadlineRange = range)

private val NOVEMBER_FIRST: LocalDate = LocalDate.of(2026, 11, 1)
private val NOVEMBER_LAST: LocalDate = LocalDate.of(2026, 11, 30)
