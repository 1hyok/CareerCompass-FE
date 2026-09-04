package com.careercompass.feature.feed.domain.usecase

import com.careercompass.core.domain.error.CoreDataFailure
import com.careercompass.core.domain.testing.FakeBoardRepository
import com.careercompass.core.model.board.BoardRegistration
import com.careercompass.core.model.board.BoardType
import com.careercompass.core.model.board.MAX_BOARDS
import com.careercompass.feature.feed.domain.board
import com.careercompass.feature.feed.domain.error.FeedFailure
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.net.UnknownHostException

class RegisterBoardUseCaseTest {
    private val registration =
        BoardRegistration(url = " Konkuk.ac.kr/board/notice ", name = "건국대 공지", type = BoardType.Scholarship)

    @Test
    fun `상한 아래면 정규화한 URL 로 등록한다`() =
        runTest {
            val repository = FakeBoardRepository(initial = List(MAX_BOARDS - 1) { board(id = it + 1L) })

            val registered = RegisterBoardUseCase(repository)(registration).getOrThrow()

            assertEquals("https://konkuk.ac.kr/board/notice", registered.url)
            assertEquals("건국대 공지", registered.name)
            assertEquals(registration.copy(url = "https://konkuk.ac.kr/board/notice"), repository.registrations.single())
            assertEquals(MAX_BOARDS, repository.boards.size)
        }

    @Test
    fun `게시판이 20개면 요청 없이 BoardLimitReached 로 거절한다`() =
        runTest {
            val repository = FakeBoardRepository(initial = List(MAX_BOARDS) { board(id = it + 1L) })

            val failure = RegisterBoardUseCase(repository)(registration).exceptionOrNull()

            assertTrue(failure is FeedFailure.BoardLimitReached)
            assertEquals(MAX_BOARDS, (failure as FeedFailure.BoardLimitReached).limit)
            assertTrue(repository.registrations.isEmpty())
        }

    @Test
    fun `목록 조회가 실패하면 등록하지 않고 그 실패를 돌려준다`() =
        runTest {
            val repository =
                FakeBoardRepository.strict().apply {
                    onGetBoards = { Result.failure(CoreDataFailure.NetworkUnavailable(UnknownHostException())) }
                }

            val outcome = RegisterBoardUseCase(repository)(registration)

            assertTrue(outcome.exceptionOrNull() is CoreDataFailure.NetworkUnavailable)
            assertTrue(repository.registrations.isEmpty())
        }

    @Test
    fun `잘못된 URL 은 목록 조회 전에 거절한다`() =
        runTest {
            val repository = FakeBoardRepository.strict()

            val outcome = RegisterBoardUseCase(repository)(registration.copy(url = "ftp://konkuk.ac.kr"))

            assertTrue(outcome.exceptionOrNull() is FeedFailure.InvalidBoardUrl)
        }
}
