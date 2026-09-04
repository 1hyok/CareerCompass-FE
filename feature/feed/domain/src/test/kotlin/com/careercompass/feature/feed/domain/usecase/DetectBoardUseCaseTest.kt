package com.careercompass.feature.feed.domain.usecase

import com.careercompass.core.domain.error.CoreDataFailure
import com.careercompass.core.domain.testing.FakeBoardRepository
import com.careercompass.core.model.board.BoardDetection
import com.careercompass.core.model.board.BoardDetectionStatus
import com.careercompass.feature.feed.domain.error.FeedFailure
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.net.UnknownHostException

class DetectBoardUseCaseTest {
    @Test
    fun `정규화한 URL 로 감지하고 그 URL 을 결과에 함께 돌려준다`() =
        runTest {
            val repository = FakeBoardRepository()

            val outcome = DetectBoardUseCase(repository)("  Konkuk.ac.kr/board/notice ").getOrThrow()

            assertEquals("https://konkuk.ac.kr/board/notice", outcome.url)
            assertEquals(repository.detection, outcome.detection)
            assertEquals(listOf("https://konkuk.ac.kr/board/notice"), repository.detectedUrls.toList())
        }

    @Test
    fun `감지 실패 상태도 결과로 전달한다`() =
        runTest {
            val blocked = BoardDetection(status = BoardDetectionStatus.Blocked, preview = emptyList(), hasDateSelector = false)
            val repository = FakeBoardRepository(detection = blocked)

            val outcome = DetectBoardUseCase(repository)("https://intra.example.com").getOrThrow()

            assertEquals(blocked, outcome.detection)
            assertTrue(!outcome.detection.isRegistrable)
        }

    @Test
    fun `형태가 잘못된 URL 은 요청 없이 InvalidBoardUrl 로 거절한다`() =
        runTest {
            val repository = FakeBoardRepository.strict()
            val useCase = DetectBoardUseCase(repository)

            listOf("", "ftp://konkuk.ac.kr", "https://", "https://konkuk ac.kr").forEach { input ->
                val failure = useCase(input).exceptionOrNull()
                assertTrue("'$input'", failure is FeedFailure.InvalidBoardUrl)
                assertEquals(input, (failure as FeedFailure.InvalidBoardUrl).input)
            }
            assertTrue(repository.detectedUrls.isEmpty())
        }

    @Test
    fun `서버 실패는 그대로 돌려준다`() =
        runTest {
            val repository =
                FakeBoardRepository.strict().apply {
                    onDetect = { Result.failure(CoreDataFailure.NetworkUnavailable(UnknownHostException())) }
                }

            val outcome = DetectBoardUseCase(repository)("https://konkuk.ac.kr")

            assertTrue(outcome.exceptionOrNull() is CoreDataFailure.NetworkUnavailable)
        }
}
