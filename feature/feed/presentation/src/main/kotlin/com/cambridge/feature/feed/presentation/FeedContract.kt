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

/**
 * 목록이 빈 **사유** — 사유마다 사용자가 할 수 있는 일이 다르다.
 *
 * 한 문장으로 뭉뚱그리면 온보딩을 막 마쳐 게시판이 0개인 사용자에게 「검색어나 필터를 바꿔 보세요」라고
 * 하게 된다. 바꿀 검색어도 필터도 없고, 정작 해야 할 게시판 등록으로 가는 길도 없는 막다른 화면이다.
 *
 * ### 여러 사유가 겹칠 때의 우선순위
 *
 * [OfflineSnapshot] > [NoBoards] > [Search] > [Filter] > [NotCollected] 중 하나만 고른다(사유 판정은
 * `FeedViewState.toEmptyReason`). 기준은 「그 조건을 되돌리면 결과가 달라지는가」다.
 *
 * - **오프라인이 가장 앞이다.** 보고 있는 것이 저장해 둔 사본이라 「아직 수집 전」·「게시판 0개」처럼
 *   서버 상태를 단정할 근거가 없다. 게다가 조건을 되돌리는 행동은 곧 재조회라, 오프라인에서 권하면
 *   실패 화면으로 튄다 — 되돌릴 것을 권하지 않는 사유가 따로 있어야 한다.
 * - **게시판 0개가 그다음이다.** 모으는 곳이 없으면 검색어·필터를 어떻게 바꿔도 나올 공고가 없다.
 * - **검색어가 필터보다 앞이다.** 셋 다 근거다 — ① 검색칸은 화면 위에 그대로 보여 사용자가 무엇이
 *   걸렸는지 이미 알지만 필터는 시트 안에 접혀 있다, ② 검색어는 문구에 그대로 실어(「'백엔드' 검색
 *   결과가 없어요」) 구체적으로 말할 수 있다, ③ 검색어는 한 글자로 걸리고 필터는 시트를 열어 「적용」
 *   까지 눌러야 하므로 마지막에 바꾼 조건일 확률이 높다. 검색어를 지운 뒤에도 비어 있으면 그때
 *   [Filter] 가 나와 조건을 한 겹씩 벗겨 준다.
 */
public sealed interface FeedEmptyReason {
    /** 등록된 게시판이 하나도 없다 — 공고를 모을 곳 자체가 없다(기능 스펙 F2-1). */
    public data object NoBoards : FeedEmptyReason

    /**
     * 검색어로 걸러 남은 것이 없다.
     *
     * @property query 결과를 만든 그 검색어. 지금 입력칸의 글자가 아니라 조회에 실린 값이다 — 입력
     *  직후 300ms 동안은 둘이 다르고, 화면은 목록을 만든 조건을 말해야 한다.
     */
    public data class Search(
        val query: String,
    ) : FeedEmptyReason {
        init {
            requireNonBlank("query", query)
        }
    }

    /** 필터 시트 조건이나 카테고리 칩이 걸려 남은 것이 없다. */
    public data object Filter : FeedEmptyReason

    /**
     * 게시판은 있고 조건도 없는데 아직 모인 공고가 없다 — 첫 수집을 기다리는 중이다.
     *
     * @property collectNotice 언제쯤 들어오는지 알려 주는 수집 주기 한 줄(「등록한 게시판을 1일 1회
     *  확인하고 있어요」). 수집이 도는 게시판이 하나도 없어 그런 말을 할 근거가 없으면 null 이다.
     */
    public data class NotCollected(
        val collectNotice: String?,
    ) : FeedEmptyReason {
        init {
            require(collectNotice == null || collectNotice.isNotBlank()) {
                "collectNotice must be null or non-blank"
            }
        }
    }

    /**
     * 저장해 둔 오프라인 스냅샷(#86)을 보는 중인데 그 목록이 비었다.
     *
     * 스냅샷이 비었다는 사실과 오프라인이라는 사실을 섞지 않는다 — 오프라인이라는 것은 목록 위 배너가
     * 이미 말하고 있고, 여기서는 「저장해 둔 목록에 없다」는 것만 말한다.
     */
    public data object OfflineSnapshot : FeedEmptyReason
}

/** Mutually exclusive loading states for the listing portion of the feed. */
public sealed interface FeedContentState {
    public data object Loading : FeedContentState

    public data class Empty(
        val reason: FeedEmptyReason,
    ) : FeedContentState

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

    /** 빈 목록의 「필터 초기화」 — 시트 조건과 카테고리 칩을 함께 되돌린다. 검색어·정렬은 건드리지 않는다. */
    public data object FilterResetSelected : FeedUiEvent

    /** 빈 목록의 「게시판 등록하기」 — 게시판이 0개라 모을 것이 없는 사용자의 유일한 길이다. */
    public data object BoardRegisterSelected : FeedUiEvent

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
