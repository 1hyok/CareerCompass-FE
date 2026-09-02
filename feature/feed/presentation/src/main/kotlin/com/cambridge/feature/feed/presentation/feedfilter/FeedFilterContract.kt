package com.cambridge.feature.feed.presentation.feedfilter

import com.cambridge.feature.feed.presentation.FeedFilterUiModel
import com.cambridge.feature.feed.presentation.FeedListingCategory

/** Deadline window options of the feed filter sheet (spec F2-3). */
public enum class FeedDeadlineFilter {
    All,
    WithinWeek,
    WithinMonth,
    IncludeExpired,
}

/** Minimum suitability score options of the feed filter sheet (spec F2-3). */
public enum class FeedMinScoreFilter {
    All,
    AtLeast60,
    AtLeast70,
    AtLeast80,
}

/** A registered board offered as a multi-select source filter. */
public data class FeedBoardFilterUiModel(
    val id: String,
    val name: String,
) {
    init {
        require(id.isNotBlank()) { "id must not be blank" }
        require(name.isNotBlank()) { "name must not be blank" }
    }
}

/** Complete, display-ready state for [FeedFilterSheetContent]. */
public data class FeedFilterUiState(
    val categories: List<FeedFilterUiModel>,
    val selectedCategory: FeedListingCategory,
    val boards: List<FeedBoardFilterUiModel>,
    val selectedBoardIds: Set<String>,
    val deadline: FeedDeadlineFilter,
    val minScore: FeedMinScoreFilter,
    val unreadOnly: Boolean,
    val matchingCount: Int?,
) {
    init {
        require(categories.map(FeedFilterUiModel::category).distinct().size == categories.size) {
            "filter categories must be unique"
        }
        require(categories.any { it.category == selectedCategory }) {
            "selectedCategory must be present in categories"
        }
        require(boards.map(FeedBoardFilterUiModel::id).distinct().size == boards.size) {
            "board ids must be unique"
        }
        require(selectedBoardIds.all { id -> boards.any { it.id == id } }) {
            "selectedBoardIds must be present in boards"
        }
        require(matchingCount == null || matchingCount >= 0) {
            "matchingCount must be null or non-negative"
        }
    }
}

/** User intents emitted by the stateless filter sheet content. */
public sealed interface FeedFilterEvent {
    public data class CategorySelected(
        val category: FeedListingCategory,
    ) : FeedFilterEvent

    public data class BoardToggled(
        val boardId: String,
    ) : FeedFilterEvent

    public data class DeadlineSelected(
        val deadline: FeedDeadlineFilter,
    ) : FeedFilterEvent

    public data class MinScoreSelected(
        val minScore: FeedMinScoreFilter,
    ) : FeedFilterEvent

    public data object UnreadOnlyToggled : FeedFilterEvent

    public data object ResetClicked : FeedFilterEvent

    public data object ApplyClicked : FeedFilterEvent

    public data object DismissClicked : FeedFilterEvent
}

/** Sort orders of the feed list; labels are string resources resolved by the UI. */
public enum class FeedSortOption {
    CollectedDesc,
    DueAsc,
    ScoreDesc,
}

/** Display-ready state for [FeedSortMenuContent]. */
public data class FeedSortMenuUiState(
    val selected: FeedSortOption,
)

/** User intents emitted by the stateless sort menu content. */
public sealed interface FeedSortMenuEvent {
    public data class SortSelected(
        val option: FeedSortOption,
    ) : FeedSortMenuEvent

    public data object DismissClicked : FeedSortMenuEvent
}
