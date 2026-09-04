package com.careercompass.feature.feed.presentation.feedfilter

import com.careercompass.feature.feed.presentation.FeedFilterUiModel
import com.careercompass.feature.feed.presentation.FeedListingCategory
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class FeedFilterContractTest {
    @Test
    fun uiState_rejectsSelectedCategoryMissingFromOptions() {
        assertThrows(IllegalArgumentException::class.java) {
            sampleState(selectedCategory = FeedListingCategory.Contest)
        }
    }

    @Test
    fun uiState_rejectsDuplicateCategories() {
        assertThrows(IllegalArgumentException::class.java) {
            sampleState(
                categories =
                    listOf(
                        FeedFilterUiModel(FeedListingCategory.All, "전체"),
                        FeedFilterUiModel(FeedListingCategory.All, "모두"),
                    ),
            )
        }
    }

    @Test
    fun uiState_rejectsSelectedBoardsMissingFromBoards() {
        assertThrows(IllegalArgumentException::class.java) {
            sampleState(selectedBoardIds = setOf("unknown"))
        }
    }

    @Test
    fun uiState_rejectsDuplicateBoardIds() {
        assertThrows(IllegalArgumentException::class.java) {
            sampleState(
                boards =
                    listOf(
                        FeedBoardFilterUiModel(id = "school", name = "학교 게시판"),
                        FeedBoardFilterUiModel(id = "school", name = "학교 공지"),
                    ),
            )
        }
    }

    @Test
    fun uiState_rejectsNegativeMatchingCountButAllowsNull() {
        assertThrows(IllegalArgumentException::class.java) {
            sampleState(matchingCount = -1)
        }
        assertEquals(null, sampleState(matchingCount = null).matchingCount)
        assertEquals(0, sampleState(matchingCount = 0).matchingCount)
    }

    @Test
    fun boardFilter_rejectsBlankStrings() {
        assertThrows(IllegalArgumentException::class.java) {
            FeedBoardFilterUiModel(id = " ", name = "학교 게시판")
        }
        assertThrows(IllegalArgumentException::class.java) {
            FeedBoardFilterUiModel(id = "school", name = " ")
        }
    }

    @Test
    fun uiState_rejectsRangeWithoutDatesOrDatesWithoutRange() {
        assertThrows(IllegalArgumentException::class.java) {
            sampleState(deadline = FeedDeadlineFilter.Range, deadlineRange = null)
        }
        assertThrows(IllegalArgumentException::class.java) {
            sampleState(deadline = FeedDeadlineFilter.WithinWeek, deadlineRange = FeedDeadlineRange())
        }
    }

    @Test
    fun deadlineRange_reportsWhyItCannotBeApplied() {
        assertEquals(FeedDeadlineRangeError.Empty, FeedDeadlineRange().error)
        assertEquals(
            FeedDeadlineRangeError.StartAfterEnd,
            FeedDeadlineRange(start = NOVEMBER_LAST, end = NOVEMBER_FIRST).error,
        )
        assertNull(FeedDeadlineRange(start = NOVEMBER_FIRST, end = NOVEMBER_LAST).error)
        assertNull(FeedDeadlineRange(start = NOVEMBER_FIRST).error)
        assertNull(FeedDeadlineRange(end = NOVEMBER_LAST).error)
    }

    @Test
    fun uiState_disablesApplyOnlyForInvalidRange() {
        assertTrue(sampleState().isApplyEnabled)
        assertTrue(
            sampleState(
                deadline = FeedDeadlineFilter.Range,
                deadlineRange = FeedDeadlineRange(start = NOVEMBER_FIRST),
            ).isApplyEnabled,
        )
        assertFalse(
            sampleState(deadline = FeedDeadlineFilter.Range, deadlineRange = FeedDeadlineRange()).isApplyEnabled,
        )
        assertFalse(
            sampleState(
                deadline = FeedDeadlineFilter.Range,
                deadlineRange = FeedDeadlineRange(start = NOVEMBER_LAST, end = NOVEMBER_FIRST),
            ).isApplyEnabled,
        )
    }

    @Test
    fun deadlineRange_editsOneEndpointAndClosesThePicker() {
        val range = FeedDeadlineRange(editing = FeedDeadlineRangeEndpoint.Start)

        val withStart = range.withDate(FeedDeadlineRangeEndpoint.Start, NOVEMBER_FIRST)

        assertEquals(NOVEMBER_FIRST, withStart.start)
        assertEquals(NOVEMBER_FIRST, withStart.dateOf(FeedDeadlineRangeEndpoint.Start))
        assertNull(withStart.end)
        assertNull(withStart.editing)

        val withEnd = withStart.withDate(FeedDeadlineRangeEndpoint.End, NOVEMBER_LAST)

        assertEquals(NOVEMBER_FIRST, withEnd.start)
        assertEquals(NOVEMBER_LAST, withEnd.dateOf(FeedDeadlineRangeEndpoint.End))
    }

    @Test
    fun missingBoards_rejectNonPositiveCounts() {
        assertThrows(IllegalArgumentException::class.java) {
            FeedMissingBoardsUiModel(count = 0, reason = FeedMissingBoardsReason.Deleted)
        }
        assertThrows(IllegalArgumentException::class.java) {
            FeedMissingBoardsUiModel(count = -1, reason = FeedMissingBoardsReason.Deleted)
        }
    }

    /**
     * 배지와 시트가 어긋나던 자리(이슈 #155) — 목록에서 사라진 게시판만 걸려 있어도 시트는 조건 하나를
     * 켜 놓고 있다고 세야 한다. 세기만 하고 그리지 않으면 끌 수 없는 조건이 된다.
     */
    @Test
    fun activeConditionCount_countsMissingBoardsAsTheBoardCondition() {
        assertEquals(0, sampleState().activeConditionCount)
        assertEquals(1, sampleState(selectedBoardIds = setOf("school")).activeConditionCount)
        assertEquals(
            1,
            sampleState(missingBoards = FeedMissingBoardsUiModel(count = 2, reason = FeedMissingBoardsReason.Deleted))
                .activeConditionCount,
        )
        // 고른 게시판과 사라진 게시판이 함께 있어도 게시판은 조건 하나다 — 배지가 세는 규칙과 같다.
        assertEquals(
            1,
            sampleState(
                selectedBoardIds = setOf("school"),
                missingBoards = FeedMissingBoardsUiModel(count = 1, reason = FeedMissingBoardsReason.Unverified),
            ).activeConditionCount,
        )
        assertEquals(
            4,
            sampleState(
                selectedBoardIds = setOf("school"),
                deadline = FeedDeadlineFilter.WithinWeek,
                minScore = FeedMinScoreFilter.AtLeast80,
                unreadOnly = true,
            ).activeConditionCount,
        )
    }

    @Test
    fun uiState_acceptsSelectionsPresentInOptions() {
        val state = sampleState(selectedBoardIds = setOf("school"))

        assertEquals(FeedListingCategory.Employment, state.selectedCategory)
        assertEquals(setOf("school"), state.selectedBoardIds)
        assertEquals(FeedDeadlineFilter.All, state.deadline)
        assertEquals(FeedMinScoreFilter.All, state.minScore)
    }
}

private fun sampleState(
    categories: List<FeedFilterUiModel> =
        listOf(
            FeedFilterUiModel(FeedListingCategory.All, "전체"),
            FeedFilterUiModel(FeedListingCategory.Employment, "채용"),
        ),
    selectedCategory: FeedListingCategory = FeedListingCategory.Employment,
    boards: List<FeedBoardFilterUiModel> =
        listOf(FeedBoardFilterUiModel(id = "school", name = "학교 게시판")),
    selectedBoardIds: Set<String> = emptySet(),
    missingBoards: FeedMissingBoardsUiModel? = null,
    deadline: FeedDeadlineFilter = FeedDeadlineFilter.All,
    deadlineRange: FeedDeadlineRange? = null,
    minScore: FeedMinScoreFilter = FeedMinScoreFilter.All,
    unreadOnly: Boolean = false,
    matchingCount: Int? = 12,
): FeedFilterUiState =
    FeedFilterUiState(
        categories = categories,
        selectedCategory = selectedCategory,
        boards = boards,
        selectedBoardIds = selectedBoardIds,
        missingBoards = missingBoards,
        deadline = deadline,
        deadlineRange = deadlineRange,
        minScore = minScore,
        unreadOnly = unreadOnly,
        matchingCount = matchingCount,
    )

private val NOVEMBER_FIRST: LocalDate = LocalDate.of(2026, 11, 1)
private val NOVEMBER_LAST: LocalDate = LocalDate.of(2026, 11, 30)
