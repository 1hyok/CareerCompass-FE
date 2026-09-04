package com.cambridge.feature.feed.domain.usecase

import com.cambridge.feature.feed.domain.postingDetail
import com.careercompass.core.domain.error.CoreDataFailure
import com.careercompass.core.domain.testing.FakePostingRepository
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.net.UnknownHostException

class OpenPostingDetailUseCaseTest {
    @Test
    fun `읽지 않은 공고는 상세 조회 후 읽음 처리하고 읽은 상태로 돌려준다`() =
        runTest {
            val repository = FakePostingRepository(details = listOf(postingDetail(id = 7, isRead = false)))

            val outcome = OpenPostingDetailUseCase(repository)(7)

            assertEquals(Result.success(postingDetail(id = 7, isRead = true)), outcome)
            assertEquals(listOf(7L), repository.readCalls.toList())
        }

    @Test
    fun `읽음 처리가 실패해도 상세는 성공으로 돌려준다`() =
        runTest {
            val repository =
                FakePostingRepository(
                    details = listOf(postingDetail(id = 7, isRead = false)),
                    onMarkRead = { Result.failure(CoreDataFailure.ServerError("INTERNAL_ERROR", RuntimeException())) },
                )

            val outcome = OpenPostingDetailUseCase(repository)(7)

            assertEquals(Result.success(postingDetail(id = 7, isRead = false)), outcome)
            assertEquals(listOf(7L), repository.readCalls.toList())
        }

    @Test
    fun `이미 읽은 공고는 읽음 처리를 요청하지 않는다`() =
        runTest {
            val repository =
                FakePostingRepository.strict().apply {
                    onGetPostingDetail = { Result.success(postingDetail(id = it, isRead = true)) }
                }

            val outcome = OpenPostingDetailUseCase(repository)(7)

            assertEquals(Result.success(postingDetail(id = 7, isRead = true)), outcome)
            assertTrue(repository.readCalls.isEmpty())
        }

    @Test
    fun `상세 조회가 실패하면 읽음 처리 없이 실패를 돌려준다`() =
        runTest {
            val repository =
                FakePostingRepository.strict().apply {
                    onGetPostingDetail = { Result.failure(CoreDataFailure.NetworkUnavailable(UnknownHostException())) }
                }

            val outcome = OpenPostingDetailUseCase(repository)(7)

            assertTrue(outcome.exceptionOrNull() is CoreDataFailure.NetworkUnavailable)
            assertTrue(repository.readCalls.isEmpty())
        }
}
