package com.cambridge.feature.feed.presentation.postingdetail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cambridge.core.common.reporting.ErrorReporter
import com.cambridge.core.domain.error.CoreDataFailure
import com.cambridge.core.domain.repository.UserProfileRepository
import com.cambridge.core.model.posting.PostingDetail
import com.cambridge.core.model.user.UserProfile
import com.cambridge.feature.feed.domain.usecase.OpenPostingDetailUseCase
import com.cambridge.feature.feed.domain.usecase.TogglePostingBookmarkUseCase
import com.cambridge.feature.feed.presentation.navigation.FEED_ARG_POSTING_ID
import com.cambridge.feature.feed.presentation.reporting.FeedFailureStage
import com.cambridge.feature.feed.presentation.reporting.recordFeedFailure
import com.cambridge.feature.feed.presentation.shared.model.FeedFailureReason
import com.cambridge.feature.feed.presentation.shared.model.SuitabilityJudgement
import com.cambridge.feature.feed.presentation.shared.model.judgeSuitability
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

public sealed interface PostingDetailLoadState {
    public data object Loading : PostingDetailLoadState

    public data class Loaded(
        val detail: PostingDetail,
    ) : PostingDetailLoadState

    public data class Failed(
        val reason: FeedFailureReason,
    ) : PostingDetailLoadState
}

public sealed interface PostingDetailDestination {
    public data object Back : PostingDetailDestination

    public data class Raw(
        val postingId: Long,
    ) : PostingDetailDestination

    public data class Posting(
        val postingId: Long,
    ) : PostingDetailDestination

    /** 앱 셸이 마이 탭(프로필 입력)으로 보낸다. */
    public data object Profile : PostingDetailDestination
}

public enum class PostingDetailMessage {
    BookmarkFailed,

    /** editor 모듈이 아직 연결되지 않았다. */
    DraftComingSoon,
}

/** 공유 시트에 실을 내용. Entry 가 `Intent.ACTION_SEND` 로 바꾼다. */
public data class PostingShareRequest(
    val title: String,
    val url: String,
)

public data class PostingDetailViewState(
    val postingId: Long,
    val loadState: PostingDetailLoadState = PostingDetailLoadState.Loading,
    val profile: UserProfile? = null,
    val pendingNavigation: PostingDetailDestination? = null,
    val shareRequest: PostingShareRequest? = null,
    val message: PostingDetailMessage? = null,
    val sessionEnded: Boolean = false,
) {
    public val detail: PostingDetail? get() = (loadState as? PostingDetailLoadState.Loaded)?.detail

    public val suitabilityJudgement: SuitabilityJudgement?
        get() = detail?.let { judgeSuitability(hasScore = it.suitability != null, profile = profile) }
}

/**
 * 공고 상세 — 열면서 읽음 처리하고([OpenPostingDetailUseCase]), 북마크는 먼저 뒤집고 실패하면 되돌린다.
 */
@HiltViewModel
public class PostingDetailViewModel
    @Inject
    constructor(
        savedStateHandle: SavedStateHandle,
        private val openPostingDetail: OpenPostingDetailUseCase,
        private val togglePostingBookmark: TogglePostingBookmarkUseCase,
        private val userProfileRepository: UserProfileRepository,
        private val errorReporter: ErrorReporter,
        /** Entry 가 상대 시각·마감 표기에 같은 시계를 쓴다. */
        public val clock: Clock,
    ) : ViewModel() {
        private val postingId: Long =
            requireNotNull(savedStateHandle.get<Long>(FEED_ARG_POSTING_ID)) { "$FEED_ARG_POSTING_ID is required" }

        private val _state = MutableStateFlow(PostingDetailViewState(postingId = postingId))
        public val state: StateFlow<PostingDetailViewState> = _state.asStateFlow()

        private var loadJob: Job? = null

        init {
            viewModelScope.launch {
                userProfileRepository.profile.collect { profile -> _state.update { it.copy(profile = profile) } }
            }
            load()
        }

        public fun onEvent(event: PostingDetailEvent) {
            when (event) {
                PostingDetailEvent.BackClicked -> {
                    navigate(PostingDetailDestination.Back)
                }

                PostingDetailEvent.BookmarkToggled -> {
                    toggleBookmark()
                }

                PostingDetailEvent.ShareClicked -> {
                    val detail = _state.value.detail ?: return
                    _state.update { it.copy(shareRequest = PostingShareRequest(title = detail.title, url = detail.url)) }
                }

                PostingDetailEvent.ViewOriginalClicked -> {
                    navigate(PostingDetailDestination.Raw(postingId))
                }

                PostingDetailEvent.CreateDraftClicked -> {
                    _state.update { it.copy(message = PostingDetailMessage.DraftComingSoon) }
                }

                PostingDetailEvent.CompleteProfileClicked -> {
                    navigate(PostingDetailDestination.Profile)
                }

                is PostingDetailEvent.SimilarPostingSelected -> {
                    val similarId = event.listingId.toLongOrNull() ?: return
                    navigate(PostingDetailDestination.Posting(similarId))
                }

                PostingDetailEvent.RetryClicked -> {
                    load()
                }
            }
        }

        public fun onNavigationConsumed() {
            _state.update { it.copy(pendingNavigation = null) }
        }

        public fun onShareConsumed() {
            _state.update { it.copy(shareRequest = null) }
        }

        public fun onMessageConsumed() {
            _state.update { it.copy(message = null) }
        }

        public fun onSessionEndedConsumed() {
            _state.update { it.copy(sessionEnded = false) }
        }

        private fun navigate(destination: PostingDetailDestination) {
            _state.update { it.copy(pendingNavigation = destination) }
        }

        private fun load() {
            loadJob?.cancel()
            _state.update { it.copy(loadState = PostingDetailLoadState.Loading) }
            loadJob =
                viewModelScope.launch {
                    openPostingDetail(postingId)
                        .onSuccess { detail -> _state.update { it.copy(loadState = PostingDetailLoadState.Loaded(detail)) } }
                        .onFailure { throwable ->
                            recordFailure(FeedFailureStage.PostingDetail, throwable)
                            _state.update {
                                it.copy(loadState = PostingDetailLoadState.Failed(throwable.toFeedFailureReason()))
                            }
                        }
                }
        }

        private fun toggleBookmark() {
            val before = _state.value.detail ?: return
            replaceDetail(before.copy(isBookmarked = !before.isBookmarked))
            viewModelScope.launch {
                togglePostingBookmark(postingId, currentlyBookmarked = before.isBookmarked)
                    .onSuccess { bookmarked -> replaceDetail(before.copy(isBookmarked = bookmarked)) }
                    .onFailure { throwable ->
                        recordFailure(FeedFailureStage.Bookmark, throwable)
                        replaceDetail(before)
                        _state.update { it.copy(message = PostingDetailMessage.BookmarkFailed) }
                    }
            }
        }

        private fun replaceDetail(detail: PostingDetail) {
            _state.update { state ->
                if (state.loadState is PostingDetailLoadState.Loaded) {
                    state.copy(
                        loadState = PostingDetailLoadState.Loaded(detail),
                    )
                } else {
                    state
                }
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
