package com.cambridge.feature.feed.presentation.board

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cambridge.core.common.reporting.ErrorReporter
import com.cambridge.core.domain.error.CoreDataFailure
import com.cambridge.core.model.board.BoardRegistration
import com.cambridge.core.model.board.MAX_BOARDS
import com.cambridge.feature.feed.domain.error.FeedFailure
import com.cambridge.feature.feed.domain.usecase.DetectBoardUseCase
import com.cambridge.feature.feed.domain.usecase.RegisterBoardUseCase
import com.cambridge.feature.feed.presentation.reporting.FeedFailureStage
import com.cambridge.feature.feed.presentation.reporting.recordFeedFailure
import com.cambridge.feature.feed.presentation.shared.util.toDetectionState
import com.cambridge.feature.feed.presentation.shared.util.toDomainBoardType
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/** URL 입력란 아래 오류. 문구는 Entry 가 만든다. */
public enum class BoardUrlError {
    Invalid,
    Duplicate,
}

public sealed interface BoardRegisterMessage {
    public data object NetworkUnavailable : BoardRegisterMessage

    public data object DetectFailed : BoardRegisterMessage

    public data object RegisterFailed : BoardRegisterMessage

    public data class LimitReached(
        val limit: Int,
    ) : BoardRegisterMessage
}

public data class BoardRegisterViewState(
    val url: String = "",
    val urlError: BoardUrlError? = null,
    val detection: BoardDetectionState = BoardDetectionState.Idle,
    /** 감지에 실제로 쓴 정규화 URL — 등록 때 같은 값을 보낸다. 감지가 없거나 실패하면 null. */
    val detectedUrl: String? = null,
    val name: String = "",
    val type: BoardType? = null,
    val cycle: BoardCollectCycle = BoardCollectCycle.Daily,
    val isSubmitting: Boolean = false,
    val message: BoardRegisterMessage? = null,
    val isBackRequested: Boolean = false,
    val sessionEnded: Boolean = false,
) {
    public val isDetectEnabled: Boolean
        get() = url.isNotBlank() && detection != BoardDetectionState.Detecting && !isSubmitting

    public val isRegisterEnabled: Boolean
        get() = detection is BoardDetectionState.Success && name.isNotBlank() && type != null && !isSubmitting
}

/**
 * 게시판 등록 — URL → 구조 감지 → 이름·유형·주기 → 등록(기능 스펙 F2-1).
 *
 * URL 이 바뀌면 이전 감지 결과는 무효다. 등록 성공은 [BoardRegisterViewState.isBackRequested] 로 목록에 돌아간다.
 */
@HiltViewModel
public class BoardRegisterViewModel
    @Inject
    constructor(
        private val detectBoard: DetectBoardUseCase,
        private val registerBoard: RegisterBoardUseCase,
        private val errorReporter: ErrorReporter,
    ) : ViewModel() {
        private val _state = MutableStateFlow(BoardRegisterViewState())
        public val state: StateFlow<BoardRegisterViewState> = _state.asStateFlow()

        private var detectJob: Job? = null

        public fun onEvent(event: BoardRegisterEvent) {
            when (event) {
                is BoardRegisterEvent.UrlChanged -> {
                    detectJob?.cancel()
                    _state.update {
                        it.copy(url = event.value, urlError = null, detection = BoardDetectionState.Idle, detectedUrl = null)
                    }
                }

                BoardRegisterEvent.DetectClicked -> {
                    detect()
                }

                is BoardRegisterEvent.NameChanged -> {
                    _state.update { it.copy(name = event.value) }
                }

                is BoardRegisterEvent.TypeSelected -> {
                    _state.update { it.copy(type = event.type) }
                }

                is BoardRegisterEvent.CycleSelected -> {
                    _state.update { it.copy(cycle = event.cycle) }
                }

                BoardRegisterEvent.RegisterClicked -> {
                    register()
                }

                BoardRegisterEvent.BackClicked -> {
                    _state.update { it.copy(isBackRequested = true) }
                }
            }
        }

        public fun onBackConsumed() {
            _state.update { it.copy(isBackRequested = false) }
        }

        public fun onMessageConsumed() {
            _state.update { it.copy(message = null) }
        }

        public fun onSessionEndedConsumed() {
            _state.update { it.copy(sessionEnded = false) }
        }

        private fun detect() {
            val current = _state.value
            if (!current.isDetectEnabled) return
            detectJob?.cancel()
            _state.update { it.copy(detection = BoardDetectionState.Detecting, urlError = null, detectedUrl = null) }
            detectJob =
                viewModelScope.launch {
                    detectBoard(current.url)
                        .onSuccess { outcome ->
                            _state.update {
                                it.copy(
                                    detection = outcome.detection.toDetectionState(),
                                    detectedUrl = outcome.url.takeIf { outcome.detection.isRegistrable },
                                )
                            }
                        }.onFailure { throwable -> onDetectFailed(throwable) }
                }
        }

        private fun onDetectFailed(throwable: Throwable) {
            when (throwable) {
                // 사용자 입력 형태 오류 — 요청 없이 끝났으므로 리포팅 대상이 아니다.
                is FeedFailure.InvalidBoardUrl -> {
                    _state.update { it.copy(detection = BoardDetectionState.Idle, urlError = BoardUrlError.Invalid) }
                }

                is CoreDataFailure.BoardBlocked -> {
                    errorReporter.recordFeedFailure(FeedFailureStage.BoardDetect, throwable)
                    _state.update { it.copy(detection = BoardDetectionState.Failed(BoardDetectionFailure.Blocked)) }
                }

                else -> {
                    errorReporter.recordFeedFailure(FeedFailureStage.BoardDetect, throwable)
                    _state.update {
                        it.copy(
                            detection = BoardDetectionState.Idle,
                            message =
                                if (throwable is CoreDataFailure.NetworkUnavailable) {
                                    BoardRegisterMessage.NetworkUnavailable
                                } else {
                                    BoardRegisterMessage.DetectFailed
                                },
                            sessionEnded = it.sessionEnded || throwable is CoreDataFailure.Unauthorized,
                        )
                    }
                }
            }
        }

        private fun register() {
            val current = _state.value
            val type = current.type ?: return
            if (!current.isRegisterEnabled) return
            val registration =
                BoardRegistration(
                    url = current.detectedUrl ?: current.url,
                    name = current.name.trim(),
                    type = type.toDomainBoardType(),
                    cycleHours = current.cycle.hours,
                )
            _state.update { it.copy(isSubmitting = true) }
            viewModelScope.launch {
                registerBoard(registration)
                    .onSuccess { _state.update { it.copy(isSubmitting = false, isBackRequested = true) } }
                    .onFailure { throwable -> onRegisterFailed(throwable) }
            }
        }

        private fun onRegisterFailed(throwable: Throwable) {
            if (throwable !is FeedFailure.InvalidBoardUrl) {
                errorReporter.recordFeedFailure(FeedFailureStage.BoardRegister, throwable)
            }
            _state.update { state ->
                val cleared = state.copy(isSubmitting = false)
                when (throwable) {
                    is FeedFailure.InvalidBoardUrl -> cleared.copy(urlError = BoardUrlError.Invalid)
                    is CoreDataFailure.DuplicateBoard -> cleared.copy(urlError = BoardUrlError.Duplicate)
                    is FeedFailure.BoardLimitReached -> cleared.copy(message = BoardRegisterMessage.LimitReached(throwable.limit))
                    is CoreDataFailure.LimitExceeded -> cleared.copy(message = BoardRegisterMessage.LimitReached(MAX_BOARDS))
                    is CoreDataFailure.NetworkUnavailable -> cleared.copy(message = BoardRegisterMessage.NetworkUnavailable)
                    is CoreDataFailure.Unauthorized -> cleared.copy(sessionEnded = true)
                    else -> cleared.copy(message = BoardRegisterMessage.RegisterFailed)
                }
            }
        }
    }
