package com.careercompass.feature.feed.domain.usecase

import com.careercompass.core.domain.error.CoreDataFailure
import com.careercompass.core.domain.testing.FakeBoardRepository
import com.careercompass.core.model.board.BoardType
import com.careercompass.core.model.board.BoardUpdate
import com.careercompass.feature.feed.domain.board
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import java.net.UnknownHostException

class BoardManagementUseCasesTest {
    private val boards = listOf(board(id = 1), board(id = 2))

    @Test
    fun `GetBoards 는 목록을 그대로 돌려준다`() =
        runTest {
            assertEquals(Result.success(boards), GetBoardsUseCase(FakeBoardRepository(initial = boards))())
        }

    @Test
    fun `GetBoards 실패는 그대로 흐른다`() =
        runTest {
            val repository =
                FakeBoardRepository.strict().apply {
                    onGetBoards = { Result.failure(CoreDataFailure.Unauthorized("AUTH_REQUIRED", RuntimeException())) }
                }

            assertTrue(GetBoardsUseCase(repository)().exceptionOrNull() is CoreDataFailure.Unauthorized)
        }

    @Test
    fun `UpdateBoard 는 부분 수정을 그대로 요청한다`() =
        runTest {
            val repository = FakeBoardRepository(initial = boards)
            val update = BoardUpdate(name = "새 이름", type = BoardType.Contest, cycleHours = 12)

            val updated = UpdateBoardUseCase(repository)(2, update).getOrThrow()

            assertEquals(board(id = 2).copy(name = "새 이름", type = BoardType.Contest, cycleHours = 12), updated)
            assertEquals(listOf(2L to update), repository.updates.toList())
        }

    @Test
    fun `UpdateBoard 는 빈 수정을 요청 전에 거부한다`() {
        val useCase = UpdateBoardUseCase(FakeBoardRepository.strict())

        assertThrows(IllegalArgumentException::class.java) {
            runBlocking { useCase(1, BoardUpdate()) }
        }
    }

    @Test
    fun `ToggleBoardActive 는 isActive 만 담아 수정한다`() =
        runTest {
            val repository = FakeBoardRepository(initial = boards)

            val paused = ToggleBoardActiveUseCase(repository)(1, isActive = false).getOrThrow()

            assertFalse(paused.isActive)
            assertEquals(listOf(1L to BoardUpdate(isActive = false)), repository.updates.toList())
        }

    @Test
    fun `DeleteBoard 는 삭제를 위임한다`() =
        runTest {
            val repository = FakeBoardRepository(initial = boards)

            assertEquals(Result.success(Unit), DeleteBoardUseCase(repository)(1))
            assertEquals(listOf(board(id = 2)), repository.boards.toList())
        }

    @Test
    fun `RetryBoard 는 재시도를 위임한다`() =
        runTest {
            val repository = FakeBoardRepository(initial = boards)

            assertEquals(Result.success(Unit), RetryBoardUseCase(repository)(2))
            assertEquals(listOf(2L), repository.retries.toList())
        }

    @Test
    fun `위임한 요청의 실패는 그대로 돌려준다`() =
        runTest {
            val repository =
                FakeBoardRepository.strict().apply {
                    onDelete = { Result.failure(CoreDataFailure.NetworkUnavailable(UnknownHostException())) }
                    onRetry = { Result.failure(CoreDataFailure.BoardBlocked("BOARD_BLOCKED", RuntimeException())) }
                }

            assertTrue(DeleteBoardUseCase(repository)(1).exceptionOrNull() is CoreDataFailure.NetworkUnavailable)
            assertTrue(RetryBoardUseCase(repository)(1).exceptionOrNull() is CoreDataFailure.BoardBlocked)
        }
}
