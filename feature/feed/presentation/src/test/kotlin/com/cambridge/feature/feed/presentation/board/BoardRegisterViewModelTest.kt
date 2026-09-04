package com.cambridge.feature.feed.presentation.board

import androidx.lifecycle.SavedStateHandle
import com.cambridge.feature.feed.domain.usecase.DetectBoardUseCase
import com.cambridge.feature.feed.domain.usecase.RegisterBoardUseCase
import com.cambridge.feature.feed.presentation.MainDispatcherRule
import com.cambridge.feature.feed.presentation.RecordingErrorReporter
import com.cambridge.feature.feed.presentation.board
import com.careercompass.core.domain.error.CoreDataFailure
import com.careercompass.core.domain.testing.FakeBoardRepository
import com.careercompass.core.model.board.Board
import com.careercompass.core.model.board.BoardDetection
import com.careercompass.core.model.board.BoardDetectionStatus
import com.careercompass.core.model.board.MAX_BOARDS
import kotlinx.coroutines.CompletableDeferred
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import com.careercompass.core.model.board.BoardType as DomainBoardType

class BoardRegisterViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val reporter = RecordingErrorReporter()

    private fun viewModel(
        repository: FakeBoardRepository = FakeBoardRepository(),
        savedStateHandle: SavedStateHandle = SavedStateHandle(),
    ): BoardRegisterViewModel =
        BoardRegisterViewModel(
            detectBoard = DetectBoardUseCase(repository),
            registerBoard = RegisterBoardUseCase(repository),
            errorReporter = reporter,
            savedStateHandle = savedStateHandle,
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
        // 일시적 전송 실패는 (원인, 단계) 조합의 세션 첫 건만 표본으로 남는다.
        assertEquals(listOf("board_detect"), reporter.stages)
    }

    @Test
    fun `감지 타임아웃은 감지 실패와 다른 상태로 화면에 남는다`() {
        val repository =
            FakeBoardRepository.strict().apply {
                onDetect = { Result.failure(CoreDataFailure.NetworkUnavailable(SocketTimeoutException("timeout"))) }
            }
        val viewModel = viewModel(repository)

        viewModel.onEvent(BoardRegisterEvent.UrlChanged("https://slow.example.ac.kr/board"))
        viewModel.onEvent(BoardRegisterEvent.DetectClicked)

        val state = viewModel.state.value
        // 서버가 알린 감지 실패(Failed)도, 연결 단절 스낵바도 아니다 — 셋이 화면에서 갈려야 한다.
        assertEquals(BoardDetectionState.TimedOut, state.detection)
        assertNull(state.message)
        assertNull(state.urlError)
        // 같은 URL 로 다시 시도할 수 있어야 한다.
        assertTrue(state.isDetectEnabled)
        assertEquals(listOf("board_detect"), reporter.stages)
    }

    @Test
    fun `서버가 알린 감지 실패는 사유 그대로 남고 타임아웃 상태가 되지 않는다`() {
        val repository =
            FakeBoardRepository(
                detection = BoardDetection(status = BoardDetectionStatus.Failed, preview = emptyList(), hasDateSelector = false),
            )
        val viewModel = viewModel(repository)

        viewModel.onEvent(BoardRegisterEvent.UrlChanged("https://konkuk.ac.kr/board/notice"))
        viewModel.onEvent(BoardRegisterEvent.DetectClicked)

        assertEquals(BoardDetectionState.Failed(BoardDetectionFailure.Failed), viewModel.state.value.detection)
        assertNull(viewModel.state.value.message)
    }

    /**
     * 503 은 **감지 결과가 아니라 요청 자체의 실패**다. 「구조를 분석하지 못했어요」로 접히면 사용자가 멀쩡한
     * 자기 게시판 URL 을 의심하며 비싼 크롤링 재시도를 되풀이한다(#212 · #134 와 같은 유형의 오해).
     */
    @Test
    fun `서버 점검은 감지 실패가 아니라 점검 상태로 화면에 남는다`() {
        val repository =
            FakeBoardRepository.strict().apply {
                onDetect = { Result.failure(CoreDataFailure.ServiceUnavailable("LLM_UNAVAILABLE", RuntimeException())) }
            }
        val viewModel = viewModel(repository)

        viewModel.onEvent(BoardRegisterEvent.UrlChanged("https://konkuk.ac.kr/board/notice"))
        viewModel.onEvent(BoardRegisterEvent.DetectClicked)

        val state = viewModel.state.value
        assertEquals(BoardDetectionState.Maintenance, state.detection)
        // 감지 실패 스낵바로도, 서버가 알린 감지 결과(Failed)로도 새지 않는다.
        assertNull(state.message)
        assertNull(state.urlError)
        // 막다른 길은 아니다 — 「구조 분석하기」는 그대로 눌린다.
        assertTrue(state.isDetectEnabled)
    }

    @Test
    fun `사유를 모르는 감지 실패만 스낵바 한 줄로 접힌다`() {
        val repository =
            FakeBoardRepository.strict().apply {
                onDetect = { Result.failure(CoreDataFailure.ServerError("INTERNAL_ERROR", RuntimeException())) }
            }
        val viewModel = viewModel(repository)

        viewModel.onEvent(BoardRegisterEvent.UrlChanged("https://konkuk.ac.kr/board/notice"))
        viewModel.onEvent(BoardRegisterEvent.DetectClicked)

        assertEquals(BoardDetectionState.Idle, viewModel.state.value.detection)
        assertEquals(BoardRegisterMessage.DetectFailed, viewModel.state.value.message)
    }

    /** 제출은 감지와 달리 폼이 그대로 살아 있어 스낵바로 알린다 — 다만 「등록하지 못했어요」와 갈라야 한다. */
    @Test
    fun `등록 제출의 서버 점검은 일반 등록 실패와 다른 안내로 갈린다`() {
        val repository =
            FakeBoardRepository().apply {
                onRegister = { Result.failure(CoreDataFailure.ServiceUnavailable("LLM_UNAVAILABLE", RuntimeException())) }
            }
        val viewModel = viewModel(repository).readyToRegister()

        viewModel.onEvent(BoardRegisterEvent.RegisterClicked)

        val state = viewModel.state.value
        assertEquals(BoardRegisterMessage.Maintenance, state.message)
        assertFalse(state.isSubmitting)
        // 감지 결과와 폼은 그대로다 — 서버가 돌아오면 그 자리에서 다시 누르면 된다.
        assertTrue(state.detection is BoardDetectionState.Success)
        assertNull(state.urlError)
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
    fun `중복 게시판은 URL 오류와 함께 스낵바로도 알린다`() {
        val repository =
            FakeBoardRepository().apply {
                onRegister = { Result.failure(CoreDataFailure.DuplicateBoard("DUPLICATE_BOARD", RuntimeException())) }
            }
        val viewModel = viewModel(repository).readyToRegister()

        viewModel.onEvent(BoardRegisterEvent.RegisterClicked)

        assertEquals(BoardUrlError.Duplicate, viewModel.state.value.urlError)
        // URL 입력란은 폼 위쪽이라 등록 버튼을 누른 자리에서는 화면 밖이다. 안내가 눈앞에도 닿아야 한다.
        assertEquals(BoardRegisterMessage.AlreadyRegistered, viewModel.state.value.message)
        assertFalse(viewModel.state.value.isSubmitting)
        assertFalse(viewModel.state.value.isBackRequested)
        assertEquals(listOf("board_register"), reporter.stages)
    }

    @Test
    fun `제출 중 뒤로가기는 화면을 벗어나지 않고 이유를 알린다`() {
        val gate = CompletableDeferred<Result<Board>>()
        val repository = FakeBoardRepository().apply { onRegister = { gate.await() } }
        val viewModel = viewModel(repository).readyToRegister()

        viewModel.onEvent(BoardRegisterEvent.RegisterClicked)
        assertTrue(viewModel.state.value.isSubmitting)

        viewModel.onEvent(BoardRegisterEvent.BackClicked)

        assertFalse(viewModel.state.value.isBackRequested)
        assertEquals(BoardRegisterMessage.SubmitInProgress, viewModel.state.value.message)
        // 요청은 살아 있다 — 나가지 않았으므로 끊길 이유가 없다.
        assertTrue(viewModel.state.value.isSubmitting)
        assertEquals(1, repository.registrations.size)
    }

    @Test
    fun `제출이 끝나면 뒤로가기가 다시 통한다`() {
        val gate = CompletableDeferred<Result<Board>>()
        val repository = FakeBoardRepository().apply { onRegister = { gate.await() } }
        val viewModel = viewModel(repository).readyToRegister()

        viewModel.onEvent(BoardRegisterEvent.RegisterClicked)
        gate.complete(Result.failure(CoreDataFailure.NetworkUnavailable(UnknownHostException())))

        assertFalse(viewModel.state.value.isSubmitting)

        viewModel.onEvent(BoardRegisterEvent.BackClicked)

        assertTrue(viewModel.state.value.isBackRequested)
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
