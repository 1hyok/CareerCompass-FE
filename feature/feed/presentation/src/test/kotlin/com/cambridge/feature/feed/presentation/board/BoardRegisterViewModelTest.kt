package com.cambridge.feature.feed.presentation.board

import com.cambridge.core.domain.error.CoreDataFailure
import com.cambridge.core.domain.testing.FakeBoardRepository
import com.cambridge.core.model.board.BoardDetection
import com.cambridge.core.model.board.BoardDetectionStatus
import com.cambridge.core.model.board.MAX_BOARDS
import com.cambridge.feature.feed.domain.usecase.DetectBoardUseCase
import com.cambridge.feature.feed.domain.usecase.RegisterBoardUseCase
import com.cambridge.feature.feed.presentation.MainDispatcherRule
import com.cambridge.feature.feed.presentation.RecordingErrorReporter
import com.cambridge.feature.feed.presentation.board
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import java.net.UnknownHostException
import com.cambridge.core.model.board.BoardType as DomainBoardType

class BoardRegisterViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val reporter = RecordingErrorReporter()

    private fun viewModel(repository: FakeBoardRepository = FakeBoardRepository()): BoardRegisterViewModel =
        BoardRegisterViewModel(
            detectBoard = DetectBoardUseCase(repository),
            registerBoard = RegisterBoardUseCase(repository),
            errorReporter = reporter,
        )

    @Test
    fun `형태가 잘못된 URL 은 요청 없이 입력 오류로 표시한다`() {
        val repository = FakeBoardRepository.strict()
        val viewModel = viewModel(repository)

        viewModel.onEvent(BoardRegisterEvent.UrlChanged("ftp://konkuk.ac.kr"))
        viewModel.onEvent(BoardRegisterEvent.DetectClicked)

        val state = viewModel.state.value
        assertEquals(BoardUrlError.Invalid, state.urlError)
        assertEquals(BoardDetectionState.Idle, state.detection)
        assertTrue(repository.detectedUrls.isEmpty())
        assertTrue(reporter.stages.isEmpty())
    }

    @Test
    fun `감지 성공은 정규화 URL 과 미리보기를 기억한다`() {
        val repository = FakeBoardRepository()
        val viewModel = viewModel(repository)

        viewModel.onEvent(BoardRegisterEvent.UrlChanged("  Konkuk.ac.kr/board/notice "))
        viewModel.onEvent(BoardRegisterEvent.DetectClicked)

        val state = viewModel.state.value
        val detection = state.detection as BoardDetectionState.Success
        assertEquals(1, detection.preview.size)
        assertTrue(detection.dateDetected)
        assertEquals("https://konkuk.ac.kr/board/notice", state.detectedUrl)
        assertNull(state.urlError)
        assertFalse(state.isRegisterEnabled)
    }

    @Test
    fun `등록 불가 상태는 실패 사유로 보이고 URL 을 바꾸면 감지가 무효가 된다`() {
        val repository =
            FakeBoardRepository(
                detection = BoardDetection(status = BoardDetectionStatus.LoginRequired, preview = emptyList(), hasDateSelector = false),
            )
        val viewModel = viewModel(repository)

        viewModel.onEvent(BoardRegisterEvent.UrlChanged("https://intra.example.com"))
        viewModel.onEvent(BoardRegisterEvent.DetectClicked)
        assertEquals(BoardDetectionState.Failed(BoardDetectionFailure.LoginRequired), viewModel.state.value.detection)
        assertNull(viewModel.state.value.detectedUrl)

        viewModel.onEvent(BoardRegisterEvent.UrlChanged("https://intra.example.com/other"))

        assertEquals(BoardDetectionState.Idle, viewModel.state.value.detection)
    }

    @Test
    fun `네트워크 단절은 감지를 되돌리고 스낵바로 알린다`() {
        val repository =
            FakeBoardRepository.strict().apply {
                onDetect = { Result.failure(CoreDataFailure.NetworkUnavailable(UnknownHostException())) }
            }
        val viewModel = viewModel(repository)

        viewModel.onEvent(BoardRegisterEvent.UrlChanged("https://konkuk.ac.kr"))
        viewModel.onEvent(BoardRegisterEvent.DetectClicked)

        assertEquals(BoardDetectionState.Idle, viewModel.state.value.detection)
        assertEquals(BoardRegisterMessage.NetworkUnavailable, viewModel.state.value.message)
        assertEquals(listOf("board_detect"), reporter.stages)
    }

    @Test
    fun `등록 성공은 감지에 쓴 URL 과 입력값으로 요청하고 목록으로 돌아간다`() {
        val repository = FakeBoardRepository()
        val viewModel = viewModel(repository).readyToRegister()

        viewModel.onEvent(BoardRegisterEvent.RegisterClicked)

        val registration = repository.registrations.single()
        assertEquals("https://konkuk.ac.kr/board/notice", registration.url)
        assertEquals("건국대 공지", registration.name)
        assertEquals(DomainBoardType.Recruit, registration.type)
        assertEquals(BoardCollectCycle.Weekly.hours, registration.cycleHours)
        assertTrue(viewModel.state.value.isBackRequested)
        assertFalse(viewModel.state.value.isSubmitting)
    }

    @Test
    fun `중복 게시판은 URL 오류로 표시한다`() {
        val repository =
            FakeBoardRepository().apply {
                onRegister = { Result.failure(CoreDataFailure.DuplicateBoard("DUPLICATE_BOARD", RuntimeException())) }
            }
        val viewModel = viewModel(repository).readyToRegister()

        viewModel.onEvent(BoardRegisterEvent.RegisterClicked)

        assertEquals(BoardUrlError.Duplicate, viewModel.state.value.urlError)
        assertFalse(viewModel.state.value.isSubmitting)
        assertFalse(viewModel.state.value.isBackRequested)
        assertEquals(listOf("board_register"), reporter.stages)
    }

    @Test
    fun `게시판이 상한이면 요청 없이 상한 문구를 띄운다`() {
        val repository = FakeBoardRepository(initial = List(MAX_BOARDS) { board(id = it + 1L) })
        val viewModel = viewModel(repository).readyToRegister()

        viewModel.onEvent(BoardRegisterEvent.RegisterClicked)

        assertEquals(BoardRegisterMessage.LimitReached(MAX_BOARDS), viewModel.state.value.message)
        assertTrue(repository.registrations.isEmpty())
        assertFalse(viewModel.state.value.isSubmitting)
    }

    @Test
    fun `등록 중 401 은 세션 종료 신호를 올린다`() {
        val repository =
            FakeBoardRepository().apply {
                onRegister = { Result.failure(CoreDataFailure.Unauthorized("AUTH_REQUIRED", RuntimeException())) }
            }
        val viewModel = viewModel(repository).readyToRegister()

        viewModel.onEvent(BoardRegisterEvent.RegisterClicked)

        assertTrue(viewModel.state.value.sessionEnded)
    }

    private fun BoardRegisterViewModel.readyToRegister(): BoardRegisterViewModel =
        apply {
            onEvent(BoardRegisterEvent.UrlChanged("Konkuk.ac.kr/board/notice"))
            onEvent(BoardRegisterEvent.DetectClicked)
            onEvent(BoardRegisterEvent.NameChanged(" 건국대 공지 "))
            onEvent(BoardRegisterEvent.TypeSelected(BoardType.Employment))
            onEvent(BoardRegisterEvent.CycleSelected(BoardCollectCycle.Weekly))
            check(state.value.isRegisterEnabled)
        }
}
