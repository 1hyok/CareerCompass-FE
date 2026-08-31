package com.cambridge.feature.feed.presentation

import org.junit.Assert.assertThrows
import org.junit.Test

class FeedContractTest {
    @Test
    fun loadedState_rejectsEmptyListingCollection() {
        assertThrows(IllegalArgumentException::class.java) {
            FeedContentState.Loaded(emptyList())
        }
    }

    @Test
    fun uiState_rejectsSelectedFilterMissingFromOptions() {
        assertThrows(IllegalArgumentException::class.java) {
            FeedUiState(
                userName = "일혁",
                newListingCount = 0,
                searchQuery = "",
                filters =
                    listOf(
                        FeedFilterUiModel(
                            category = FeedListingCategory.Employment,
                            label = "채용",
                        ),
                    ),
                selectedFilter = FeedListingCategory.Scholarship,
                selectedSort = FeedSortUiModel(id = "fit", label = "적합도 높은순"),
                totalListingCount = 0,
                content = FeedContentState.Empty,
            )
        }
    }
}
