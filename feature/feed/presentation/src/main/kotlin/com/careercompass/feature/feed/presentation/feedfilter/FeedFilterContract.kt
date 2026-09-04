package com.careercompass.feature.feed.presentation.feedfilter

import com.careercompass.feature.feed.presentation.FeedFilterUiModel
import com.careercompass.feature.feed.presentation.FeedListingCategory
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

/**
 * 「적합도 하한」 선택지 — 값은 F3-2 의 **레이블 경계**다(이슈 #200).
 *
 * 스펙 F2-3 이 적은 70 을 뺐다. 70 은 어떤 레이블의 경계도 아니라서 「70점 이상」으로 거르면
 * 「적합」(60~79)이 반으로 잘려 나오고, 그렇게 걸러진 목록을 화면이 설명할 말이 없다. 선택지의 값과
 * 카드에 뜨는 레이블이 같은 경계를 쓰게 맞춘 것이다 —
 * 근거는 [FeedQuery.ALLOWED_MIN_SCORES][com.careercompass.feature.feed.domain.model.FeedQuery.ALLOWED_MIN_SCORES]
 * 와 `docs/spec/suitability-score-boundary.md`.
 */
public enum class FeedMinScoreFilter {
    All,

    /** 60점 이상 — 「적합」 이상. */
    AtLeast60,

    /** 80점 이상 — 「매우 적합」. */
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
 * 조건에 걸린 게시판이 목록에 없는 이유 — 문구는 시트가 리소스로 옮긴다.
 *
 * **둘을 가르는 것이 이 타입의 존재 이유다**(이슈 #155). 목록을 받아 본 적이 없으면 「없다」는 사실이
 * 아니라 모름이고, 그 둘에 같은 말을 붙이면 잠깐 연결이 끊긴 사용자에게 게시판이 지워졌다고 하게 된다.
 */
public enum class FeedMissingBoardsReason {
    /** 목록을 받아 봤는데 그 안에 없다 — 그 사이 지워진 게시판이다. */
    Deleted,

    /** 목록을 아직 못 받았다(조회 실패) — 지워졌는지 알 수 없다. */
    Unverified,
}

/**
 * 시트의 게시판 목록에는 없는데 조건에는 남아 있는 게시판 — 켜진 태그 하나로 묶어 보인다(이슈 #155).
 *
 * **조용히 버리지 않는 이유**: 버리면 사용자가 모르는 사이 조회 조건이 넓어져 목록에 없던 공고가 섞인다.
 * **남기기만 하지 않는 이유**: 시트에 안 보이면 끌 방법이 없는데 배지에는 세어져, 「필터 2개」를 보고 연
 * 시트에 하나만 켜져 있게 된다. 그래서 **보이되 끌 수 있게** 한다.
 *
 * 이름이 아니라 개수로 묶는 이유 — 조건이 들고 있는 것은 id 뿐이라 지워진 게시판의 이름을 알 길이 없다.
 * 이름 없는 태그를 여러 개 늘어놓으면 어느 것이 무엇인지 못 고르므로, 한 번에 끄는 태그 하나로 준다.
 *
 * @property count 목록에 없는 게시판 id 의 수.
 */
public data class FeedMissingBoardsUiModel(
    val count: Int,
    val reason: FeedMissingBoardsReason,
) {
    init {
        require(count > 0) { "count must be positive" }
    }
}

/**
 * Complete, display-ready state for [FeedFilterSheetContent].
 *
 * @property deadlineRange 「직접 지정」을 고른 동안의 범위. [deadline] 이 [FeedDeadlineFilter.Range] 일
 *  때만 값이 있다 — 프리셋과 범위가 동시에 걸린 상태를 계약에서 막는다.
 * @property missingBoards [boards] 에 없는데 조건에는 걸려 있는 게시판. 없으면 null. 이것이 있는데 시트가
 *  그리지 않으면 끄지 못하는 조건이 생긴다([activeConditionCount] 참고).
 */
public data class FeedFilterUiState(
    val categories: List<FeedFilterUiModel>,
    val selectedCategory: FeedListingCategory,
    val boards: List<FeedBoardFilterUiModel>,
    val selectedBoardIds: Set<String>,
    val missingBoards: FeedMissingBoardsUiModel?,
    val deadline: FeedDeadlineFilter,
    val deadlineRange: FeedDeadlineRange?,
    val minScore: FeedMinScoreFilter,
    val unreadOnly: Boolean,
    val matchingCount: Int?,
) {
    /** 「적용」을 누를 수 있는가 — 잘못된 범위는 막는다(기능 스펙 F2-3). */
    val isApplyEnabled: Boolean get() = deadlineRange?.error == null

    /**
     * 이 시트에 **켜진 채로 보이는** 조건의 수 — 헤더 배지(`FeedViewState.activeFilterCount`)와 같아야 한다.
     *
     * 배지와 시트가 세는 자리가 갈려 있어 어긋날 수 있었다(이슈 #155). 배지는 조회 조건을, 시트는 그린 것을
     * 보고 세는데, 목록에 없는 게시판 id 를 시트가 조용히 버리던 동안 그 하나가 배지에만 남았다. 여기서
     * 「보이는 것」의 수를 계약이 직접 말하게 해, 둘이 같은지 테스트가 붙들 수 있게 한다.
     *
     * 게시판은 몇 개를 골랐든 조건 하나로 센다 — 배지의 셈과 같은 규칙이고, [missingBoards] 도 실제로
     * 목록을 좁히고 있으므로 함께 센다.
     */
    public val activeConditionCount: Int
        get() =
            listOf(
                selectedBoardIds.isNotEmpty() || missingBoards != null,
                deadline != FeedDeadlineFilter.All,
                minScore != FeedMinScoreFilter.All,
                unreadOnly,
            ).count { it }

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

    /** 목록에 없는 게시판 조건을 한 번에 껐다([FeedFilterUiState.missingBoards]). */
    public data object MissingBoardsCleared : FeedFilterEvent

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
