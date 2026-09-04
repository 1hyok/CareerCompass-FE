package com.careercompass.feature.feed.domain.usecase

import com.careercompass.core.domain.repository.PostingRepository
import com.careercompass.core.model.posting.PostingQuery
import com.careercompass.core.model.posting.PostingSort
import java.time.Clock
import java.time.LocalDate
import javax.inject.Inject

/**
 * 오늘 새로 수집된 공고 수를 센다.
 *
 * 수집 최신순 첫 페이지부터 `collectedAt` 이 오늘([Clock] 의 시간대 기준)인 항목을 세고, 페이지 끝까지
 * 오늘이면 `nextCursor` 를 따라 최대 [MAX_PAGES] 페이지까지 이어간다. 그 너머는 세지 않으므로
 * 결과는 하한이다. 중간에 페이지 조회가 실패하면 부분 합을 돌려주지 않고 실패로 끝낸다.
 */
public class CountTodayNewPostingsUseCase
    @Inject
    constructor(
        private val postingRepository: PostingRepository,
        private val clock: Clock,
    ) {
        public suspend operator fun invoke(): Result<Int> {
            val today = LocalDate.now(clock)
            var cursor: String? = null
            var count = 0
            repeat(MAX_PAGES) {
                val page =
                    postingRepository
                        .getPostings(PostingQuery(sort = PostingSort.CollectedDesc, cursor = cursor))
                        .getOrElse { return Result.failure(it) }
                val collectedToday = page.items.takeWhile { it.collectedAt.atZone(clock.zone).toLocalDate() == today }
                count += collectedToday.size
                val reachedOlder = collectedToday.size < page.items.size
                cursor = page.nextCursor
                if (reachedOlder || cursor == null) return Result.success(count)
            }
            return Result.success(count)
        }

        public companion object {
            /** 이어서 세는 페이지 상한 — 기본 페이지 크기 기준 최대 100건까지 센다. */
            public const val MAX_PAGES: Int = 5
        }
    }
