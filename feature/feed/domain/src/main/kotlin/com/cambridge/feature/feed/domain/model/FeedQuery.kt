package com.cambridge.feature.feed.domain.model

import com.cambridge.core.model.posting.Posting
import com.cambridge.core.model.posting.PostingQuery
import com.cambridge.core.model.posting.PostingSort
import com.cambridge.core.model.posting.PostingType
import java.time.LocalDate

/**
 * 마감일 필터 — 기능 스펙 F2-3 「필터 조건 · 마감일」(마감 임박순 / 마감일 지정 범위).
 *
 * 마감이 지난 공고는 목록에서 기본 숨김이고 [IncludeExpired]·[Range] 로만 드러난다. 서버 파라미터가 없어
 * 클라이언트에서 적용한다([GetFeedPageUseCase][com.cambridge.feature.feed.domain.usecase.GetFeedPageUseCase]).
 *
 * enum 이 아니라 sealed 인 이유 — [Range] 만 값(시작·종료일)을 들고 있는데 프리셋과 배타적이다. 「프리셋
 * 하나 + 범위 필드」로 나눠 두면 둘이 어긋난 상태(7일 이내인데 범위도 걸림)가 타입에 남는다.
 */
public sealed interface FeedDeadlineFilter {
    /** 마감 지난 공고만 숨기고 나머지(마감일 없음 포함)는 모두 보인다. */
    public data object All : FeedDeadlineFilter

    /** 오늘부터 7일 이내 마감(당일 포함). 마감일 없는 공고는 제외. */
    public data object WithinWeek : FeedDeadlineFilter

    /** 오늘부터 30일 이내 마감(당일 포함). 마감일 없는 공고는 제외. */
    public data object WithinMonth : FeedDeadlineFilter

    /** 마감 지난 공고까지 모두 보인다. */
    public data object IncludeExpired : FeedDeadlineFilter

    /**
     * 직접 고른 마감일 범위 — 양 끝을 포함하고, 마감일 없는 공고는 제외한다.
     *
     * **한쪽만 골라도 된다** — [start] 만이면 그날부터, [end] 만이면 그날까지다. 두 끝을 모두 강제하면
     * 「11월부터 마감하는 공고」를 보려는 사람이 반대쪽 끝을 아무 날이나 찍게 되어 조건이 왜곡된다.
     * 둘 다 비면 거르는 것이 없으므로 [All] 과 구분되지 않는다 — 값 자체를 만들지 못하게 막는다.
     *
     * 오늘 기준 「마감 지난 공고 숨김」을 얹지 않는 이유 — 날짜를 직접 고른 조회에까지 그 규칙을 적용하면
     * 지난 범위를 고른 결과가 언제나 빈 목록이 되어 필터가 거짓말을 한다.
     */
    public data class Range(
        val start: LocalDate?,
        val end: LocalDate?,
    ) : FeedDeadlineFilter {
        init {
            require(start != null || end != null) { "range must have at least one of start/end" }
            require(start == null || end == null || !end.isBefore(start)) {
                "range end must not be before start"
            }
        }

        /** [date] 가 범위 안인가 — 열린 쪽 끝은 항상 통과다. */
        public fun contains(date: LocalDate): Boolean = (start == null || !date.isBefore(start)) && (end == null || !date.isAfter(end))
    }
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

    /**
     * 서버 파라미터가 없어 클라이언트가 직접 거르는 조건([deadline]·[searchQuery])을 [postings] 에 적용한다.
     *
     * 조회 결과([GetFeedPageUseCase][com.cambridge.feature.feed.domain.usecase.GetFeedPageUseCase])와
     * 오프라인 스냅샷 목록이 **같은 코드**를 쓰게 하려고 규칙을 여기 한 곳에 둔다 — 두 자리로 나뉘면 마감일
     * 조건이 한쪽에만 반영된다.
     *
     * 서버 조건(유형·게시판·적합도·읽음)은 여기서 다시 적용하지 않는다. 그건 서버가 판단해 이미 걸러 준
     * 몫이고, 클라이언트가 흉내 내기 시작하면 온라인 결과와 어긋나는 두 번째 정본이 생긴다.
     *
     * @param today 「오늘」 기준일. 호출자가 주입한 시계로 만든다.
     */
    public fun filterClientSide(
        postings: List<Posting>,
        today: LocalDate,
    ): List<Posting> = postings.filter { matchesDeadline(it, today) && matchesSearch(it) }

    private fun matchesDeadline(
        posting: Posting,
        today: LocalDate,
    ): Boolean =
        when (deadline) {
            FeedDeadlineFilter.All -> !posting.isExpired(today)
            FeedDeadlineFilter.IncludeExpired -> true
            FeedDeadlineFilter.WithinWeek -> posting.isDueWithin(WEEK_DAYS, today)
            FeedDeadlineFilter.WithinMonth -> posting.isDueWithin(MONTH_DAYS, today)
            is FeedDeadlineFilter.Range -> posting.dueDate?.let(deadline::contains) == true
        }

    private fun Posting.isDueWithin(
        maxDays: Long,
        today: LocalDate,
    ): Boolean = daysUntilDue(today)?.let { it in 0..maxDays } == true

    private fun matchesSearch(posting: Posting): Boolean = searchQuery.isEmpty() || posting.title.contains(searchQuery, ignoreCase = true)

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

        private const val WEEK_DAYS = 7L
        private const val MONTH_DAYS = 30L

        private val DEFAULT: FeedQuery by lazy { FeedQuery() }
    }
}
