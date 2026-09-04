package com.careercompass.feature.feed.presentation.postingdetail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.careercompass.core.common.reporting.ErrorReporter
import com.careercompass.core.domain.error.CoreDataFailure
import com.careercompass.core.domain.repository.UserProfileRepository
import com.careercompass.core.model.posting.PostingDetail
import com.careercompass.core.model.user.UserProfile
import com.careercompass.feature.feed.domain.usecase.OpenPostingDetailUseCase
import com.careercompass.feature.feed.domain.usecase.TogglePostingBookmarkUseCase
import com.careercompass.feature.feed.presentation.navigation.FEED_ARG_POSTING_ID
import com.careercompass.feature.feed.presentation.reporting.FeedFailureStage
import com.careercompass.feature.feed.presentation.reporting.recordFeedFailure
import com.careercompass.feature.feed.presentation.shared.model.FeedFailureReason
import com.careercompass.feature.feed.presentation.shared.model.SuitabilityJudgement
import com.careercompass.feature.feed.presentation.shared.model.judgeSuitability
import com.careercompass.feature.feed.presentation.shared.model.toFeedFailureReason
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Clock
import javax.inject.Inject
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

/**
 * 적합도 자동 재조회의 간격과 횟수 — 5초 × 4회 = 20초, 그 뒤 멈춘다(#221).
 *
 * 서버가 진행률을 주지 않으므로(엣지 상태 §2.2) 끝을 화면이 정해야 한다. 무한 폴링은 영구 실패 공고에서
 * 영원히 돌며 배터리와 서버를 함께 태운다. 20초는 「금방 끝나는 분석은 기다려 주되, 안 끝나는 분석에 매달리지
 * 않는다」의 경계로 잡은 값이다 — 서버가 진행 상태를 주는 날 이 상수는 사라진다.
 */
public val SUITABILITY_AUTO_RECHECK_INTERVAL: Duration = 5.seconds

/** [SUITABILITY_AUTO_RECHECK_INTERVAL] 참고. */
public const val SUITABILITY_AUTO_RECHECK_LIMIT: Int = 4

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

/**
 * @property isSuitabilityRecheckExhausted 적합도 자동 재조회를 다 썼다(#221). 「분석 중」일 때만 뜻이 있다 —
 *   판정이 바뀌면(점수 도착·프로필 미입력·다시 읽기) 함께 지워진다.
 */
public data class PostingDetailViewState(
    val postingId: Long,
    val loadState: PostingDetailLoadState = PostingDetailLoadState.Loading,
    val profile: UserProfile? = null,
    val isSuitabilityRecheckExhausted: Boolean = false,
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
 *
 * ### 적합도 자동 재조회 (#221)
 * 적합도는 서버가 LLM 으로 계산하므로 공고를 열었을 때 아직 없는 것이 정상 경로다. 판정이 「분석 중」인 동안
 * [SUITABILITY_AUTO_RECHECK_INTERVAL] 마다 [SUITABILITY_AUTO_RECHECK_LIMIT] 번까지 **조용히** 다시 읽는다 —
 * `loadState` 를 `Loading` 으로 되돌리지 않아 화면이 깜빡이지 않는다. 다 쓰면
 * [PostingDetailViewState.isSuitabilityRecheckExhausted] 를 올려 카드가 「다시 확인」을 열고, 그 버튼은 한 번에
 * 한 번만 더 묻는다.
 *
 * 재조회의 시작·정지는 **판정을 따라간다**([SuitabilityJudgement]): 「분석 중」이 되면 시작하고, 그 밖의
 * 판정이 되면 멈추고 소진도 지운다. 그래서 프로필이 늦게 도착해 「프로필 미입력」으로 갈리면 그 자리에서
 * 멈춘다 — 둘을 섞지 않는다는 요구(#100)가 폴링 게이트에서도 지켜진다. 마이 탭에서 프로필을 채우고 돌아와
 * 「분석 중」이 되면 그때 시작한다.
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
        private var recheckJob: Job? = null

        init {
            viewModelScope.launch {
                userProfileRepository.profile.collect { profile -> _state.update { it.copy(profile = profile) } }
            }
            viewModelScope.launch {
                _state
                    .map { it.suitabilityJudgement }
                    .distinctUntilChanged()
                    .collect { judgement ->
                        if (judgement == SuitabilityJudgement.Analyzing) startAutoRecheck() else stopRecheck()
                    }
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

                PostingDetailEvent.SuitabilityRecheckClicked -> {
                    recheckOnDemand()
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

        private fun startAutoRecheck() {
            if (recheckJob?.isActive == true) return
            recheckJob =
                viewModelScope.launch {
                    repeat(SUITABILITY_AUTO_RECHECK_LIMIT) {
                        delay(SUITABILITY_AUTO_RECHECK_INTERVAL)
                        // 기다리는 사이 판정이 갈렸으면(프로필 도착 등) 헛요청을 보내지 않는다.
                        if (!isStillAnalyzing()) return@launch
                        recheckSuitability()
                    }
                    if (isStillAnalyzing()) _state.update { it.copy(isSuitabilityRecheckExhausted = true) }
                }
        }

        private fun stopRecheck() {
            recheckJob?.cancel()
            recheckJob = null
            _state.update { if (it.isSuitabilityRecheckExhausted) it.copy(isSuitabilityRecheckExhausted = false) else it }
        }

        /** 「다시 확인」 — 한 번만 더 묻고, 여전히 분석 중이면 다시 소진 상태로 돌아간다. */
        private fun recheckOnDemand() {
            if (!isStillAnalyzing()) return
            recheckJob?.cancel()
            recheckJob =
                viewModelScope.launch {
                    _state.update { it.copy(isSuitabilityRecheckExhausted = false) }
                    recheckSuitability()
                    if (isStillAnalyzing()) _state.update { it.copy(isSuitabilityRecheckExhausted = true) }
                }
        }

        /**
         * 조용한 재조회 — 성공하면 상세를 갈아 끼우되 `Loaded` 를 유지하고, 실패해도 화면을 흔들지 않는다.
         *
         * 북마크 여부만은 지금 값을 지킨다 — 사용자가 방금 뒤집어 서버 응답을 기다리는 중일 수 있고, 그 결과는
         * [toggleBookmark] 가 확정한다. [OpenPostingDetailUseCase] 는 이미 읽은 공고에 읽음 요청을 다시 보내지
         * 않으므로 재조회가 읽음 요청을 되풀이하지도 않는다.
         */
        private suspend fun recheckSuitability() {
            openPostingDetail(postingId)
                .onSuccess { fresh -> updateDetail { current -> fresh.copy(isBookmarked = current.isBookmarked) } }
                .onFailure { throwable -> recordFailure(FeedFailureStage.SuitabilityRecheck, throwable) }
        }

        private fun isStillAnalyzing(): Boolean = _state.value.suitabilityJudgement == SuitabilityJudgement.Analyzing

        /**
         * 북마크는 먼저 뒤집고, 응답이 오면 **그 필드만** 지금 상세 위에 확정한다.
         *
         * 요청을 보내던 순간의 상세를 통째로 되돌려 놓으면 안 된다(#235) — 응답이 오가는 사이 상세는 바뀔 수
         * 있다. 「분석 중」이면 [recheckSuitability] 가 적합도를 실어 오는데, 옛 스냅샷으로 덮으면 방금 나타난
         * 점수가 사라진다. 그래서 성공은 서버가 준 북마크 값을, 실패는 누르기 전 북마크 값을 **지금 상세**에
         * 얹는다.
         */
        private fun toggleBookmark() {
            val wasBookmarked = _state.value.detail?.isBookmarked ?: return
            updateDetail { it.copy(isBookmarked = !wasBookmarked) }
            viewModelScope.launch {
                togglePostingBookmark(postingId, currentlyBookmarked = wasBookmarked)
                    .onSuccess { bookmarked -> updateDetail { it.copy(isBookmarked = bookmarked) } }
                    .onFailure { throwable ->
                        recordFailure(FeedFailureStage.Bookmark, throwable)
                        updateDetail { it.copy(isBookmarked = wasBookmarked) }
                        _state.update { it.copy(message = PostingDetailMessage.BookmarkFailed) }
                    }
            }
        }

        /** 읽은 상세가 있을 때만 그 위에 [transform] 을 적용한다 — 로딩·실패 중에는 아무것도 하지 않는다. */
        private fun updateDetail(transform: (PostingDetail) -> PostingDetail) {
            _state.update { state ->
                val loaded = state.loadState as? PostingDetailLoadState.Loaded ?: return@update state
                state.copy(loadState = PostingDetailLoadState.Loaded(transform(loaded.detail)))
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
