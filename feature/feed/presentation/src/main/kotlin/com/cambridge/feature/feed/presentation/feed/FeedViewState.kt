package com.cambridge.feature.feed.presentation.feed

import com.cambridge.core.model.board.Board
import com.cambridge.core.model.posting.Posting
import com.cambridge.feature.feed.domain.model.FeedDeadlineFilter
import com.cambridge.feature.feed.domain.model.FeedQuery
import com.cambridge.feature.feed.presentation.FeedListingCategory
import com.cambridge.feature.feed.presentation.shared.util.toListingCategory
import com.cambridge.feature.feed.presentation.shared.util.toPostingTypes

/** 목록 조회의 진행 상태. 항목은 [FeedViewState.postings] 에 따로 누적된다. */
public sealed interface FeedLoadState {
    public data object Loading : FeedLoadState

    public data object Loaded : FeedLoadState

    public data class Failed(
        val isNetworkUnavailable: Boolean,
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
}

/** 스낵바 한 줄로 끝나는 실패. 문구는 Entry 가 리소스로 만든다. */
public enum class FeedMessage {
    BookmarkFailed,
    LoadMoreFailed,
    RefreshFailed,
}

/**
 * 필터 시트가 편집 중인 조건 — 「적용」 전까지 [FeedQuery] 에 반영되지 않는다.
 *
 * 카테고리는 칩 행과 같은 값을 다루므로 시트에서도 함께 고를 수 있다.
 */
public data class FeedFilterDraft(
    val category: FeedListingCategory,
    val boardIds: Set<Long>,
    val deadline: FeedDeadlineFilter,
    val minScore: Int?,
    val unreadOnly: Boolean,
) {
    public fun applyTo(query: FeedQuery): FeedQuery =
        query.copy(
            types = category.toPostingTypes(),
            boardIds = boardIds,
            deadline = deadline,
            minScore = minScore,
            unreadOnly = unreadOnly,
        )

    public companion object {
        public fun from(query: FeedQuery): FeedFilterDraft =
            FeedFilterDraft(
                category = query.types.toListingCategory(),
                boardIds = query.boardIds,
                deadline = query.deadline,
                minScore = query.minScore,
                unreadOnly = query.unreadOnly,
            )

        /** 「초기화」 — 카테고리까지 「전체」로 되돌린다. */
        public val Default: FeedFilterDraft =
            FeedFilterDraft(
                category = FeedListingCategory.All,
                boardIds = emptySet(),
                deadline = FeedDeadlineFilter.All,
                minScore = null,
                unreadOnly = false,
            )
    }
}

/**
 * 피드 홈의 전체 상태 — 도메인 값 그대로. 표시 문구(라벨·D-day·상대 시각)는 Entry 가 만든다.
 *
 * @property searchInput 입력창의 현재 글자. 300ms 뒤 [query] 의 `searchQuery` 로 옮겨진다.
 * @property query 서버·클라이언트 조회 조건(카테고리 칩 = `types`, 필터 시트 = 나머지, 정렬 포함).
 * @property postings 지금까지 받은 페이지의 누적 목록.
 * @property filterDraft 필터 시트가 열려 있으면 편집 중인 조건, 닫혀 있으면 null.
 */
public data class FeedViewState(
    val userName: String? = null,
    val todayNewCount: Int = 0,
    val searchInput: String = "",
    val query: FeedQuery = FeedQuery(),
    val boards: List<Board> = emptyList(),
    val postings: List<Posting> = emptyList(),
    val nextCursor: String? = null,
    val loadState: FeedLoadState = FeedLoadState.Loading,
    val isRefreshing: Boolean = false,
    val isLoadingMore: Boolean = false,
    val filterDraft: FeedFilterDraft? = null,
    val isSortMenuVisible: Boolean = false,
    val pendingNavigation: FeedDestination? = null,
    val message: FeedMessage? = null,
    val sessionEnded: Boolean = false,
) {
    public val hasNext: Boolean get() = nextCursor != null

    public val selectedCategory: FeedListingCategory get() = query.types.toListingCategory()

    /** 기본값과 다른 시트 조건의 수 — 헤더 필터 버튼의 배지. 카테고리 칩은 눈에 보이므로 세지 않는다. */
    public val activeFilterCount: Int
        get() =
            listOf(
                query.boardIds.isNotEmpty(),
                query.deadline != FeedDeadlineFilter.All,
                query.minScore != null,
                query.unreadOnly,
            ).count { it }
}
