package com.cambridge.feature.feed.domain.usecase

import com.cambridge.core.domain.repository.PostingRepository
import com.cambridge.core.model.posting.Posting
import com.cambridge.feature.feed.domain.model.FeedDeadlineFilter
import com.cambridge.feature.feed.domain.model.FeedPage
import com.cambridge.feature.feed.domain.model.FeedQuery
import java.time.Clock
import java.time.LocalDate
import javax.inject.Inject

/**
 * 피드 한 페이지를 조회한다 — 서버 필터([FeedQuery.toPostingQuery]) 뒤에 클라이언트 규칙을 얹는다.
 *
 * 1. 마감 지난 공고는 [FeedDeadlineFilter.IncludeExpired] 가 아니면 숨긴다(기능 스펙 F2-3 「마감」).
 * 2. [FeedDeadlineFilter.WithinWeek]·[FeedDeadlineFilter.WithinMonth] 는 오늘부터 7·30일 이내 마감만
 *    남긴다(당일 포함, 마감일 없는 공고 제외).
 * 3. 검색어가 있으면 제목에 대소문자 무시로 포함되는 공고만 남긴다.
 *
 * 「오늘」은 주입된 [Clock] 기준이다. 필터로 항목이 줄어도 `nextCursor` 는 그대로 넘기므로
 * 빈 페이지가 끝을 뜻하지 않는다([FeedPage] 참고).
 */
public class GetFeedPageUseCase
    @Inject
    constructor(
        private val postingRepository: PostingRepository,
        private val clock: Clock,
    ) {
        public suspend operator fun invoke(
            query: FeedQuery,
            cursor: String? = null,
        ): Result<FeedPage> {
            val page =
                postingRepository
                    .getPostings(query.toPostingQuery(cursor))
                    .getOrElse { return Result.failure(it) }
            val today = LocalDate.now(clock)
            val postings =
                page.items.filter { posting ->
                    posting.matchesDeadline(query.deadline, today) && posting.matchesSearch(query.searchQuery)
                }
            return Result.success(FeedPage(postings = postings, nextCursor = page.nextCursor))
        }

        private fun Posting.matchesDeadline(
            filter: FeedDeadlineFilter,
            today: LocalDate,
        ): Boolean =
            when (filter) {
                FeedDeadlineFilter.All -> !isExpired(today)
                FeedDeadlineFilter.IncludeExpired -> true
                FeedDeadlineFilter.WithinWeek -> isDueWithin(WEEK_DAYS, today)
                FeedDeadlineFilter.WithinMonth -> isDueWithin(MONTH_DAYS, today)
            }

        private fun Posting.isDueWithin(
            maxDays: Long,
            today: LocalDate,
        ): Boolean = daysUntilDue(today)?.let { it in 0..maxDays } == true

        private fun Posting.matchesSearch(searchQuery: String): Boolean =
            searchQuery.isEmpty() || title.contains(searchQuery, ignoreCase = true)

        private companion object {
            const val WEEK_DAYS = 7L
            const val MONTH_DAYS = 30L
        }
    }
