package com.cambridge.feature.feed.presentation.feedfilter

import com.cambridge.feature.feed.presentation.FeedFilterUiModel
import com.cambridge.feature.feed.presentation.FeedListingCategory
import java.time.LocalDate

/** Deadline window options of the feed filter sheet (spec F2-3). */
public enum class FeedDeadlineFilter {
    All,
    WithinWeek,
    WithinMonth,
    IncludeExpired,

    /** 「직접 지정」 — 고른 범위는 [FeedFilterUiState.deadlineRange] 에 있다. */
    Range,
}

/** 「직접 지정」의 두 끝 — 날짜 선택기가 어느 쪽을 고치는지. */
public enum class FeedDeadlineRangeEndpoint {
    Start,
    End,
}

/** 「적용」을 막는 범위 오류. 문구는 시트가 리소스로 옮긴다. */
public enum class FeedDeadlineRangeError {
    /** 시작일·종료일을 하나도 고르지 않았다 — 거를 것이 없어 프리셋 「전체」와 구분되지 않는다. */
    Empty,

    /** 시작일이 종료일보다 늦다. */
    StartAfterEnd,
}

/**
 * 「직접 지정」으로 고르는 마감일 범위 — 양 끝을 포함한다.
 *
 * 한쪽만 골라도 조회할 수 있어([FeedDeadlineFilter.Range] 참고) 두 끝이 각각 null 일 수 있다. 도메인
 * 값과 달리 **잘못된 범위도 담는다** — 「적용」을 막고 이유를 보여 주려면 뒤집힌 상태를 들고 있어야 한다.
 *
 * @property editing 열려 있는 날짜 선택기의 대상. null 이면 선택기가 닫혀 있다.
 */
public data class FeedDeadlineRange(
    val start: LocalDate? = null,
    val end: LocalDate? = null,
    val editing: FeedDeadlineRangeEndpoint? = null,
) {
    /** 지금 조회에 쓸 수 없는 이유. null 이면 적용할 수 있다. */
    public val error: FeedDeadlineRangeError?
        get() =
            when {
                start == null && end == null -> FeedDeadlineRangeError.Empty
                start != null && end != null && end.isBefore(start) -> FeedDeadlineRangeError.StartAfterEnd
                else -> null
            }

    /** [endpoint] 쪽에 고른 날짜. 날짜 선택기의 초기값이다. */
    public fun dateOf(endpoint: FeedDeadlineRangeEndpoint): LocalDate? =
        when (endpoint) {
            FeedDeadlineRangeEndpoint.Start -> start
            FeedDeadlineRangeEndpoint.End -> end
        }

    /** [endpoint] 쪽을 [date] 로 바꾸고 선택기를 닫는다. */
    public fun withDate(
        endpoint: FeedDeadlineRangeEndpoint,
        date: LocalDate,
    ): FeedDeadlineRange =
        when (endpoint) {
            FeedDeadlineRangeEndpoint.Start -> copy(start = date, editing = null)
            FeedDeadlineRangeEndpoint.End -> copy(end = date, editing = null)
        }
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

/**
 * Complete, display-ready state for [FeedFilterSheetContent].
 *
 * @property deadlineRange 「직접 지정」을 고른 동안의 범위. [deadline] 이 [FeedDeadlineFilter.Range] 일
 *  때만 값이 있다 — 프리셋과 범위가 동시에 걸린 상태를 계약에서 막는다.
 */
public data class FeedFilterUiState(
    val categories: List<FeedFilterUiModel>,
    val selectedCategory: FeedListingCategory,
    val boards: List<FeedBoardFilterUiModel>,
    val selectedBoardIds: Set<String>,
    val deadline: FeedDeadlineFilter,
    val deadlineRange: FeedDeadlineRange?,
    val minScore: FeedMinScoreFilter,
    val unreadOnly: Boolean,
    val matchingCount: Int?,
) {
    /** 「적용」을 누를 수 있는가 — 잘못된 범위는 막는다(기능 스펙 F2-3). */
    val isApplyEnabled: Boolean get() = deadlineRange?.error == null

    init {
        require((deadline == FeedDeadlineFilter.Range) == (deadlineRange != null)) {
            "deadlineRange must be present exactly when deadline is Range"
        }
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

    /** 「직접 지정」의 한쪽 끝을 눌렀다 — 그 끝을 고르는 날짜 선택기를 연다. */
    public data class DeadlineRangeEndpointClicked(
        val endpoint: FeedDeadlineRangeEndpoint,
    ) : FeedFilterEvent

    /** 날짜 선택기에서 날짜를 확정했다. 어느 끝인지는 [FeedDeadlineRange.editing] 이 안다. */
    public data class DeadlineRangeDateSelected(
        val date: LocalDate,
    ) : FeedFilterEvent

    /** 날짜 선택기를 그냥 닫았다 — 고른 범위는 그대로 둔다. */
    public data object DeadlineRangePickerDismissed : FeedFilterEvent

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
