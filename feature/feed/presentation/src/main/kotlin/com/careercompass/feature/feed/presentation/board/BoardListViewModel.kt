package com.careercompass.feature.feed.presentation.board

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.careercompass.core.common.reporting.ErrorReporter
import com.careercompass.core.domain.error.CoreDataFailure
import com.careercompass.core.model.board.Board
import com.careercompass.core.model.board.BoardUpdate
import com.careercompass.feature.feed.domain.usecase.DeleteBoardUseCase
import com.careercompass.feature.feed.domain.usecase.GetBoardsUseCase
import com.careercompass.feature.feed.domain.usecase.RetryBoardUseCase
import com.careercompass.feature.feed.domain.usecase.ToggleBoardActiveUseCase
import com.careercompass.feature.feed.domain.usecase.UpdateBoardUseCase
import com.careercompass.feature.feed.presentation.reporting.FeedFailureStage
import com.careercompass.feature.feed.presentation.reporting.recordFeedFailure
import com.careercompass.feature.feed.presentation.shared.model.FeedFailureReason
import com.careercompass.feature.feed.presentation.shared.model.toFeedFailureReason
import com.careercompass.feature.feed.presentation.shared.util.toCollectCycle
import com.careercompass.feature.feed.presentation.shared.util.toDomainBoardType
import com.careercompass.feature.feed.presentation.shared.util.toUiBoardType
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Clock
import javax.inject.Inject
import com.careercompass.core.model.board.BoardStatus as DomainBoardStatus

public sealed interface BoardListLoadState {
    public data object Loading : BoardListLoadState

    public data class Loaded(
        val boards: List<Board>,
    ) : BoardListLoadState

    public data class Failed(
        val reason: FeedFailureReason,
    ) : BoardListLoadState
}

public sealed interface BoardListDestination {
    public data object Back : BoardListDestination

    public data object Register : BoardListDestination
}

public enum class BoardListMessage {
    ToggleFailed,
    RetryFailed,
    RetryRequested,
    DeleteFailed,
    Updated,
    UpdateFailed,
}

/**
 * 수정 시트가 편집 중인 값 — 「저장」 전까지 [board] 에 반영되지 않는다.
 *
 * URL 은 서버가 감지 결과와 묶어 두므로 바꿀 수 없다. [toUpdate] 는 원본과 다른 필드만 담아
 * `PATCH /boards/{id}` 가 바뀐 것만 실어 나가게 한다.
 */
public data class BoardEditDraft(
    val board: Board,
    val name: String,
    val type: BoardType,
    val cycle: BoardCollectCycle,
    val isSaving: Boolean = false,
) {
    /** 원본과 다른 필드만 채운 부분 수정. 이름은 trim 해 비교하고, 비어 있으면 바뀐 것으로 보지 않는다. */
    public fun toUpdate(): BoardUpdate {
        val trimmedName = name.trim()
        return BoardUpdate(
            name = trimmedName.takeIf { it.isNotEmpty() && it != board.name },
            type = type.toDomainBoardType().takeIf { it != board.type },
            cycleHours = cycle.hours.takeIf { it != board.cycleHours },
        )
    }

    public companion object {
        public fun from(board: Board): BoardEditDraft =
            BoardEditDraft(
                board = board,
                name = board.name,
                type = board.type.toUiBoardType(),
                cycle = board.cycleHours.toCollectCycle(),
            )
    }
}

public data class BoardListViewState(
    val loadState: BoardListLoadState = BoardListLoadState.Loading,
    /** 삭제 확인 다이얼로그가 가리키는 게시판. null 이면 닫힘. */
    val pendingDeletion: Board? = null,
    /** 수정 시트가 편집 중인 게시판. null 이면 닫힘. */
    val editDraft: BoardEditDraft? = null,
    val pendingNavigation: BoardListDestination? = null,
    val message: BoardListMessage? = null,
    val sessionEnded: Boolean = false,
) {
    public val boards: List<Board> get() = (loadState as? BoardListLoadState.Loaded)?.boards.orEmpty()
}

/**
 * 내 게시판 목록 — 수집 ON/OFF 는 먼저 뒤집고 실패하면 되돌리며, 삭제는 확인 뒤에만 보낸다.
 * 카드를 누르면 수정 시트가 열리고, 저장은 바뀐 필드만 `PATCH` 로 보낸다.
 *
 * 등록 화면에서 돌아오면 Entry 가 [refresh] 를 부른다.
 *
 * 수정 시트에서 고치던 값은 [BoardEditInputDraft] 가 [SavedStateHandle] 에 남긴다(#156). 서버에 이미 있는
 * 게시판을 고치는 자리라 **필드 단위로 서버가 이긴다** — 남기는 것은 사용자가 실제로 바꾼 필드뿐이고, 시트를
 * 다시 열 때 바탕은 그 순간 목록에 있는 서버 값이다. 왜 그렇게 정했는지(그리고 왜 「충돌을 알아채 물어보기」가
 * 불가능한지)는 그 클래스의 KDoc 에 있다.
 */
@HiltViewModel
public class BoardListViewModel
    @Inject
    constructor(
        private val getBoards: GetBoardsUseCase,
        private val toggleBoardActive: ToggleBoardActiveUseCase,
        private val retryBoard: RetryBoardUseCase,
        private val deleteBoard: DeleteBoardUseCase,
        private val updateBoard: UpdateBoardUseCase,
        private val errorReporter: ErrorReporter,
        /** Entry 가 마지막 수집 상대 시각에 같은 시계를 쓴다. */
        public val clock: Clock,
        savedStateHandle: SavedStateHandle,
    ) : ViewModel() {
        private val editInputDraft = BoardEditInputDraft(savedStateHandle)

        private val _state = MutableStateFlow(BoardListViewState())
        public val state: StateFlow<BoardListViewState> = _state.asStateFlow()

        private var loadJob: Job? = null

        init {
            load(showLoading = true)
            // 시트가 값을 바꾸는 자리마다 저장하지 않고 상태 흐름 한 곳에서 남긴다 — 각자 저장하게 두면 언젠가
            // 한 곳이 빠지고, 빠진 자리는 프로세스가 죽어야 드러난다. 갱신은 시트가 **열려 있는 동안만** 한다:
            // 닫힘(null)까지 여기서 받으면 살아난 직후 시트가 닫힌 상태라는 이유로 방금 복원한 초안을 지운다.
            viewModelScope.launch {
                _state.map { it.editDraft }.distinctUntilChanged().collect { draft -> draft?.let(editInputDraft::save) }
            }
        }

        public fun onEvent(event: BoardListEvent) {
            when (event) {
                BoardListEvent.AddBoardClicked -> {
                    _state.update { it.copy(pendingNavigation = BoardListDestination.Register) }
                }

                is BoardListEvent.BoardToggled -> {
                    toggle(event.boardId.toLongOrNull() ?: return)
                }

                is BoardListEvent.RetryClicked -> {
                    retry(event.boardId.toLongOrNull() ?: return)
                }

                is BoardListEvent.DeleteClicked -> {
                    val boardId = event.boardId.toLongOrNull() ?: return
                    val board = _state.value.boards.firstOrNull { it.id == boardId } ?: return
                    _state.update { it.copy(pendingDeletion = board) }
                }

                is BoardListEvent.BoardSelected -> {
                    val boardId = event.boardId.toLongOrNull() ?: return
                    val board = _state.value.boards.firstOrNull { it.id == boardId } ?: return
                    // 바탕은 언제나 방금 읽어 온 서버 값이고, 살아남은 초안은 사용자가 바꾼 필드만 그 위에 덮는다.
                    val base = BoardEditDraft.from(board)
                    val restored = editInputDraft.restoredEdit()?.applyTo(base) ?: base
                    _state.update { it.copy(editDraft = restored) }
                }

                BoardListEvent.BackClicked -> {
                    _state.update { it.copy(pendingNavigation = BoardListDestination.Back) }
                }
            }
        }

        /** 수정 시트 이벤트. 저장 중에는 닫기를 무시해 응답이 시트 없는 화면에 떨어지지 않게 한다. */
        public fun onEditEvent(event: BoardEditEvent) {
            when (event) {
                is BoardEditEvent.NameChanged -> {
                    updateDraft { it.copy(name = event.value) }
                }

                is BoardEditEvent.TypeSelected -> {
                    updateDraft { it.copy(type = event.type) }
                }

                is BoardEditEvent.CycleSelected -> {
                    updateDraft { it.copy(cycle = event.cycle) }
                }

                BoardEditEvent.SaveClicked -> {
                    save()
                }

                BoardEditEvent.DismissClicked -> {
                    if (_state.value.editDraft?.isSaving == true) return
                    closeEditSheet()
                }
            }
        }

        /** 화면 재진입 시 목록을 다시 읽는다. 첫 로드가 진행 중이면 겹치지 않는다. */
        public fun refresh() {
            if (loadJob?.isActive == true) return
            load(showLoading = _state.value.loadState !is BoardListLoadState.Loaded)
        }

        public fun retryLoad() {
            load(showLoading = true)
        }

        public fun confirmDelete() {
            val board = _state.value.pendingDeletion ?: return
            _state.update { it.copy(pendingDeletion = null) }
            viewModelScope.launch {
                deleteBoard(board.id)
                    .onSuccess { updateBoards { boards -> boards.filterNot { it.id == board.id } } }
                    .onFailure { throwable ->
                        recordFailure(FeedFailureStage.BoardDelete, throwable)
                        _state.update { it.copy(message = BoardListMessage.DeleteFailed) }
                    }
            }
        }

        public fun dismissDelete() {
            _state.update { it.copy(pendingDeletion = null) }
        }

        public fun onNavigationConsumed() {
            _state.update { it.copy(pendingNavigation = null) }
        }

        public fun onMessageConsumed() {
            _state.update { it.copy(message = null) }
        }

        public fun onSessionEndedConsumed() {
            _state.update { it.copy(sessionEnded = false) }
        }

        private fun load(showLoading: Boolean) {
            loadJob?.cancel()
            if (showLoading) _state.update { it.copy(loadState = BoardListLoadState.Loading) }
            loadJob =
                viewModelScope.launch {
                    getBoards()
                        .onSuccess { boards -> _state.update { it.copy(loadState = BoardListLoadState.Loaded(boards)) } }
                        .onFailure { throwable ->
                            recordFailure(FeedFailureStage.BoardList, throwable)
                            _state.update { state ->
                                if (state.loadState is BoardListLoadState.Loaded) {
                                    state
                                } else {
                                    state.copy(loadState = BoardListLoadState.Failed(throwable.toFeedFailureReason()))
                                }
                            }
                        }
                }
        }

        private fun toggle(boardId: Long) {
            val before = _state.value.boards.firstOrNull { it.id == boardId } ?: return
            val optimistic =
                before.copy(
                    isActive = !before.isActive,
                    status =
                        when {
                            before.isActive -> DomainBoardStatus.Paused
                            before.status == DomainBoardStatus.Paused -> DomainBoardStatus.Active
                            else -> before.status
                        },
                )
            replaceBoard(optimistic)
            viewModelScope.launch {
                toggleBoardActive(boardId, isActive = optimistic.isActive)
                    .onSuccess { updated -> replaceBoard(updated) }
                    .onFailure { throwable ->
                        recordFailure(FeedFailureStage.BoardToggle, throwable)
                        // 되돌리는 것은 토글이 건드린 두 필드뿐이다 — 요청이 오가는 사이 수정 시트가 저장한
                        // 이름·주기를 옛 스냅샷으로 덮지 않는다(#235).
                        updateBoard(boardId) { it.copy(isActive = before.isActive, status = before.status) }
                        _state.update { it.copy(message = BoardListMessage.ToggleFailed) }
                    }
            }
        }

        private fun retry(boardId: Long) {
            val board = _state.value.boards.firstOrNull { it.id == boardId } ?: return
            viewModelScope.launch {
                retryBoard(boardId)
                    .onSuccess {
                        replaceBoard(board.copy(status = DomainBoardStatus.Active, failCount = 0, isActive = true))
                        _state.update { it.copy(message = BoardListMessage.RetryRequested) }
                    }.onFailure { throwable ->
                        recordFailure(FeedFailureStage.BoardRetry, throwable)
                        _state.update { it.copy(message = BoardListMessage.RetryFailed) }
                    }
            }
        }

        /** 바뀐 필드만 보낸다. 바뀐 게 없으면 요청 없이 닫고, 실패하면 시트를 유지한 채 알린다. */
        private fun save() {
            val draft = _state.value.editDraft ?: return
            if (draft.isSaving || draft.name.isBlank()) return
            val update = draft.toUpdate()
            if (update.isEmpty) {
                closeEditSheet()
                return
            }
            updateDraft { it.copy(isSaving = true) }
            viewModelScope.launch {
                updateBoard(draft.board.id, update)
                    .onSuccess { updated ->
                        replaceBoard(updated)
                        editInputDraft.clear()
                        _state.update { it.copy(editDraft = null, message = BoardListMessage.Updated) }
                    }.onFailure { throwable ->
                        recordFailure(FeedFailureStage.BoardUpdate, throwable)
                        updateDraft { it.copy(isSaving = false) }
                        _state.update { it.copy(message = BoardListMessage.UpdateFailed) }
                    }
            }
        }

        /** 시트를 닫으면서 초안도 버린다 — 닫히는 이유(취소·보낼 것 없음)가 모두 편집을 버리는 쪽이다. */
        private fun closeEditSheet() {
            editInputDraft.clear()
            _state.update { it.copy(editDraft = null) }
        }

        private fun updateDraft(transform: (BoardEditDraft) -> BoardEditDraft) {
            _state.update { state -> state.editDraft?.let { state.copy(editDraft = transform(it)) } ?: state }
        }

        private fun replaceBoard(board: Board) {
            updateBoards { boards -> boards.map { if (it.id == board.id) board else it } }
        }

        private fun updateBoard(
            boardId: Long,
            transform: (Board) -> Board,
        ) {
            updateBoards { boards -> boards.map { if (it.id == boardId) transform(it) else it } }
        }

        private fun updateBoards(transform: (List<Board>) -> List<Board>) {
            _state.update { state ->
                val loaded = state.loadState as? BoardListLoadState.Loaded ?: return@update state
                state.copy(loadState = BoardListLoadState.Loaded(transform(loaded.boards)))
            }
        }

        private fun recordFailure(
            stage: FeedFailureStage,
            throwable: Throwable,
        ) {
            errorReporter.recordFeedFailure(stage, throwable)
            if (throwable is CoreDataFailure.Unauthorized) {
                _state.update { it.copy(sessionEnded = true) }
            }
        }
    }
