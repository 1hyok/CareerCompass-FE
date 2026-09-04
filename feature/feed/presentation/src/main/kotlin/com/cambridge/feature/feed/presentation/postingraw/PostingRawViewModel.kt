package com.cambridge.feature.feed.presentation.postingraw

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cambridge.core.common.reporting.ErrorReporter
import com.cambridge.core.domain.error.CoreDataFailure
import com.cambridge.core.model.posting.PostingDetail
import com.cambridge.feature.feed.domain.usecase.OpenPostingDetailUseCase
import com.cambridge.feature.feed.presentation.navigation.FEED_ARG_POSTING_ID
import com.cambridge.feature.feed.presentation.reporting.FeedFailureStage
import com.cambridge.feature.feed.presentation.reporting.recordFeedFailure
import com.cambridge.feature.feed.presentation.shared.model.FeedFailureReason
import com.cambridge.feature.feed.presentation.shared.model.toFeedFailureReason
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Clock
import javax.inject.Inject

public sealed interface PostingRawLoadState {
    public data object Loading : PostingRawLoadState

    public data class Loaded(
        val detail: PostingDetail,
    ) : PostingRawLoadState

    /**
     * 실패를 **사유**로 들고 있는다 — 피드 홈·게시판 목록·공고 상세가 이미 쓰는 계약([FeedFailureReason])이다.
     *
     * 예전에는 `isNetworkUnavailable` 불리언 한 칸이었고, 그래서 503(서버 점검)이 「네트워크 아님」 쪽으로
     * 떨어져 일반 실패로 접혔다. 점검 중에 상세에서 「원문 보기」를 누르면 바로 앞 화면은 「서비스가 잠시
     * 점검 중이에요」라고 해 놓고 이 화면만 「원문을 불러오지 못했어요」라고 말하던 원인이다(#212).
     */
    public data class Failed(
        val reason: FeedFailureReason,
    ) : PostingRawLoadState
}

public data class PostingRawViewState(
    val postingId: Long,
    val loadState: PostingRawLoadState = PostingRawLoadState.Loading,
    val isBackRequested: Boolean = false,
    /** 외부 브라우저로 열 원본 링크. Entry 가 `Intent.ACTION_VIEW` 로 바꾸고 [PostingRawViewModel.onOpenUrlConsumed] 로 비운다. */
    val openUrl: String? = null,
    val sessionEnded: Boolean = false,
)

/** 원문 보기 — 상세와 같은 use case 로 본문·원본 링크를 다시 받는다(이미 읽음 처리된 공고라 추가 요청은 없다). */
@HiltViewModel
public class PostingRawViewModel
    @Inject
    constructor(
        savedStateHandle: SavedStateHandle,
        private val openPostingDetail: OpenPostingDetailUseCase,
        private val errorReporter: ErrorReporter,
        /** Entry 가 수집 시각 표기에 같은 시계를 쓴다. */
        public val clock: Clock,
    ) : ViewModel() {
        private val postingId: Long =
            requireNotNull(savedStateHandle.get<Long>(FEED_ARG_POSTING_ID)) { "$FEED_ARG_POSTING_ID is required" }

        private val _state = MutableStateFlow(PostingRawViewState(postingId = postingId))
        public val state: StateFlow<PostingRawViewState> = _state.asStateFlow()

        private var loadJob: Job? = null

        init {
            load()
        }

        public fun onEvent(event: PostingRawEvent) {
            when (event) {
                PostingRawEvent.BackClicked -> {
                    _state.update { it.copy(isBackRequested = true) }
                }

                PostingRawEvent.OpenOriginalClicked -> {
                    val detail = (_state.value.loadState as? PostingRawLoadState.Loaded)?.detail ?: return
                    _state.update { it.copy(openUrl = detail.url) }
                }
            }
        }

        public fun retry() {
            load()
        }

        public fun onBackConsumed() {
            _state.update { it.copy(isBackRequested = false) }
        }

        public fun onOpenUrlConsumed() {
            _state.update { it.copy(openUrl = null) }
        }

        public fun onSessionEndedConsumed() {
            _state.update { it.copy(sessionEnded = false) }
        }

        private fun load() {
            loadJob?.cancel()
            _state.update { it.copy(loadState = PostingRawLoadState.Loading) }
            loadJob =
                viewModelScope.launch {
                    openPostingDetail(postingId)
                        .onSuccess { detail -> _state.update { it.copy(loadState = PostingRawLoadState.Loaded(detail)) } }
                        .onFailure { throwable ->
                            errorReporter.recordFeedFailure(FeedFailureStage.PostingRaw, throwable)
                            _state.update {
                                it.copy(
                                    loadState = PostingRawLoadState.Failed(throwable.toFeedFailureReason()),
                                    sessionEnded = it.sessionEnded || throwable is CoreDataFailure.Unauthorized,
                                )
                            }
                        }
                }
        }
    }
