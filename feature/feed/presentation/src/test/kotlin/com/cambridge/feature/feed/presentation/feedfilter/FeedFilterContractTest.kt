package com.cambridge.feature.feed.presentation.feedfilter

import com.cambridge.feature.feed.presentation.FeedFilterUiModel
import com.cambridge.feature.feed.presentation.FeedListingCategory
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

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
    matchingCount: Int? = 12,
): FeedFilterUiState =
    FeedFilterUiState(
        categories = categories,
        selectedCategory = selectedCategory,
        boards = boards,
        selectedBoardIds = selectedBoardIds,
        deadline = FeedDeadlineFilter.All,
        minScore = FeedMinScoreFilter.All,
        unreadOnly = false,
        matchingCount = matchingCount,
    )
