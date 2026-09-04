package com.careercompass.feature.feed.domain.usecase

import com.careercompass.core.domain.error.CoreDataFailure
import com.careercompass.core.domain.testing.FakePostingRepository
import com.careercompass.core.model.posting.PostingQuery
import com.careercompass.core.model.posting.PostingSort
import com.careercompass.feature.feed.domain.FIXED_CLOCK
import com.careercompass.feature.feed.domain.NOON_TODAY
import com.careercompass.feature.feed.domain.posting
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.net.UnknownHostException
import java.time.Duration
import java.time.Instant

class CountTodayNewPostingsUseCaseTest {
    private fun useCase(repository: FakePostingRepository) = CountTodayNewPostingsUseCase(repository, FIXED_CLOCK)

    /** 앞에서 [todayCount] 건은 오늘 정오부터 1분 간격으로, 나머지는 이틀 전으로 수집된 목록. */
    private fun postings(
        todayCount: Int,
        olderCount: Int,
    ) = List(todayCount) { posting(id = it + 1L, collectedAt = NOON_TODAY.minus(Duration.ofMinutes(it.toLong()))) } +
        List(olderCount) { posting(id = 1_000L + it, collectedAt = NOON_TODAY.minus(Duration.ofDays(2)).minusSeconds(it.toLong())) }

    @Test
    fun `첫 페이지에서 오늘 수집분만 세고 그 뒤로는 조회하지 않는다`() =
        runTest {
            val repository = FakePostingRepository(initial = postings(todayCount = 3, olderCount = 40))

            assertEquals(Result.success(3), useCase(repository)())
            assertEquals(
                listOf(PostingQuery(sort = PostingSort.CollectedDesc, cursor = null)),
                repository.queries.toList(),
            )
        }

    @Test
    fun `페이지 끝까지 오늘이면 nextCursor 를 따라 이어서 센다`() =
        runTest {
            val repository = FakePostingRepository(initial = postings(todayCount = 42, olderCount = 10))

            assertEquals(Result.success(42), useCase(repository)())
            assertEquals(listOf(null, "20", "40"), repository.queries.map(PostingQuery::cursor))
        }

    @Test
    fun `마지막 페이지가 모두 오늘이면 커서가 없으므로 멈춘다`() =
        runTest {
            val repository = FakePostingRepository(initial = postings(todayCount = 25, olderCount = 0))

            assertEquals(Result.success(25), useCase(repository)())
            assertEquals(2, repository.queries.size)
        }

    @Test
    fun `최대 5페이지까지만 센다`() =
        runTest {
            val repository = FakePostingRepository(initial = postings(todayCount = 130, olderCount = 0))

            assertEquals(Result.success(100), useCase(repository)())
            assertEquals(CountTodayNewPostingsUseCase.MAX_PAGES, repository.queries.size)
        }

    @Test
    fun `오늘 여부는 시계의 시간대로 판단한다`() =
        runTest {
            val lateYesterdayUtc = Instant.parse("2026-09-01T16:30:00Z") // 09-02 01:30 KST
            val earlierYesterdayUtc = Instant.parse("2026-09-01T14:30:00Z") // 09-01 23:30 KST
            val repository =
                FakePostingRepository(
                    initial =
                        listOf(
                            posting(id = 1, collectedAt = lateYesterdayUtc),
                            posting(id = 2, collectedAt = earlierYesterdayUtc),
                        ),
                )

            assertEquals(Result.success(1), useCase(repository)())
        }

    @Test
    fun `빈 목록은 0 이다`() =
        runTest {
            assertEquals(Result.success(0), useCase(FakePostingRepository())())
        }

    @Test
    fun `이어 세는 도중 조회가 실패하면 부분 합 없이 실패로 끝낸다`() =
        runTest {
            val repository = FakePostingRepository(initial = postings(todayCount = 42, olderCount = 0))
            val delegate = FakePostingRepository(initial = repository.postings.toList())
            repository.onGetPostings = { query ->
                when (query.cursor) {
                    null -> delegate.getPostings(query)
                    else -> Result.failure(CoreDataFailure.NetworkUnavailable(UnknownHostException()))
                }
            }

            val outcome = useCase(repository)()

            assertTrue(outcome.exceptionOrNull() is CoreDataFailure.NetworkUnavailable)
        }
}
