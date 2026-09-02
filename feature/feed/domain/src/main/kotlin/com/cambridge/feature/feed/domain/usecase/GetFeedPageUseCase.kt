package com.cambridge.feature.feed.domain.usecase

import com.cambridge.core.domain.repository.PostingRepository
import com.cambridge.feature.feed.domain.model.FeedDeadlineFilter
import com.cambridge.feature.feed.domain.model.FeedPage
import com.cambridge.feature.feed.domain.model.FeedQuery
import java.time.Clock
import java.time.LocalDate
import javax.inject.Inject

/**
 * 피드 한 페이지를 조회한다 — 서버 필터([FeedQuery.toPostingQuery]) 뒤에 클라이언트 규칙을 얹는다.
 *
 * 클라이언트 규칙은 [FeedQuery.filterClientSide] 한 곳에 있다(오프라인 스냅샷 목록도 같은 함수를 쓴다).
 *
 * 1. 마감 지난 공고는 [FeedDeadlineFilter.IncludeExpired] 가 아니면 숨긴다(기능 스펙 F2-3 「마감」).
 * 2. [FeedDeadlineFilter.WithinWeek]·[FeedDeadlineFilter.WithinMonth] 는 오늘부터 7·30일 이내 마감만
 *    남긴다(당일 포함, 마감일 없는 공고 제외).
 * 3. [FeedDeadlineFilter.Range] 는 고른 범위(양 끝 포함) 안에 마감하는 공고만 남긴다 — 오늘과 무관하고,
 *    마감일 없는 공고는 제외한다.
 * 4. 검색어가 있으면 제목에 대소문자 무시로 포함되는 공고만 남긴다.
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
            val postings = query.filterClientSide(page.items, LocalDate.now(clock))
            return Result.success(FeedPage(postings = postings, nextCursor = page.nextCursor))
        }
    }
