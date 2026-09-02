package com.cambridge.feature.feed.domain.model

import com.cambridge.core.model.posting.PostingQuery
import com.cambridge.core.model.posting.PostingSort
import com.cambridge.core.model.posting.PostingType

/**
 * 마감일 필터 — 기능 스펙 F2-3 「필터 조건 · 마감일」과 「공고 상태 관리 · 마감」.
 *
 * 마감이 지난 공고는 목록에서 기본 숨김이고 [IncludeExpired] 로만 드러난다. 서버 파라미터가 없어
 * 클라이언트에서 적용한다([GetFeedPageUseCase][com.cambridge.feature.feed.domain.usecase.GetFeedPageUseCase]).
 */
public enum class FeedDeadlineFilter {
    /** 마감 지난 공고만 숨기고 나머지(마감일 없음 포함)는 모두 보인다. */
    All,

    /** 오늘부터 7일 이내 마감(당일 포함). 마감일 없는 공고는 제외. */
    WithinWeek,

    /** 오늘부터 30일 이내 마감(당일 포함). 마감일 없는 공고는 제외. */
    WithinMonth,

    /** 마감 지난 공고까지 모두 보인다. */
    IncludeExpired,
}

/**
 * 피드 목록 조회 조건 — 기능 스펙 F2-3 필터·정렬을 한 값으로 묶는다.
 *
 * 유형·게시판·최소 점수·읽지 않음·정렬은 서버 파라미터([toPostingQuery])로 옮기고,
 * [deadline]·[searchQuery] 는 서버 파라미터가 없어 클라이언트에서 적용한다.
 *
 * `data class` 가 아닌 이유 — [searchQuery] 를 **앞뒤 공백을 지운 값으로 저장**해야 하는데 data class 는
 * 생성자 인자를 가공해 저장할 수 없다. 대신 [copy]·[equals]·[hashCode]·[toString] 을 직접 제공한다.
 *
 * @property minScore 적합도 하한. 스펙의 선택지(60·70·80)만 허용한다 — null 은 「전체」.
 * @property searchQuery 제목 검색어(앞뒤 공백 제거). 빈 문자열이면 검색하지 않는다.
 */
public class FeedQuery(
    public val types: Set<PostingType> = emptySet(),
    public val boardIds: Set<Long> = emptySet(),
    public val deadline: FeedDeadlineFilter = FeedDeadlineFilter.All,
    public val minScore: Int? = null,
    public val unreadOnly: Boolean = false,
    public val sort: PostingSort = PostingSort.CollectedDesc,
    searchQuery: String = "",
) {
    public val searchQuery: String = searchQuery.trim()

    init {
        require(minScore == null || minScore in ALLOWED_MIN_SCORES) {
            "minScore must be null or one of $ALLOWED_MIN_SCORES"
        }
    }

    public val hasSearchQuery: Boolean get() = searchQuery.isNotEmpty()

    /**
     * 아무 조건도 걸리지 않은 기본 조회(`FeedQuery()`)인가 — 오프라인 스냅샷([FeedSnapshot])은 이 조건의
     * 첫 페이지만 저장한다.
     */
    public val isDefault: Boolean get() = this == DEFAULT

    /**
     * 서버 `GET /postings` 쿼리로 옮긴다. 집합은 정렬해 실어 같은 조건이면 같은 요청이 되게 한다.
     * 검색어·마감일 필터는 서버 파라미터가 없어 여기 실리지 않는다.
     */
    public fun toPostingQuery(
        cursor: String?,
        limit: Int = PostingQuery.DEFAULT_LIMIT,
    ): PostingQuery =
        PostingQuery(
            boardIds = boardIds.sorted(),
            types = types.sortedBy(PostingType::ordinal),
            minScore = minScore,
            unreadOnly = unreadOnly,
            sort = sort,
            cursor = cursor,
            limit = limit,
        )

    public fun copy(
        types: Set<PostingType> = this.types,
        boardIds: Set<Long> = this.boardIds,
        deadline: FeedDeadlineFilter = this.deadline,
        minScore: Int? = this.minScore,
        unreadOnly: Boolean = this.unreadOnly,
        sort: PostingSort = this.sort,
        searchQuery: String = this.searchQuery,
    ): FeedQuery =
        FeedQuery(
            types = types,
            boardIds = boardIds,
            deadline = deadline,
            minScore = minScore,
            unreadOnly = unreadOnly,
            sort = sort,
            searchQuery = searchQuery,
        )

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is FeedQuery) return false
        return types == other.types &&
            boardIds == other.boardIds &&
            deadline == other.deadline &&
            minScore == other.minScore &&
            unreadOnly == other.unreadOnly &&
            sort == other.sort &&
            searchQuery == other.searchQuery
    }

    override fun hashCode(): Int {
        var result = types.hashCode()
        result = 31 * result + boardIds.hashCode()
        result = 31 * result + deadline.hashCode()
        result = 31 * result + (minScore ?: 0)
        result = 31 * result + unreadOnly.hashCode()
        result = 31 * result + sort.hashCode()
        result = 31 * result + searchQuery.hashCode()
        return result
    }

    override fun toString(): String =
        "FeedQuery(types=$types, boardIds=$boardIds, deadline=$deadline, minScore=$minScore, " +
            "unreadOnly=$unreadOnly, sort=$sort, searchQuery='$searchQuery')"

    public companion object {
        /** 기능 스펙 F2-3 「적합도 점수」 필터 선택지. */
        public val ALLOWED_MIN_SCORES: Set<Int> = setOf(60, 70, 80)

        private val DEFAULT: FeedQuery by lazy { FeedQuery() }
    }
}
