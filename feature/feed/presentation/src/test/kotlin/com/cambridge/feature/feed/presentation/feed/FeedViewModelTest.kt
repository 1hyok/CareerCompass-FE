package com.cambridge.feature.feed.presentation.feed

import com.cambridge.core.domain.error.CoreDataFailure
import com.cambridge.core.domain.testing.FakeBoardRepository
import com.cambridge.core.domain.testing.FakePostingRepository
import com.cambridge.core.domain.testing.FakeUserProfileRepository
import com.cambridge.core.model.paging.CursorPage
import com.cambridge.core.model.posting.Posting
import com.cambridge.core.model.posting.PostingQuery
import com.cambridge.core.model.posting.PostingSort
import com.cambridge.core.model.posting.PostingType
import com.cambridge.feature.feed.domain.model.FeedDeadlineFilter
import com.cambridge.feature.feed.domain.model.FeedSnapshot
import com.cambridge.feature.feed.domain.testing.FakeFeedSnapshotRepository
import com.cambridge.feature.feed.domain.usecase.CountTodayNewPostingsUseCase
import com.cambridge.feature.feed.domain.usecase.GetBoardsUseCase
import com.cambridge.feature.feed.domain.usecase.GetFeedPageUseCase
import com.cambridge.feature.feed.domain.usecase.TogglePostingBookmarkUseCase
import com.cambridge.feature.feed.presentation.FIXED_CLOCK
import com.cambridge.feature.feed.presentation.FeedListingCategory
import com.cambridge.feature.feed.presentation.FeedUiEvent
import com.cambridge.feature.feed.presentation.MainDispatcherRule
import com.cambridge.feature.feed.presentation.RecordingErrorReporter
import com.cambridge.feature.feed.presentation.TODAY
import com.cambridge.feature.feed.presentation.board
import com.cambridge.feature.feed.presentation.feedfilter.FeedDeadlineRange
import com.cambridge.feature.feed.presentation.feedfilter.FeedDeadlineRangeEndpoint
import com.cambridge.feature.feed.presentation.feedfilter.FeedDeadlineRangeError
import com.cambridge.feature.feed.presentation.feedfilter.FeedFilterEvent
import com.cambridge.feature.feed.presentation.feedfilter.FeedMinScoreFilter
import com.cambridge.feature.feed.presentation.feedfilter.FeedSortMenuEvent
import com.cambridge.feature.feed.presentation.feedfilter.FeedSortOption
import com.cambridge.feature.feed.presentation.posting
import com.cambridge.feature.feed.presentation.profile
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import java.net.UnknownHostException
import com.cambridge.feature.feed.presentation.feedfilter.FeedDeadlineFilter as UiDeadlineFilter

@OptIn(ExperimentalCoroutinesApi::class)
class FeedViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val reporter = RecordingErrorReporter()

    private fun viewModel(
        postingRepository: FakePostingRepository = FakePostingRepository(initial = List(3) { posting(id = it + 1L) }),
        boardRepository: FakeBoardRepository = FakeBoardRepository(initial = listOf(board(id = 1), board(id = 2))),
        profileRepository: FakeUserProfileRepository = FakeUserProfileRepository(initialProfile = profile()),
        snapshotRepository: FakeFeedSnapshotRepository = FakeFeedSnapshotRepository(),
    ): FeedViewModel =
        FeedViewModel(
            getFeedPage = GetFeedPageUseCase(postingRepository, FIXED_CLOCK),
            countTodayNewPostings = CountTodayNewPostingsUseCase(postingRepository, FIXED_CLOCK),
            togglePostingBookmark = TogglePostingBookmarkUseCase(postingRepository),
            getBoards = GetBoardsUseCase(boardRepository),
            userProfileRepository = profileRepository,
            feedSnapshotRepository = snapshotRepository,
            errorReporter = reporter,
            clock = FIXED_CLOCK,
        )

    /** 네트워크 단절로 첫 조회가 실패하는 리포지토리. */
    private fun offlinePostings() =
        FakePostingRepository().apply {
            onGetPostings = { Result.failure(CoreDataFailure.NetworkUnavailable(UnknownHostException())) }
        }

    private fun snapshot(vararg ids: Long) = FeedSnapshot(postings = ids.map { posting(id = it) }, savedAt = FIXED_CLOCK.instant())

    @Test
    fun `초기 로드는 첫 페이지·오늘 신규 개수·이름·게시판을 함께 채운다`() {
        val state = viewModel().state.value

        assertEquals(FeedLoadState.Loaded, state.loadState)
        assertEquals(listOf(1L, 2L, 3L), state.postings.map(Posting::id))
        assertNull(state.nextCursor)
        assertEquals(3, state.todayNewCount)
        assertEquals("일혁", state.userName)
        assertEquals(listOf(1L, 2L), state.boards.map { it.id })
        assertEquals(FeedListingCategory.All, state.selectedCategory)
        assertEquals(0, state.activeFilterCount)
    }

    @Test
    fun `검색어는 즉시 상태에 반영되고 300ms 뒤에 재조회한다`() =
        runTest(mainDispatcherRule.dispatcher) {
            val repository =
                FakePostingRepository(
                    initial = listOf(posting(id = 1, title = "카카오 인턴"), posting(id = 2, title = "네이버 공채")),
                )
            val viewModel = viewModel(postingRepository = repository)
            val queriesBefore = repository.queries.size

            viewModel.onEvent(FeedUiEvent.SearchQueryChanged("카카오"))

            assertEquals("카카오", viewModel.state.value.searchInput)
            assertEquals("", viewModel.state.value.query.searchQuery)
            advanceTimeBy(FeedViewModel.SEARCH_DEBOUNCE_MS - 1)
            assertEquals(queriesBefore, repository.queries.size)

            advanceTimeBy(2)

            assertEquals("카카오", viewModel.state.value.query.searchQuery)
            assertEquals(
                listOf(1L),
                viewModel.state.value.postings
                    .map(Posting::id),
            )
        }

    @Test
    fun `연속 입력은 마지막 검색어로 한 번만 재조회한다`() =
        runTest(mainDispatcherRule.dispatcher) {
            val repository = FakePostingRepository(initial = listOf(posting(id = 1, title = "카카오 인턴")))
            val viewModel = viewModel(postingRepository = repository)
            val queriesBefore = repository.queries.size

            viewModel.onEvent(FeedUiEvent.SearchQueryChanged("카"))
            advanceTimeBy(100)
            viewModel.onEvent(FeedUiEvent.SearchQueryChanged("카카"))
            advanceTimeBy(100)
            viewModel.onEvent(FeedUiEvent.SearchQueryChanged("카카오"))
            advanceTimeBy(FeedViewModel.SEARCH_DEBOUNCE_MS + 1)

            assertEquals(queriesBefore + 1, repository.queries.size)
            assertEquals("카카오", viewModel.state.value.query.searchQuery)
        }

    @Test
    fun `카테고리 칩은 types 하나로 재조회한다`() {
        val repository = FakePostingRepository(initial = listOf(posting(id = 1), posting(id = 2, type = PostingType.Scholarship)))
        val viewModel = viewModel(postingRepository = repository)

        viewModel.onEvent(FeedUiEvent.FilterSelected(FeedListingCategory.Scholarship))

        assertEquals(listOf(PostingType.Scholarship), repository.queries.last().types)
        assertEquals(FeedListingCategory.Scholarship, viewModel.state.value.selectedCategory)
        assertEquals(
            listOf(2L),
            viewModel.state.value.postings
                .map(Posting::id),
        )
    }

    @Test
    fun `같은 칩을 다시 누르면 재조회하지 않는다`() {
        val repository = FakePostingRepository(initial = listOf(posting(id = 1)))
        val viewModel = viewModel(postingRepository = repository)
        val queriesBefore = repository.queries.size

        viewModel.onEvent(FeedUiEvent.FilterSelected(FeedListingCategory.All))

        assertEquals(queriesBefore, repository.queries.size)
    }

    @Test
    fun `필터 시트는 적용할 때만 조회 조건에 반영된다`() {
        val repository = FakePostingRepository(initial = listOf(posting(id = 1, boardId = 1, score = 80, dueDate = TODAY.plusDays(2))))
        val viewModel = viewModel(postingRepository = repository)
        val queriesBefore = repository.queries.size

        viewModel.onEvent(FeedUiEvent.FilterRequested)
        viewModel.onFilterEvent(FeedFilterEvent.BoardToggled("1"))
        viewModel.onFilterEvent(FeedFilterEvent.DeadlineSelected(UiDeadlineFilter.WithinWeek))
        viewModel.onFilterEvent(FeedFilterEvent.MinScoreSelected(FeedMinScoreFilter.AtLeast70))
        viewModel.onFilterEvent(FeedFilterEvent.UnreadOnlyToggled)

        val draft = requireNotNull(viewModel.state.value.filterDraft)
        assertEquals(setOf(1L), draft.boardIds)
        assertEquals(queriesBefore, repository.queries.size)

        viewModel.onFilterEvent(FeedFilterEvent.ApplyClicked)

        val state = viewModel.state.value
        assertNull(state.filterDraft)
        assertEquals(setOf(1L), state.query.boardIds)
        assertEquals(FeedDeadlineFilter.WithinWeek, state.query.deadline)
        assertEquals(70, state.query.minScore)
        assertTrue(state.query.unreadOnly)
        assertEquals(4, state.activeFilterCount)
        assertEquals(
            PostingQuery(boardIds = listOf(1L), minScore = 70, unreadOnly = true),
            repository.queries.last(),
        )
        assertEquals(listOf(1L), state.postings.map(Posting::id))
    }

    @Test
    fun `직접 지정 범위는 날짜를 고른 뒤에야 적용되고 다시 열면 그대로 남는다`() {
        val repository =
            FakePostingRepository(
                initial =
                    listOf(
                        posting(id = 1, dueDate = TODAY.plusDays(10)),
                        posting(id = 2, dueDate = TODAY.plusDays(40)),
                        posting(id = 3, dueDate = null),
                    ),
            )
        val viewModel = viewModel(postingRepository = repository)

        viewModel.onEvent(FeedUiEvent.FilterRequested)
        viewModel.onFilterEvent(FeedFilterEvent.DeadlineSelected(UiDeadlineFilter.Range))

        // 날짜가 하나도 없으면 거를 것이 없다 — 「적용」은 시트를 닫지 않는다.
        viewModel.onFilterEvent(FeedFilterEvent.ApplyClicked)
        assertNotNull(viewModel.state.value.filterDraft)

        viewModel.onFilterEvent(FeedFilterEvent.DeadlineRangeEndpointClicked(FeedDeadlineRangeEndpoint.Start))
        viewModel.onFilterEvent(FeedFilterEvent.DeadlineRangeDateSelected(TODAY.plusDays(5)))
        viewModel.onFilterEvent(FeedFilterEvent.DeadlineRangeEndpointClicked(FeedDeadlineRangeEndpoint.End))
        viewModel.onFilterEvent(FeedFilterEvent.DeadlineRangeDateSelected(TODAY.plusDays(20)))
        viewModel.onFilterEvent(FeedFilterEvent.ApplyClicked)

        val state = viewModel.state.value
        assertNull(state.filterDraft)
        assertEquals(
            FeedDeadlineFilter.Range(start = TODAY.plusDays(5), end = TODAY.plusDays(20)),
            state.query.deadline,
        )
        assertEquals(listOf(1L), state.postings.map(Posting::id))
        assertEquals(1, state.activeFilterCount)

        viewModel.onEvent(FeedUiEvent.FilterRequested)

        val reopened = requireNotNull(viewModel.state.value.filterDraft)
        assertEquals(UiDeadlineFilter.Range, reopened.deadline)
        assertEquals(
            FeedDeadlineRange(start = TODAY.plusDays(5), end = TODAY.plusDays(20)),
            reopened.deadlineRange,
        )
    }

    @Test
    fun `뒤집힌 범위는 적용되지 않고 초기화로 지워진다`() {
        val viewModel = viewModel()

        viewModel.onEvent(FeedUiEvent.FilterRequested)
        viewModel.onFilterEvent(FeedFilterEvent.DeadlineSelected(UiDeadlineFilter.Range))
        viewModel.onFilterEvent(FeedFilterEvent.DeadlineRangeEndpointClicked(FeedDeadlineRangeEndpoint.Start))
        viewModel.onFilterEvent(FeedFilterEvent.DeadlineRangeDateSelected(TODAY.plusDays(20)))
        viewModel.onFilterEvent(FeedFilterEvent.DeadlineRangeEndpointClicked(FeedDeadlineRangeEndpoint.End))
        viewModel.onFilterEvent(FeedFilterEvent.DeadlineRangeDateSelected(TODAY.plusDays(5)))

        val draft = requireNotNull(viewModel.state.value.filterDraft)
        assertFalse(draft.isApplicable)
        assertEquals(FeedDeadlineRangeError.StartAfterEnd, draft.deadlineRange.error)

        viewModel.onFilterEvent(FeedFilterEvent.ApplyClicked)

        assertNotNull(viewModel.state.value.filterDraft)
        assertEquals(FeedDeadlineFilter.All, viewModel.state.value.query.deadline)

        viewModel.onFilterEvent(FeedFilterEvent.ResetClicked)

        assertEquals(FeedFilterDraft.Default, viewModel.state.value.filterDraft)
        assertEquals(
            FeedDeadlineRange(),
            viewModel.state.value.filterDraft
                ?.deadlineRange,
        )
    }

    @Test
    fun `날짜 선택기를 그냥 닫으면 고른 범위는 그대로다`() {
        val viewModel = viewModel()

        viewModel.onEvent(FeedUiEvent.FilterRequested)
        viewModel.onFilterEvent(FeedFilterEvent.DeadlineSelected(UiDeadlineFilter.Range))
        viewModel.onFilterEvent(FeedFilterEvent.DeadlineRangeEndpointClicked(FeedDeadlineRangeEndpoint.Start))
        viewModel.onFilterEvent(FeedFilterEvent.DeadlineRangeDateSelected(TODAY))
        viewModel.onFilterEvent(FeedFilterEvent.DeadlineRangeEndpointClicked(FeedDeadlineRangeEndpoint.End))
        viewModel.onFilterEvent(FeedFilterEvent.DeadlineRangePickerDismissed)

        assertEquals(
            FeedDeadlineRange(start = TODAY, end = null, editing = null),
            viewModel.state.value.filterDraft
                ?.deadlineRange,
        )
    }

    @Test
    fun `범위가 걸린 조회는 스냅샷으로 저장하지 않는다`() {
        // 범위도 조건이다 — 부분집합을 「전체」로 저장하면 오프라인 목록이 거짓말을 한다.
        val snapshots = FakeFeedSnapshotRepository()
        val viewModel = viewModel(snapshotRepository = snapshots)
        snapshots.saved.clear()

        viewModel.onEvent(FeedUiEvent.FilterRequested)
        viewModel.onFilterEvent(FeedFilterEvent.DeadlineSelected(UiDeadlineFilter.Range))
        viewModel.onFilterEvent(FeedFilterEvent.DeadlineRangeEndpointClicked(FeedDeadlineRangeEndpoint.Start))
        viewModel.onFilterEvent(FeedFilterEvent.DeadlineRangeDateSelected(TODAY))
        viewModel.onFilterEvent(FeedFilterEvent.ApplyClicked)

        assertFalse(viewModel.state.value.query.isDefault)
        assertTrue(snapshots.saved.isEmpty())
    }

    @Test
    fun `필터 시트 초기화는 카테고리까지 되돌리고 닫기는 초안을 버린다`() {
        val viewModel = viewModel()
        viewModel.onEvent(FeedUiEvent.FilterSelected(FeedListingCategory.Employment))

        viewModel.onEvent(FeedUiEvent.FilterRequested)
        assertEquals(
            FeedListingCategory.Employment,
            viewModel.state.value.filterDraft
                ?.category,
        )
        viewModel.onFilterEvent(FeedFilterEvent.ResetClicked)
        assertEquals(FeedFilterDraft.Default, viewModel.state.value.filterDraft)
        viewModel.onFilterEvent(FeedFilterEvent.DismissClicked)

        assertNull(viewModel.state.value.filterDraft)
        assertEquals(FeedListingCategory.Employment, viewModel.state.value.selectedCategory)
    }

    @Test
    fun `정렬 선택은 메뉴를 닫고 재조회한다`() {
        val repository = FakePostingRepository(initial = listOf(posting(id = 1)))
        val viewModel = viewModel(postingRepository = repository)

        viewModel.onEvent(FeedUiEvent.SortMenuRequested)
        assertTrue(viewModel.state.value.isSortMenuVisible)
        viewModel.onSortEvent(FeedSortMenuEvent.SortSelected(FeedSortOption.DueAsc))

        assertFalse(viewModel.state.value.isSortMenuVisible)
        assertEquals(PostingSort.DueAsc, viewModel.state.value.query.sort)
        assertEquals(PostingSort.DueAsc, repository.queries.last().sort)
    }

    @Test
    fun `무한 스크롤은 커서를 따라 누적하고 끝에서는 요청하지 않는다`() {
        val repository = FakePostingRepository(initial = List(45) { posting(id = it + 1L) })
        val viewModel = viewModel(postingRepository = repository)
        assertEquals(20, viewModel.state.value.postings.size)
        assertEquals("20", viewModel.state.value.nextCursor)

        viewModel.onLoadMore()
        assertEquals(40, viewModel.state.value.postings.size)
        viewModel.onLoadMore()
        assertEquals(45, viewModel.state.value.postings.size)
        assertNull(viewModel.state.value.nextCursor)
        assertFalse(viewModel.state.value.isLoadingMore)

        val queriesBefore = repository.queries.size
        viewModel.onLoadMore()

        assertEquals(queriesBefore, repository.queries.size)
        assertEquals(
            45,
            viewModel.state.value.postings
                .map(Posting::id)
                .distinct()
                .size,
        )
    }

    @Test
    fun `클라이언트 필터로 빈 페이지가 와도 커서가 있으면 이어 읽는다`() {
        val expired = posting(id = 1, dueDate = TODAY.minusDays(1))
        val valid = posting(id = 2, dueDate = TODAY.plusDays(1))
        val repository =
            FakePostingRepository(
                onGetPostings = { query ->
                    Result.success(
                        when (query.cursor) {
                            null -> CursorPage(items = listOf(expired), nextCursor = "1")
                            else -> CursorPage(items = listOf(valid), nextCursor = null)
                        },
                    )
                },
            )

        val state = viewModel(postingRepository = repository).state.value

        assertEquals(FeedLoadState.Loaded, state.loadState)
        assertEquals(listOf(2L), state.postings.map(Posting::id))
        assertNull(state.nextCursor)
    }

    @Test
    fun `북마크는 먼저 뒤집고 실패하면 되돌린 뒤 스낵바를 띄운다`() =
        runTest(mainDispatcherRule.dispatcher) {
            val gate = CompletableDeferred<Unit>()
            val repository =
                FakePostingRepository(
                    initial = listOf(posting(id = 1, isBookmarked = false)),
                    onSetBookmarked = { _, _ ->
                        gate.await()
                        Result.failure(CoreDataFailure.NetworkUnavailable(UnknownHostException()))
                    },
                )
            val viewModel = viewModel(postingRepository = repository)

            viewModel.onEvent(FeedUiEvent.BookmarkToggled("1"))
            assertTrue(
                viewModel.state.value.postings
                    .single()
                    .isBookmarked,
            )

            gate.complete(Unit)

            assertFalse(
                viewModel.state.value.postings
                    .single()
                    .isBookmarked,
            )
            assertEquals(FeedMessage.BookmarkFailed, viewModel.state.value.message)
            assertEquals(listOf("bookmark"), reporter.stages)
            viewModel.onMessageConsumed()
            assertNull(viewModel.state.value.message)
        }

    @Test
    fun `북마크 성공은 서버가 돌려준 값으로 확정한다`() {
        val repository = FakePostingRepository(initial = listOf(posting(id = 1, isBookmarked = false)))
        val viewModel = viewModel(postingRepository = repository)

        viewModel.onEvent(FeedUiEvent.BookmarkToggled("1"))

        assertTrue(
            viewModel.state.value.postings
                .single()
                .isBookmarked,
        )
        assertEquals(listOf(1L to true), repository.bookmarkCalls.toList())
        assertNull(viewModel.state.value.message)
    }

    @Test
    fun `네트워크 단절은 네트워크 오류 상태가 되고 다시 시도로 복구한다`() {
        val repository = FakePostingRepository(initial = listOf(posting(id = 1)))
        repository.onGetPostings = { Result.failure(CoreDataFailure.NetworkUnavailable(UnknownHostException())) }
        val viewModel = viewModel(postingRepository = repository)

        assertEquals(FeedLoadState.Failed(isNetworkUnavailable = true), viewModel.state.value.loadState)
        assertTrue(reporter.stages.contains("feed_load"))

        repository.onGetPostings = null
        viewModel.retry()

        assertEquals(FeedLoadState.Loaded, viewModel.state.value.loadState)
        assertEquals(
            listOf(1L),
            viewModel.state.value.postings
                .map(Posting::id),
        )
    }

    @Test
    fun `서버 오류는 일반 실패 상태다`() {
        val repository = FakePostingRepository()
        repository.onGetPostings = { Result.failure(CoreDataFailure.ServerError("INTERNAL_ERROR", RuntimeException())) }

        assertEquals(FeedLoadState.Failed(isNetworkUnavailable = false), viewModel(postingRepository = repository).state.value.loadState)
    }

    @Test
    fun `401 은 세션 종료 신호를 올린다`() {
        val repository = FakePostingRepository()
        repository.onGetPostings = { Result.failure(CoreDataFailure.Unauthorized("AUTH_REQUIRED", RuntimeException())) }
        val viewModel = viewModel(postingRepository = repository)

        assertTrue(viewModel.state.value.sessionEnded)

        viewModel.onSessionEndedConsumed()

        assertFalse(viewModel.state.value.sessionEnded)
    }

    @Test
    fun `당겨서 새로고침은 목록을 유지한 채 다시 받는다`() {
        val repository = FakePostingRepository(initial = listOf(posting(id = 1)))
        val viewModel = viewModel(postingRepository = repository)
        repository.postings += posting(id = 2)

        viewModel.refresh()

        assertFalse(viewModel.state.value.isRefreshing)
        assertEquals(
            listOf(1L, 2L),
            viewModel.state.value.postings
                .map(Posting::id)
                .sorted(),
        )
    }

    @Test
    fun `새로고침 실패는 기존 목록을 지우지 않고 스낵바만 띄운다`() {
        val repository = FakePostingRepository(initial = listOf(posting(id = 1)))
        val viewModel = viewModel(postingRepository = repository)
        repository.onGetPostings = { Result.failure(CoreDataFailure.ServerError("INTERNAL_ERROR", RuntimeException())) }

        viewModel.refresh()

        assertEquals(FeedLoadState.Loaded, viewModel.state.value.loadState)
        assertEquals(
            listOf(1L),
            viewModel.state.value.postings
                .map(Posting::id),
        )
        assertEquals(FeedMessage.RefreshFailed, viewModel.state.value.message)
    }

    @Test
    fun `카드·알림·게시판 이동은 단발 신호로 올라가고 소비되면 비워진다`() {
        val viewModel = viewModel()

        viewModel.onEvent(FeedUiEvent.ListingSelected("2"))
        assertEquals(FeedDestination.PostingDetail(2L), viewModel.state.value.pendingNavigation)
        viewModel.onNavigationConsumed()
        assertNull(viewModel.state.value.pendingNavigation)

        viewModel.onEvent(FeedUiEvent.NotificationsSelected)
        assertEquals(FeedDestination.Notifications, viewModel.state.value.pendingNavigation)
        viewModel.onBoardListRequested()
        assertEquals(FeedDestination.BoardList, viewModel.state.value.pendingNavigation)
        viewModel.onBoardRegisterRequested()
        assertEquals(FeedDestination.BoardRegister, viewModel.state.value.pendingNavigation)
    }

    @Test
    fun `오늘 개수·게시판 조회 실패는 피드를 막지 않고 기록만 남긴다`() {
        val boardRepository = FakeBoardRepository.strict().apply { onGetBoards = { Result.failure(RuntimeException("boom")) } }

        val state = viewModel(boardRepository = boardRepository).state.value

        assertEquals(FeedLoadState.Loaded, state.loadState)
        assertTrue(state.boards.isEmpty())
        assertTrue(reporter.stages.contains("filter_boards"))
    }

    // ── 오프라인 스냅샷 (#86) ──────────────────────────────────────────────────

    @Test
    fun `기본 조건의 첫 페이지 성공은 스냅샷으로 저장한다`() {
        val snapshots = FakeFeedSnapshotRepository()

        viewModel(snapshotRepository = snapshots)

        assertEquals(1, snapshots.saved.size)
        assertEquals(
            listOf(1L, 2L, 3L),
            snapshots.saved
                .single()
                .postings
                .map(Posting::id),
        )
        assertEquals(FIXED_CLOCK.instant(), snapshots.saved.single().savedAt)
    }

    @Test
    fun `조건이 걸린 조회는 스냅샷으로 저장하지 않는다`() {
        // 필터된 부분집합을 「전체」로 저장하면 오프라인에서 빠진 공고를 모른 채 읽게 된다.
        val snapshots = FakeFeedSnapshotRepository()
        val viewModel = viewModel(snapshotRepository = snapshots)
        snapshots.saved.clear()

        viewModel.onEvent(FeedUiEvent.FilterSelected(FeedListingCategory.Employment))

        assertTrue(snapshots.saved.isEmpty())
    }

    @Test
    fun `빈 결과는 스냅샷으로 저장하지 않는다`() {
        val snapshots = FakeFeedSnapshotRepository()

        viewModel(postingRepository = FakePostingRepository(), snapshotRepository = snapshots)

        assertTrue(snapshots.saved.isEmpty())
    }

    @Test
    fun `네트워크 단절로 실패하면 저장된 스냅샷을 오프라인 제안으로 싣는다`() {
        val snapshots = FakeFeedSnapshotRepository(initial = snapshot(7L, 8L))

        val state = viewModel(postingRepository = offlinePostings(), snapshotRepository = snapshots).state.value

        assertEquals(FeedLoadState.Failed(isNetworkUnavailable = true), state.loadState)
        assertEquals(listOf(7L, 8L), state.offlineSnapshot?.postings?.map(Posting::id))
        assertFalse(state.isOffline)
    }

    @Test
    fun `스냅샷이 없으면 오프라인 제안도 없다`() {
        val state = viewModel(postingRepository = offlinePostings()).state.value

        assertNull(state.offlineSnapshot)
    }

    @Test
    fun `오프라인 모드로 보기는 스냅샷을 목록으로 걸고 저장 시각을 남긴다`() {
        val viewModel =
            viewModel(postingRepository = offlinePostings(), snapshotRepository = FakeFeedSnapshotRepository(initial = snapshot(7L, 8L)))

        viewModel.showOfflineSnapshot()

        val state = viewModel.state.value
        assertTrue(state.isOffline)
        assertEquals(FeedLoadState.Loaded, state.loadState)
        assertEquals(listOf(7L, 8L), state.postings.map(Posting::id))
        assertEquals(FIXED_CLOCK.instant(), state.offlineSavedAt)
        assertNull(state.nextCursor)
    }

    @Test
    fun `오프라인 목록에도 마감일 범위가 그대로 적용된다`() {
        // 스냅샷은 기본 조회의 사본이라 범위가 반영돼 있지 않다 — 걸어 둔 조건 밖 공고가 새어 나오면 안 된다.
        val snapshots =
            FakeFeedSnapshotRepository(
                initial =
                    FeedSnapshot(
                        postings =
                            listOf(
                                posting(id = 7, dueDate = TODAY.plusDays(10)),
                                posting(id = 8, dueDate = TODAY.plusDays(40)),
                                posting(id = 9, dueDate = null),
                            ),
                        savedAt = FIXED_CLOCK.instant(),
                    ),
            )
        val viewModel = viewModel(postingRepository = offlinePostings(), snapshotRepository = snapshots)

        viewModel.onEvent(FeedUiEvent.FilterRequested)
        viewModel.onFilterEvent(FeedFilterEvent.DeadlineSelected(UiDeadlineFilter.Range))
        viewModel.onFilterEvent(FeedFilterEvent.DeadlineRangeEndpointClicked(FeedDeadlineRangeEndpoint.Start))
        viewModel.onFilterEvent(FeedFilterEvent.DeadlineRangeDateSelected(TODAY.plusDays(5)))
        viewModel.onFilterEvent(FeedFilterEvent.DeadlineRangeEndpointClicked(FeedDeadlineRangeEndpoint.End))
        viewModel.onFilterEvent(FeedFilterEvent.DeadlineRangeDateSelected(TODAY.plusDays(20)))
        viewModel.onFilterEvent(FeedFilterEvent.ApplyClicked)
        viewModel.showOfflineSnapshot()

        val state = viewModel.state.value
        assertTrue(state.isOffline)
        assertEquals(listOf(7L), state.postings.map(Posting::id))
    }

    @Test
    fun `오프라인 모드에서는 더 불러오기가 돌지 않고 북마크는 안내만 한다`() {
        val postings = offlinePostings()
        val viewModel = viewModel(postingRepository = postings, snapshotRepository = FakeFeedSnapshotRepository(initial = snapshot(7L)))
        viewModel.showOfflineSnapshot()
        val queriesBefore = postings.queries.size

        viewModel.onLoadMore()
        viewModel.onEvent(FeedUiEvent.BookmarkToggled("7"))

        assertEquals(queriesBefore, postings.queries.size)
        assertEquals(FeedMessage.OfflineReadOnly, viewModel.state.value.message)
        assertTrue(postings.bookmarkCalls.isEmpty())
    }

    @Test
    fun `새로고침이 성공하면 온라인 목록으로 돌아온다`() {
        val postings = offlinePostings()
        val viewModel = viewModel(postingRepository = postings, snapshotRepository = FakeFeedSnapshotRepository(initial = snapshot(7L)))
        viewModel.showOfflineSnapshot()
        assertTrue(viewModel.state.value.isOffline)

        postings.onGetPostings = null
        postings.postings.addAll(List(2) { posting(id = it + 1L) })
        viewModel.refresh()

        val state = viewModel.state.value
        assertFalse(state.isOffline)
        assertNull(state.offlineSavedAt)
        assertNull(state.offlineSnapshot)
        assertEquals(listOf(1L, 2L), state.postings.map(Posting::id))
    }

    @Test
    fun `스냅샷 저장이 실패해도 조회 결과는 그대로 두고 기록만 남긴다`() {
        val snapshots =
            FakeFeedSnapshotRepository().apply {
                onSave = { Result.failure(IllegalStateException("disk full")) }
            }

        val viewModel = viewModel(snapshotRepository = snapshots)

        assertEquals(FeedLoadState.Loaded, viewModel.state.value.loadState)
        assertEquals(
            listOf(1L, 2L, 3L),
            viewModel.state.value.postings
                .map(Posting::id),
        )
        assertTrue(reporter.stages.contains("feed_snapshot_save"))
    }
}
