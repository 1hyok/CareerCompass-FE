package com.cambridge.feature.feed.presentation

import org.junit.Assert.assertEquals
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
    fun displayReadyModels_rejectBlankRequiredStrings() {
        val invalidFactories: List<Pair<String, () -> Any>> =
            listOf(
                "filter label must not be blank" to {
                    FeedFilterUiModel(FeedListingCategory.All, " \t")
                },
                "sort id must not be blank" to {
                    FeedSortUiModel(id = " \t", label = "적합도 높은순")
                },
                "sort label must not be blank" to {
                    FeedSortUiModel(id = "fit", label = " \t")
                },
                "id must not be blank" to {
                    sampleListing(id = " \t")
                },
                "title must not be blank" to {
                    sampleListing(title = " \t")
                },
                "categoryLabel must not be blank" to {
                    sampleListing(categoryLabel = " \t")
                },
                "sourceLabel must not be blank" to {
                    sampleListing(sourceLabel = " \t")
                },
                "deadlineLabel must not be blank" to {
                    sampleListing(deadlineLabel = " \t")
                },
                "userName must not be blank" to {
                    sampleUiState(userName = " \t")
                },
            )

        invalidFactories.forEach { (expectedMessage, factory) ->
            val exception =
                assertThrows(IllegalArgumentException::class.java) {
                    factory()
                }

            assertEquals(expectedMessage, exception.message)
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
            sampleUiState(
                filters =
                    listOf(
                        FeedFilterUiModel(
                            category = FeedListingCategory.Employment,
                            label = "채용",
                        ),
                    ),
                selectedFilter = FeedListingCategory.Scholarship,
            )
        }
    }

    @Test
    fun uiState_rejectsDuplicateFilterCategories() {
        assertThrows(IllegalArgumentException::class.java) {
            sampleUiState(
                filters =
                    listOf(
                        FeedFilterUiModel(FeedListingCategory.All, "전체"),
                        FeedFilterUiModel(FeedListingCategory.All, "모두"),
                    ),
            )
        }
    }

    @Test
    fun uiState_acceptsUniqueFiltersContainingSelection() {
        val state = sampleUiState()

        assertEquals("", state.searchQuery)
        assertEquals(
            listOf(FeedListingCategory.All, FeedListingCategory.Employment),
            state.filters.map(FeedFilterUiModel::category),
        )
        assertEquals(FeedListingCategory.All, state.selectedFilter)
    }
}

private fun sampleUiState(
    userName: String = "일혁",
    filters: List<FeedFilterUiModel> =
        listOf(
            FeedFilterUiModel(FeedListingCategory.All, "전체"),
            FeedFilterUiModel(FeedListingCategory.Employment, "채용"),
        ),
    selectedFilter: FeedListingCategory = FeedListingCategory.All,
): FeedUiState =
    FeedUiState(
        userName = userName,
        newListingCount = 0,
        searchQuery = "",
        filters = filters,
        selectedFilter = selectedFilter,
        selectedSort = FeedSortUiModel(id = "fit", label = "적합도 높은순"),
        totalListingCount = 0,
        content = FeedContentState.Empty,
    )

private fun sampleListing(
    id: String = "listing-1",
    title: String = "공고",
    categoryLabel: String = "채용",
    sourceLabel: String = "공식 채용",
    deadlineLabel: String = "D-7",
): FeedListingUiModel =
    FeedListingUiModel(
        id = id,
        title = title,
        category = FeedListingCategory.Employment,
        categoryLabel = categoryLabel,
        sourceLabel = sourceLabel,
        suitabilityScore = 88,
        deadlineLabel = deadlineLabel,
        isDeadlineUrgent = false,
        isNew = true,
        isBookmarked = false,
    )
