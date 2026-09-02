package com.cambridge.feature.feed.presentation.feed

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cambridge.core.common.reporting.ErrorReporter
import com.cambridge.core.domain.error.CoreDataFailure
import com.cambridge.core.domain.repository.UserProfileRepository
import com.cambridge.core.model.posting.Posting
import com.cambridge.feature.feed.domain.model.FeedPage
import com.cambridge.feature.feed.domain.model.FeedQuery
import com.cambridge.feature.feed.domain.usecase.CountTodayNewPostingsUseCase
import com.cambridge.feature.feed.domain.usecase.GetBoardsUseCase
import com.cambridge.feature.feed.domain.usecase.GetFeedPageUseCase
import com.cambridge.feature.feed.domain.usecase.TogglePostingBookmarkUseCase
import com.cambridge.feature.feed.presentation.FeedUiEvent
import com.cambridge.feature.feed.presentation.feedfilter.FeedFilterEvent
import com.cambridge.feature.feed.presentation.feedfilter.FeedSortMenuEvent
import com.cambridge.feature.feed.presentation.reporting.FeedFailureStage
import com.cambridge.feature.feed.presentation.reporting.recordFeedFailure
import com.cambridge.feature.feed.presentation.shared.util.toDomainDeadlineFilter
import com.cambridge.feature.feed.presentation.shared.util.toMinScore
import com.cambridge.feature.feed.presentation.shared.util.toPostingSort
import com.cambridge.feature.feed.presentation.shared.util.toPostingTypes
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Clock
import javax.inject.Inject

/**
 * 피드 홈 — 조회 조건·페이징·북마크를 다루고, 표시 문구는 만들지 않는다.
 *
 * - 검색어는 입력 즉시 상태에 반영하고 [SEARCH_DEBOUNCE_MS] 뒤에 재조회한다.
 * - 페이지가 비어도 `nextCursor` 가 남아 있으면 끝이 아니다([FeedPage]) — 항목이 나올 때까지 몇 페이지를 이어 읽는다.
 * - 북마크는 먼저 뒤집고 실패하면 되돌린다.
 * - `401` 은 [FeedViewState.sessionEnded] 로 올리고, 네트워크 단절은 [FeedLoadState.Failed] 로 구분한다.
 */
@HiltViewModel
public class FeedViewModel
    @Inject
    constructor(
        private val getFeedPage: GetFeedPageUseCase,
        private val countTodayNewPostings: CountTodayNewPostingsUseCase,
        private val togglePostingBookmark: TogglePostingBookmarkUseCase,
        private val getBoards: GetBoardsUseCase,
        private val userProfileRepository: UserProfileRepository,
        private val errorReporter: ErrorReporter,
        /** Entry 가 D-day·신규 판정에 같은 시계를 쓴다. */
        public val clock: Clock,
    ) : ViewModel() {
        private val _state = MutableStateFlow(FeedViewState())
        public val state: StateFlow<FeedViewState> = _state.asStateFlow()

        private var loadJob: Job? = null
        private var loadMoreJob: Job? = null
        private var searchJob: Job? = null

        init {
            viewModelScope.launch {
                userProfileRepository.profile.collect { profile ->
                    _state.update { it.copy(userName = profile?.name) }
                }
            }
            loadBoards()
            loadTodayCount()
            load()
        }

        public fun onEvent(event: FeedUiEvent) {
            when (event) {
                is FeedUiEvent.SearchQueryChanged -> {
                    onSearchInput(event.query)
                }

                is FeedUiEvent.FilterSelected -> {
                    applyQuery(_state.value.query.copy(types = event.category.toPostingTypes()))
                }

                FeedUiEvent.FilterRequested -> {
                    _state.update { it.copy(filterDraft = FeedFilterDraft.from(it.query)) }
                }

                FeedUiEvent.SortMenuRequested -> {
                    _state.update { it.copy(isSortMenuVisible = true) }
                }

                is FeedUiEvent.ListingSelected -> {
                    val postingId = event.listingId.toLongOrNull() ?: return
                    _state.update { it.copy(pendingNavigation = FeedDestination.PostingDetail(postingId)) }
                }

                is FeedUiEvent.BookmarkToggled -> {
                    toggleBookmark(event.listingId.toLongOrNull() ?: return)
                }

                FeedUiEvent.NotificationsSelected -> {
                    _state.update { it.copy(pendingNavigation = FeedDestination.Notifications) }
                }
            }
        }

        public fun onFilterEvent(event: FeedFilterEvent) {
            when (event) {
                is FeedFilterEvent.CategorySelected -> {
                    updateDraft { it.copy(category = event.category) }
                }

                is FeedFilterEvent.BoardToggled -> {
                    val boardId = event.boardId.toLongOrNull() ?: return
                    updateDraft { draft ->
                        draft.copy(boardIds = if (boardId in draft.boardIds) draft.boardIds - boardId else draft.boardIds + boardId)
                    }
                }

                is FeedFilterEvent.DeadlineSelected -> {
                    updateDraft { it.copy(deadline = event.deadline.toDomainDeadlineFilter()) }
                }

                is FeedFilterEvent.MinScoreSelected -> {
                    updateDraft { it.copy(minScore = event.minScore.toMinScore()) }
                }

                FeedFilterEvent.UnreadOnlyToggled -> {
                    updateDraft { it.copy(unreadOnly = !it.unreadOnly) }
                }

                FeedFilterEvent.ResetClicked -> {
                    updateDraft { FeedFilterDraft.Default }
                }

                FeedFilterEvent.ApplyClicked -> {
                    val draft = _state.value.filterDraft ?: return
                    _state.update { it.copy(filterDraft = null) }
                    applyQuery(draft.applyTo(_state.value.query))
                }

                FeedFilterEvent.DismissClicked -> {
                    _state.update { it.copy(filterDraft = null) }
                }
            }
        }

        public fun onSortEvent(event: FeedSortMenuEvent) {
            when (event) {
                is FeedSortMenuEvent.SortSelected -> {
                    _state.update { it.copy(isSortMenuVisible = false) }
                    applyQuery(_state.value.query.copy(sort = event.option.toPostingSort()))
                }

                FeedSortMenuEvent.DismissClicked -> {
                    _state.update { it.copy(isSortMenuVisible = false) }
                }
            }
        }

        /** 목록 끝에 닿았다. 다음 커서가 없거나 이미 읽는 중이면 무시한다. */
        public fun onLoadMore() {
            val current = _state.value
            val cursor = current.nextCursor ?: return
            if (current.isLoadingMore || current.isRefreshing || current.loadState != FeedLoadState.Loaded) return
            val query = current.query
            _state.update { it.copy(isLoadingMore = true) }
            loadMoreJob =
                viewModelScope.launch {
                    fetchUntilNonEmpty(query, cursor)
                        .onSuccess { page ->
                            _state.update {
                                it.copy(
                                    postings = (it.postings + page.postings).distinctBy(Posting::id),
                                    nextCursor = page.nextCursor,
                                    isLoadingMore = false,
                                )
                            }
                        }.onFailure { throwable ->
                            recordFailure(FeedFailureStage.FeedLoadMore, throwable)
                            _state.update { it.copy(isLoadingMore = false, message = FeedMessage.LoadMoreFailed) }
                        }
                }
        }

        /** 당겨서 새로고침 — 목록은 유지한 채 첫 페이지와 오늘 신규 개수를 다시 받는다. */
        public fun refresh() {
            if (_state.value.loadState == FeedLoadState.Loading) return
            loadJob?.cancel()
            loadMoreJob?.cancel()
            val query = _state.value.query
            _state.update { it.copy(isRefreshing = true, isLoadingMore = false) }
            loadTodayCount()
            loadJob =
                viewModelScope.launch {
                    fetchUntilNonEmpty(query, cursor = null)
                        .onSuccess { page ->
                            _state.update {
                                it.copy(
                                    postings = page.postings,
                                    nextCursor = page.nextCursor,
                                    loadState = FeedLoadState.Loaded,
                                    isRefreshing = false,
                                )
                            }
                        }.onFailure { throwable ->
                            recordFailure(FeedFailureStage.FeedRefresh, throwable)
                            _state.update {
                                if (it.postings.isEmpty()) {
                                    it.copy(loadState = FeedLoadState.Failed(throwable.isNetworkUnavailable()), isRefreshing = false)
                                } else {
                                    it.copy(isRefreshing = false, message = FeedMessage.RefreshFailed)
                                }
                            }
                        }
                }
        }

        /** 오류 화면의 「다시 시도」 — 처음부터 다시 읽는다. */
        public fun retry() {
            load()
        }

        public fun onBoardListRequested() {
            _state.update { it.copy(pendingNavigation = FeedDestination.BoardList) }
        }

        public fun onBoardRegisterRequested() {
            _state.update { it.copy(pendingNavigation = FeedDestination.BoardRegister) }
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

        private fun onSearchInput(input: String) {
            _state.update { it.copy(searchInput = input) }
            searchJob?.cancel()
            searchJob =
                viewModelScope.launch {
                    delay(SEARCH_DEBOUNCE_MS)
                    applyQuery(_state.value.query.copy(searchQuery = input))
                }
        }

        private fun updateDraft(transform: (FeedFilterDraft) -> FeedFilterDraft) {
            _state.update { state -> state.filterDraft?.let { state.copy(filterDraft = transform(it)) } ?: state }
        }

        private fun applyQuery(query: FeedQuery) {
            if (query == _state.value.query) return
            _state.update { it.copy(query = query) }
            load()
        }

        private fun load() {
            loadJob?.cancel()
            loadMoreJob?.cancel()
            val query = _state.value.query
            _state.update {
                it.copy(
                    loadState = FeedLoadState.Loading,
                    postings = emptyList(),
                    nextCursor = null,
                    isRefreshing = false,
                    isLoadingMore = false,
                )
            }
            loadJob =
                viewModelScope.launch {
                    fetchUntilNonEmpty(query, cursor = null)
                        .onSuccess { page ->
                            _state.update {
                                it.copy(
                                    postings = page.postings,
                                    nextCursor = page.nextCursor,
                                    loadState = FeedLoadState.Loaded,
                                )
                            }
                        }.onFailure { throwable ->
                            recordFailure(FeedFailureStage.FeedLoad, throwable)
                            _state.update { it.copy(loadState = FeedLoadState.Failed(throwable.isNetworkUnavailable())) }
                        }
                }
        }

        /**
         * 클라이언트 필터로 페이지가 통째로 비면 다음 커서를 따라 최대 [MAX_EMPTY_PAGE_FOLLOW_UPS]페이지까지 이어 읽는다.
         * 그래도 비면 받은 커서를 그대로 돌려줘 호출자가 끝으로 오판하지 않게 한다.
         */
        private suspend fun fetchUntilNonEmpty(
            query: FeedQuery,
            cursor: String?,
        ): Result<FeedPage> {
            val collected = mutableListOf<Posting>()
            var next = cursor
            repeat(MAX_EMPTY_PAGE_FOLLOW_UPS) {
                val page = getFeedPage(query, next).getOrElse { return Result.failure(it) }
                collected += page.postings
                next = page.nextCursor
                if (collected.isNotEmpty() || !page.hasNext) {
                    return Result.success(FeedPage(postings = collected.toList(), nextCursor = next))
                }
            }
            return Result.success(FeedPage(postings = collected.toList(), nextCursor = next))
        }

        private fun toggleBookmark(postingId: Long) {
            val before = _state.value.postings.firstOrNull { it.id == postingId } ?: return
            replacePosting(before.copy(isBookmarked = !before.isBookmarked))
            viewModelScope.launch {
                togglePostingBookmark(postingId, currentlyBookmarked = before.isBookmarked)
                    .onSuccess { bookmarked -> replacePosting(before.copy(isBookmarked = bookmarked)) }
                    .onFailure { throwable ->
                        recordFailure(FeedFailureStage.Bookmark, throwable)
                        replacePosting(before)
                        _state.update { it.copy(message = FeedMessage.BookmarkFailed) }
                    }
            }
        }

        private fun replacePosting(posting: Posting) {
            _state.update { state ->
                state.copy(postings = state.postings.map { if (it.id == posting.id) posting else it })
            }
        }

        private fun loadBoards() {
            viewModelScope.launch {
                getBoards()
                    .onSuccess { boards -> _state.update { it.copy(boards = boards) } }
                    .onFailure { recordFailure(FeedFailureStage.FilterBoards, it) }
            }
        }

        private fun loadTodayCount() {
            viewModelScope.launch {
                countTodayNewPostings()
                    .onSuccess { count -> _state.update { it.copy(todayNewCount = count) } }
                    .onFailure { recordFailure(FeedFailureStage.TodayCount, it) }
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

        private fun Throwable.isNetworkUnavailable(): Boolean = this is CoreDataFailure.NetworkUnavailable

        public companion object {
            /** 검색어 입력 뒤 재조회까지 기다리는 시간. */
            public const val SEARCH_DEBOUNCE_MS: Long = 300L

            /** 빈 페이지가 이어질 때 따라가는 페이지 수 상한 — 기본 페이지 크기 기준 100건. */
            public const val MAX_EMPTY_PAGE_FOLLOW_UPS: Int = 5
        }
    }
