package com.cambridge.feature.feed.presentation.board

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cambridge.core.common.reporting.ErrorReporter
import com.cambridge.core.domain.error.CoreDataFailure
import com.cambridge.core.model.board.Board
import com.cambridge.core.model.board.BoardUpdate
import com.cambridge.feature.feed.domain.usecase.DeleteBoardUseCase
import com.cambridge.feature.feed.domain.usecase.GetBoardsUseCase
import com.cambridge.feature.feed.domain.usecase.RetryBoardUseCase
import com.cambridge.feature.feed.domain.usecase.ToggleBoardActiveUseCase
import com.cambridge.feature.feed.domain.usecase.UpdateBoardUseCase
import com.cambridge.feature.feed.presentation.reporting.FeedFailureStage
import com.cambridge.feature.feed.presentation.reporting.recordFeedFailure
import com.cambridge.feature.feed.presentation.shared.util.toCollectCycle
import com.cambridge.feature.feed.presentation.shared.util.toDomainBoardType
import com.cambridge.feature.feed.presentation.shared.util.toUiBoardType
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Clock
import javax.inject.Inject
import com.cambridge.core.model.board.BoardStatus as DomainBoardStatus

public sealed interface BoardListLoadState {
    public data object Loading : BoardListLoadState

    public data class Loaded(
        val boards: List<Board>,
    ) : BoardListLoadState

    public data class Failed(
        val isNetworkUnavailable: Boolean,
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
    ) : ViewModel() {
        private val _state = MutableStateFlow(BoardListViewState())
        public val state: StateFlow<BoardListViewState> = _state.asStateFlow()

        private var loadJob: Job? = null

        init {
            load(showLoading = true)
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
                    _state.update { it.copy(editDraft = BoardEditDraft.from(board)) }
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
                    _state.update { state ->
                        if (state.editDraft?.isSaving == true) state else state.copy(editDraft = null)
                    }
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
                                    state.copy(loadState = BoardListLoadState.Failed(throwable is CoreDataFailure.NetworkUnavailable))
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
                        replaceBoard(before)
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
                _state.update { it.copy(editDraft = null) }
                return
            }
            updateDraft { it.copy(isSaving = true) }
            viewModelScope.launch {
                updateBoard(draft.board.id, update)
                    .onSuccess { updated ->
                        replaceBoard(updated)
                        _state.update { it.copy(editDraft = null, message = BoardListMessage.Updated) }
                    }.onFailure { throwable ->
                        recordFailure(FeedFailureStage.BoardUpdate, throwable)
                        updateDraft { it.copy(isSaving = false) }
                        _state.update { it.copy(message = BoardListMessage.UpdateFailed) }
                    }
            }
        }

        private fun updateDraft(transform: (BoardEditDraft) -> BoardEditDraft) {
            _state.update { state -> state.editDraft?.let { state.copy(editDraft = transform(it)) } ?: state }
        }

        private fun replaceBoard(board: Board) {
            updateBoards { boards -> boards.map { if (it.id == board.id) board else it } }
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
