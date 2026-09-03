package com.cambridge.feature.feed.presentation

/** Stable identifiers for the listing categories rendered by the feed (spec F2-3 「공고 유형」). */
public enum class FeedListingCategory {
    All,
    Employment,
    Scholarship,
    Contest,
    ExternalActivity,
    Other,
}

/** A localized filter option displayed above the feed results. */
public data class FeedFilterUiModel(
    val category: FeedListingCategory,
    val label: String,
) {
    init {
        requireNonBlank("filter label", label)
    }
}

/** The currently selected, localized sort option. */
public data class FeedSortUiModel(
    val id: String,
    val label: String,
) {
    init {
        requireNonBlank("sort id", id)
        requireNonBlank("sort label", label)
    }
}

/**
 * 카드의 점수 자리에 무엇을 보일지 (기능 스펙 F2-3 「적합도 점수 표시 조건」·F3-1 「처리 시점」).
 *
 * 점수가 없는 이유를 둘로 가른다 — 프로필이 비어 산출을 못 하는 것과 아직 분석이 끝나지 않은 것은
 * 사용자가 할 일이 다르다. 하나로 뭉치면 프로필을 안 채운 사용자에게 목록이 영원히 「분석 중」이다.
 */
public sealed interface FeedSuitabilityState {
    /** 서버가 준 점수. */
    public data class Scored(
        val score: Int,
    ) : FeedSuitabilityState {
        init {
            require(score in 0..100) { "score must be between 0 and 100: $score" }
        }
    }

    /** 아직 파싱·분석이 끝나지 않았다. */
    public data object Analyzing : FeedSuitabilityState

    /** 프로필(희망 직무·관심 태그)이 비어 산출 자체가 불가능하다. */
    public data object ProfileIncomplete : FeedSuitabilityState
}

/** Display-only data for one listing card. */
public data class FeedListingUiModel(
    val id: String,
    val title: String,
    val category: FeedListingCategory,
    val categoryLabel: String,
    val sourceLabel: String,
    val suitability: FeedSuitabilityState,
    val deadlineLabel: String,
    val isDeadlineUrgent: Boolean,
    val isNew: Boolean,
    val isBookmarked: Boolean,
) {
    init {
        requireNonBlank("id", id)
        requireNonBlank("title", title)
        requireNonBlank("categoryLabel", categoryLabel)
        requireNonBlank("sourceLabel", sourceLabel)
        requireNonBlank("deadlineLabel", deadlineLabel)
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
            require(listings.map(FeedListingUiModel::id).distinct().size == listings.size) {
                "listing ids must be unique"
            }
        }
    }
}

/**
 * Complete, display-ready state for [FeedScreen].
 *
 * [activeFilterCount] is the number of filter-sheet conditions (board, deadline, score, unread) that
 * differ from their defaults; the category chip row is not counted because it is visible on its own.
 *
 * [offlineNotice] is the localized banner shown while the feed displays a stored offline snapshot
 * ("오프라인 · 9월 3일 14:20 기준 목록"); `null` hides the banner.
 *
 * [isProfileNoticeVisible] 는 프로필이 비어 점수를 못 내는 항목이 목록에 있을 때만 참이다 — 목록 위에
 * 안내를 한 번 얹고, 카드마다 같은 말을 되풀이하지 않는다.
 */
public data class FeedUiState(
    val userName: String,
    val newListingCount: Int,
    val searchQuery: String,
    val filters: List<FeedFilterUiModel>,
    val selectedFilter: FeedListingCategory,
    val selectedSort: FeedSortUiModel,
    val totalListingCount: Int,
    val content: FeedContentState,
    val activeFilterCount: Int = 0,
    val offlineNotice: String? = null,
    val isProfileNoticeVisible: Boolean = false,
) {
    init {
        requireNonBlank("userName", userName)
        require(newListingCount >= 0) { "newListingCount must not be negative" }
        require(totalListingCount >= 0) { "totalListingCount must not be negative" }
        require(activeFilterCount >= 0) { "activeFilterCount must not be negative" }
        require(offlineNotice == null || offlineNotice.isNotBlank()) { "offlineNotice must be null or non-blank" }
        require(filters.map(FeedFilterUiModel::category).distinct().size == filters.size) {
            "filter categories must be unique"
        }
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

    /** The filter button in the header was pressed; the host opens the filter sheet. */
    public data object FilterRequested : FeedUiEvent

    public data object SortMenuRequested : FeedUiEvent

    public data class ListingSelected(
        val listingId: String,
    ) : FeedUiEvent

    public data class BookmarkToggled(
        val listingId: String,
    ) : FeedUiEvent

    public data object NotificationsSelected : FeedUiEvent

    /** 프로필 입력 안내를 눌렀다 — 앱 셸이 마이 탭으로 보낸다. */
    public data object CompleteProfileSelected : FeedUiEvent
}

private fun requireNonBlank(
    fieldName: String,
    value: String,
) {
    require(value.isNotBlank()) {
        "$fieldName must not be blank"
    }
}
