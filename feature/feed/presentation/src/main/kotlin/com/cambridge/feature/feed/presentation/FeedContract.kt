package com.cambridge.feature.feed.presentation

/** Stable identifiers for the listing categories rendered by the feed. */
public enum class FeedListingCategory {
    All,
    Employment,
    Scholarship,
    Contest,
    ExternalActivity,
}

/** A localized filter option displayed above the feed results. */
public data class FeedFilterUiModel(
    val category: FeedListingCategory,
    val label: String,
)

/** The currently selected, localized sort option. */
public data class FeedSortUiModel(
    val id: String,
    val label: String,
)

/** Display-only data for one listing card. */
public data class FeedListingUiModel(
    val id: String,
    val title: String,
    val category: FeedListingCategory,
    val categoryLabel: String,
    val sourceLabel: String,
    val suitabilityScore: Int,
    val deadlineLabel: String,
    val isDeadlineUrgent: Boolean,
    val isNew: Boolean,
    val isBookmarked: Boolean,
) {
    init {
        require(suitabilityScore in 0..100) {
            "suitabilityScore must be between 0 and 100: $suitabilityScore"
        }
    }
}

/** Mutually exclusive loading states for the listing portion of the feed. */
public sealed interface FeedContentState {
    public data object Loading : FeedContentState

    public data object Empty : FeedContentState

    public data class Loaded(
        val listings: List<FeedListingUiModel>,
    ) : FeedContentState {
        init {
            require(listings.isNotEmpty()) {
                "Use FeedContentState.Empty when there are no listings"
            }
        }
    }
}

/** Complete, display-ready state for [FeedScreen]. */
public data class FeedUiState(
    val userName: String,
    val newListingCount: Int,
    val searchQuery: String,
    val filters: List<FeedFilterUiModel>,
    val selectedFilter: FeedListingCategory,
    val selectedSort: FeedSortUiModel,
    val totalListingCount: Int,
    val content: FeedContentState,
) {
    init {
        require(newListingCount >= 0) { "newListingCount must not be negative" }
        require(totalListingCount >= 0) { "totalListingCount must not be negative" }
        require(filters.any { it.category == selectedFilter }) {
            "selectedFilter must be present in filters"
        }
    }
}

/** User intents emitted by the stateless feed UI. */
public sealed interface FeedUiEvent {
    public data class SearchQueryChanged(
        val query: String,
    ) : FeedUiEvent

    public data class FilterSelected(
        val category: FeedListingCategory,
    ) : FeedUiEvent

    public data object SortMenuRequested : FeedUiEvent

    public data class ListingSelected(
        val listingId: String,
    ) : FeedUiEvent

    public data class BookmarkToggled(
        val listingId: String,
    ) : FeedUiEvent

    public data object NotificationsSelected : FeedUiEvent
}
