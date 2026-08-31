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
    fun listing_rejectsBlankId() {
        assertThrows(IllegalArgumentException::class.java) {
            sampleListing(id = "  ")
        }
    }

    @Test
    fun loadedState_rejectsDuplicateListingIds() {
        assertThrows(IllegalArgumentException::class.java) {
            FeedContentState.Loaded(
                listOf(
                    sampleListing(id = "duplicate"),
                    sampleListing(id = "duplicate"),
                ),
            )
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

private fun sampleListing(id: String): FeedListingUiModel =
    FeedListingUiModel(
        id = id,
        title = "공고",
        category = FeedListingCategory.Employment,
        categoryLabel = "채용",
        sourceLabel = "공식 채용",
        suitabilityScore = 88,
        deadlineLabel = "D-7",
        isDeadlineUrgent = false,
        isNew = true,
        isBookmarked = false,
    )
