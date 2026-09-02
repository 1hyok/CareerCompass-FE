package com.cambridge.feature.feed.presentation.board

import com.cambridge.core.domain.error.CoreDataFailure
import com.cambridge.core.domain.testing.FakeBoardRepository
import com.cambridge.core.model.board.BoardUpdate
import com.cambridge.feature.feed.domain.usecase.DeleteBoardUseCase
import com.cambridge.feature.feed.domain.usecase.GetBoardsUseCase
import com.cambridge.feature.feed.domain.usecase.RetryBoardUseCase
import com.cambridge.feature.feed.domain.usecase.ToggleBoardActiveUseCase
import com.cambridge.feature.feed.presentation.FIXED_CLOCK
import com.cambridge.feature.feed.presentation.MainDispatcherRule
import com.cambridge.feature.feed.presentation.RecordingErrorReporter
import com.cambridge.feature.feed.presentation.board
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import java.net.UnknownHostException
import com.cambridge.core.model.board.BoardStatus as DomainBoardStatus

@OptIn(ExperimentalCoroutinesApi::class)
class BoardListViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val reporter = RecordingErrorReporter()

    private fun viewModel(repository: FakeBoardRepository): BoardListViewModel =
        BoardListViewModel(
            getBoards = GetBoardsUseCase(repository),
            toggleBoardActive = ToggleBoardActiveUseCase(repository),
            retryBoard = RetryBoardUseCase(repository),
            deleteBoard = DeleteBoardUseCase(repository),
            errorReporter = reporter,
            clock = FIXED_CLOCK,
        )

    private fun repository() =
        FakeBoardRepository(
            initial =
                listOf(
                    board(id = 1),
                    board(id = 2, isActive = false, status = DomainBoardStatus.Failed, failCount = 3),
                ),
        )

    @Test
    fun `목록을 읽어 온다`() {
        val state = viewModel(repository()).state.value

        assertEquals(listOf(1L, 2L), state.boards.map { it.id })
    }

    @Test
    fun `수집 토글은 isActive 만 담아 보내고 서버 값으로 확정한다`() {
        val repository = repository()
        val viewModel = viewModel(repository)

        viewModel.onEvent(BoardListEvent.BoardToggled("1"))

        assertFalse(
            viewModel.state.value.boards
                .first { it.id == 1L }
                .isActive,
        )
        assertEquals(listOf(1L to BoardUpdate(isActive = false)), repository.updates.toList())
        assertNull(viewModel.state.value.message)
    }

    @Test
    fun `토글은 먼저 뒤집고 실패하면 되돌린다`() =
        runTest(mainDispatcherRule.dispatcher) {
            val gate = CompletableDeferred<Unit>()
            val repository =
                repository().apply {
                    onUpdate = { _, _ ->
                        gate.await()
                        Result.failure(CoreDataFailure.NetworkUnavailable(UnknownHostException()))
                    }
                }
            val viewModel = viewModel(repository)

            viewModel.onEvent(BoardListEvent.BoardToggled("1"))
            val optimistic =
                viewModel.state.value.boards
                    .first { it.id == 1L }
            assertFalse(optimistic.isActive)
            assertEquals(DomainBoardStatus.Paused, optimistic.status)

            gate.complete(Unit)

            val reverted =
                viewModel.state.value.boards
                    .first { it.id == 1L }
            assertTrue(reverted.isActive)
            assertEquals(DomainBoardStatus.Active, reverted.status)
            assertEquals(BoardListMessage.ToggleFailed, viewModel.state.value.message)
            assertEquals(listOf("board_toggle"), reporter.stages)
        }

    @Test
    fun `재시도는 위임하고 성공하면 실패 상태를 지운다`() {
        val repository = repository()
        val viewModel = viewModel(repository)

        viewModel.onEvent(BoardListEvent.RetryClicked("2"))

        val retried =
            viewModel.state.value.boards
                .first { it.id == 2L }
        assertEquals(listOf(2L), repository.retries.toList())
        assertEquals(DomainBoardStatus.Active, retried.status)
        assertEquals(0, retried.failCount)
        assertEquals(BoardListMessage.RetryRequested, viewModel.state.value.message)
    }

    @Test
    fun `재시도 실패는 스낵바로 알린다`() {
        val repository =
            repository().apply {
                onRetry =
                    { Result.failure(CoreDataFailure.BoardBlocked("BOARD_BLOCKED", RuntimeException())) }
            }
        val viewModel = viewModel(repository)

        viewModel.onEvent(BoardListEvent.RetryClicked("2"))

        assertEquals(BoardListMessage.RetryFailed, viewModel.state.value.message)
        assertEquals(
            DomainBoardStatus.Failed,
            viewModel.state.value.boards
                .first { it.id == 2L }
                .status,
        )
    }

    @Test
    fun `삭제는 확인 다이얼로그를 거쳐야 보낸다`() {
        val repository = repository()
        val viewModel = viewModel(repository)

        viewModel.onEvent(BoardListEvent.DeleteClicked("1"))
        assertEquals(
            1L,
            viewModel.state.value.pendingDeletion
                ?.id,
        )
        viewModel.dismissDelete()
        assertNull(viewModel.state.value.pendingDeletion)
        assertEquals(2, repository.boards.size)

        viewModel.onEvent(BoardListEvent.DeleteClicked("1"))
        viewModel.confirmDelete()

        assertNull(viewModel.state.value.pendingDeletion)
        assertEquals(
            listOf(2L),
            viewModel.state.value.boards
                .map { it.id },
        )
        assertEquals(listOf(2L), repository.boards.map { it.id })
    }

    @Test
    fun `삭제 실패는 목록을 유지하고 스낵바로 알린다`() {
        val repository =
            repository().apply {
                onDelete =
                    { Result.failure(CoreDataFailure.ServerError("INTERNAL_ERROR", RuntimeException())) }
            }
        val viewModel = viewModel(repository)

        viewModel.onEvent(BoardListEvent.DeleteClicked("1"))
        viewModel.confirmDelete()

        assertEquals(
            listOf(1L, 2L),
            viewModel.state.value.boards
                .map { it.id },
        )
        assertEquals(BoardListMessage.DeleteFailed, viewModel.state.value.message)
    }

    @Test
    fun `추가·뒤로가기는 단발 신호로 올라간다`() {
        val viewModel = viewModel(repository())

        viewModel.onEvent(BoardListEvent.AddBoardClicked)
        assertEquals(BoardListDestination.Register, viewModel.state.value.pendingNavigation)
        viewModel.onNavigationConsumed()
        viewModel.onEvent(BoardListEvent.BackClicked)
        assertEquals(BoardListDestination.Back, viewModel.state.value.pendingNavigation)
    }

    @Test
    fun `목록 조회 실패는 네트워크 여부를 구분하고 401 은 세션을 끝낸다`() {
        val network =
            FakeBoardRepository.strict().apply {
                onGetBoards =
                    { Result.failure(CoreDataFailure.NetworkUnavailable(UnknownHostException())) }
            }
        assertEquals(BoardListLoadState.Failed(isNetworkUnavailable = true), viewModel(network).state.value.loadState)

        val unauthorized =
            FakeBoardRepository.strict().apply {
                onGetBoards = { Result.failure(CoreDataFailure.Unauthorized("AUTH_REQUIRED", RuntimeException())) }
            }
        val state = viewModel(unauthorized).state.value
        assertEquals(BoardListLoadState.Failed(isNetworkUnavailable = false), state.loadState)
        assertTrue(state.sessionEnded)
    }

    @Test
    fun `재진입 새로고침은 새 목록을 반영하고 로딩 상태로 되돌리지 않는다`() {
        val repository = repository()
        val viewModel = viewModel(repository)
        repository.boards += board(id = 3)

        viewModel.refresh()

        assertEquals(
            listOf(1L, 2L, 3L),
            viewModel.state.value.boards
                .map { it.id },
        )
    }
}
