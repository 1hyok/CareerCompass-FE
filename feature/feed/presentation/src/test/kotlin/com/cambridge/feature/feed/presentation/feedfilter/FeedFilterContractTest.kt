package com.cambridge.feature.feed.presentation.feedfilter

import com.cambridge.feature.feed.presentation.FeedFilterUiModel
import com.cambridge.feature.feed.presentation.FeedListingCategory
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
    deadline: FeedDeadlineFilter = FeedDeadlineFilter.All,
    deadlineRange: FeedDeadlineRange? = null,
    matchingCount: Int? = 12,
): FeedFilterUiState =
    FeedFilterUiState(
        categories = categories,
        selectedCategory = selectedCategory,
        boards = boards,
        selectedBoardIds = selectedBoardIds,
        deadline = deadline,
        deadlineRange = deadlineRange,
        minScore = FeedMinScoreFilter.All,
        unreadOnly = false,
        matchingCount = matchingCount,
    )

private val NOVEMBER_FIRST: LocalDate = LocalDate.of(2026, 11, 1)
private val NOVEMBER_LAST: LocalDate = LocalDate.of(2026, 11, 30)
