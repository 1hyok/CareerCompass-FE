package com.cambridge.feature.feed.presentation.feed

import com.cambridge.core.model.board.Board
import com.cambridge.core.model.posting.Posting
import com.cambridge.core.model.user.UserProfile
import com.cambridge.feature.feed.domain.model.FeedQuery
import com.cambridge.feature.feed.domain.model.FeedSnapshot
import com.cambridge.feature.feed.presentation.FeedListingCategory
import com.cambridge.feature.feed.presentation.FeedLoadMoreState
import com.cambridge.feature.feed.presentation.feedfilter.FeedDeadlineFilter
import com.cambridge.feature.feed.presentation.feedfilter.FeedDeadlineRange
import com.cambridge.feature.feed.presentation.shared.model.FeedFailureReason
import com.cambridge.feature.feed.presentation.shared.model.lacksSuitabilityInputs
import com.cambridge.feature.feed.presentation.shared.util.toDomainDeadlineFilter
import com.cambridge.feature.feed.presentation.shared.util.toListingCategory
import com.cambridge.feature.feed.presentation.shared.util.toPostingTypes
import com.cambridge.feature.feed.presentation.shared.util.toUiDeadlineFilter
import com.cambridge.feature.feed.presentation.shared.util.toUiDeadlineRange
import java.time.Instant
import com.cambridge.feature.feed.domain.model.FeedDeadlineFilter as DomainDeadlineFilter

/** 목록 조회의 진행 상태. 항목은 [FeedViewState.postings] 에 따로 누적된다. */
public sealed interface FeedLoadState {
    public data object Loading : FeedLoadState

    public data object Loaded : FeedLoadState

    public data class Failed(
        val reason: FeedFailureReason,
    ) : FeedLoadState
}

/** 피드가 요청하는 이동. Entry 가 소비하고 [FeedViewModel.onNavigationConsumed] 로 비운다. */
public sealed interface FeedDestination {
    public data class PostingDetail(
        val postingId: Long,
    ) : FeedDestination

    public data object Notifications : FeedDestination

    public data object BoardRegister : FeedDestination

    public data object BoardList : FeedDestination

    /** 프로필 입력 안내 — 앱 셸이 마이 탭으로 보낸다. 공고 상세의 같은 이름 목적지와 같은 자리다. */
    public data object Profile : FeedDestination
}

/** 스낵바 한 줄로 끝나는 실패. 문구는 Entry 가 리소스로 만든다. */
public enum class FeedMessage {
    BookmarkFailed,
    LoadMoreFailed,
    RefreshFailed,

    /** 오프라인 모드에서 북마크를 눌렀다 — 스냅샷은 읽기 전용이다. */
    OfflineReadOnly,
}

/**
 * 필터 시트가 편집 중인 조건 — 「적용」 전까지 [FeedQuery] 에 반영되지 않는다.
 *
 * 카테고리는 칩 행과 같은 값을 다루므로 시트에서도 함께 고를 수 있다. 마감일은 도메인 값이 아니라 시트
 * 선택지([FeedDeadlineFilter])로 들고 있다 — 「직접 지정」을 고른 뒤 날짜를 찍기 전까지는 도메인으로
 * 옮길 수 없는 중간 상태이고, 그 상태에서 「적용」을 막아야 하기 때문이다.
 *
 * @property deadlineRange 「직접 지정」의 편집값. 시트가 열려 있는 동안에는 다른 선택지를 골라도 지우지
 *  않아, 되돌아오면 찍어 둔 날짜가 그대로 남는다.
 */
public data class FeedFilterDraft(
    val category: FeedListingCategory,
    val boardIds: Set<Long>,
    val deadline: FeedDeadlineFilter,
    val deadlineRange: FeedDeadlineRange,
    val minScore: Int?,
    val unreadOnly: Boolean,
) {
    /** 지금 「적용」을 누를 수 있는가 — 잘못된 범위는 막는다. */
    public val isApplicable: Boolean get() = deadline.toDomainDeadlineFilter(deadlineRange) != null

    /** 적용할 수 없는 초안([isApplicable] 이 false)이면 null 이다. */
    public fun applyTo(query: FeedQuery): FeedQuery? =
        deadline.toDomainDeadlineFilter(deadlineRange)?.let { deadlineFilter ->
            query.copy(
                types = category.toPostingTypes(),
                boardIds = boardIds,
                deadline = deadlineFilter,
                minScore = minScore,
                unreadOnly = unreadOnly,
            )
        }

    public companion object {
        public fun from(query: FeedQuery): FeedFilterDraft =
            FeedFilterDraft(
                category = query.types.toListingCategory(),
                boardIds = query.boardIds,
                deadline = query.deadline.toUiDeadlineFilter(),
                deadlineRange = query.deadline.toUiDeadlineRange(),
                minScore = query.minScore,
                unreadOnly = query.unreadOnly,
            )

        /** 「초기화」 — 카테고리와 고른 마감일 범위까지 되돌린다. */
        public val Default: FeedFilterDraft =
            FeedFilterDraft(
                category = FeedListingCategory.All,
                boardIds = emptySet(),
                deadline = FeedDeadlineFilter.All,
                deadlineRange = FeedDeadlineRange(),
                minScore = null,
                unreadOnly = false,
            )
    }
}

/**
 * 피드 홈의 전체 상태 — 도메인 값 그대로. 표시 문구(라벨·D-day·상대 시각)는 Entry 가 만든다.
 *
 * @property profile 마지막으로 받은 내 프로필. 인사말과 적합도 표시 판정의 근거다. 아직 못 받았으면 null.
 * @property searchInput 입력창의 현재 글자. 300ms 뒤 [query] 의 `searchQuery` 로 옮겨진다.
 * @property query 서버·클라이언트 조회 조건(카테고리 칩 = `types`, 필터 시트 = 나머지, 정렬 포함).
 * @property boards 등록된 게시판. 필터 시트의 선택지이자 빈 피드 사유 판정의 근거다.
 * @property boardsLoaded 게시판 목록을 실제로 받아 봤는가 — 조회에 실패해 비어 있는 것과 정말 0개인 것을
 *  가른다. 이 구분이 없으면 게시판 조회만 실패한 사용자에게 「등록한 게시판이 없어요」라고 하게 된다.
 * @property postings 지금까지 받은 페이지의 누적 목록. 오프라인 모드에서는 스냅샷의 목록.
 * @property nextCursor 다음 페이지 커서. null 이면 서버에 더 남은 것이 없다([hasNext]).
 * @property loadMore 목록 끝의 이어 읽기 상태 — 자동 페이징이 도는지, 사용자 손이 필요한지.
 * @property filterDraft 필터 시트가 열려 있으면 편집 중인 조건, 닫혀 있으면 null.
 * @property offlineSnapshot 네트워크 단절 실패 직후 읽어 둔 스냅샷 — 있으면 오류 화면에 「오프라인 모드로 보기」가 열린다.
 * @property isOffline 스냅샷 목록을 보여 주는 중. 더 불러오기·북마크는 잠긴다.
 * @property offlineSavedAt 보여 주는 스냅샷의 저장 시각(배너 문구 근거). [isOffline] 일 때만 값이 있다.
 */
public data class FeedViewState(
    val profile: UserProfile? = null,
    val todayNewCount: Int = 0,
    val searchInput: String = "",
    val query: FeedQuery = FeedQuery(),
    val boards: List<Board> = emptyList(),
    val boardsLoaded: Boolean = false,
    val postings: List<Posting> = emptyList(),
    val nextCursor: String? = null,
    val loadState: FeedLoadState = FeedLoadState.Loading,
    val isRefreshing: Boolean = false,
    val loadMore: FeedLoadMoreState = FeedLoadMoreState.Ready,
    val filterDraft: FeedFilterDraft? = null,
    val isSortMenuVisible: Boolean = false,
    val pendingNavigation: FeedDestination? = null,
    val message: FeedMessage? = null,
    val sessionEnded: Boolean = false,
    val offlineSnapshot: FeedSnapshot? = null,
    val isOffline: Boolean = false,
    val offlineSavedAt: Instant? = null,
) {
    public val userName: String? get() = profile?.name

    /**
     * 서버에 아직 더 남았는가 — **목록이 비어 있어도** 커서가 남았으면 끝이 아니다.
     *
     * 검색어·마감일은 서버 파라미터가 없어 받아 온 페이지 안에서만 걸러진다([FeedQuery.filterClientSide]).
     * 그래서 「받은 것이 없다」와 「서버에 없다」가 다르고, 이 값이 둘을 가른다 — 빈 목록의 사유 판정
     * (`toEmptyReason`)과 이어 읽기 잠금(`FeedEntry`)이 모두 여기에 기댄다.
     */
    public val hasNext: Boolean get() = nextCursor != null

    public val isLoadingMore: Boolean get() = loadMore == FeedLoadMoreState.Loading

    /**
     * 목록 위에 프로필 입력 안내를 얹을지 — 프로필이 비어 있고, 그 때문에 점수를 못 보이는 항목이 실제로
     * 있을 때만이다. 모든 공고에 점수가 붙어 있으면 안내할 것이 없다.
     */
    public val isProfileNoticeVisible: Boolean
        get() = profile.lacksSuitabilityInputs() && postings.any { it.score == null }

    public val selectedCategory: FeedListingCategory get() = query.types.toListingCategory()

    /** 기본값과 다른 시트 조건의 수 — 헤더 필터 버튼의 배지. 카테고리 칩은 눈에 보이므로 세지 않는다. */
    public val activeFilterCount: Int
        get() =
            listOf(
                query.boardIds.isNotEmpty(),
                query.deadline != DomainDeadlineFilter.All,
                query.minScore != null,
                query.unreadOnly,
            ).count { it }

    /**
     * 검색어 말고 목록을 좁히는 조건이 하나라도 걸려 있는가 — 빈 목록을 필터 탓으로 돌릴 근거다.
     *
     * 배지([activeFilterCount])와 달리 카테고리 칩도 센다. 배지는 「시트를 열어야 보이는 조건이 몇 개인가」
     * 를 말하지만, 여기서는 「초기화하면 결과가 달라지는가」를 묻기 때문이다 — 칩도 풀면 달라진다.
     */
    public val hasActiveFilter: Boolean
        get() = activeFilterCount > 0 || selectedCategory != FeedListingCategory.All
}
