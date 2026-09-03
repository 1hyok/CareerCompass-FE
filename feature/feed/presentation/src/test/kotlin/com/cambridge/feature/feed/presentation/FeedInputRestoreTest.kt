package com.cambridge.feature.feed.presentation

import android.os.Parcel
import androidx.lifecycle.SavedStateHandle
import com.cambridge.core.domain.testing.FakeBoardRepository
import com.cambridge.core.domain.testing.FakePostingRepository
import com.cambridge.core.domain.testing.FakeUserProfileRepository
import com.cambridge.core.model.posting.Posting
import com.cambridge.core.model.posting.PostingQuery
import com.cambridge.core.model.posting.PostingSort
import com.cambridge.core.model.posting.PostingType
import com.cambridge.feature.feed.domain.testing.FakeFeedSnapshotRepository
import com.cambridge.feature.feed.domain.usecase.CountTodayNewPostingsUseCase
import com.cambridge.feature.feed.domain.usecase.DetectBoardUseCase
import com.cambridge.feature.feed.domain.usecase.GetBoardsUseCase
import com.cambridge.feature.feed.domain.usecase.GetFeedPageUseCase
import com.cambridge.feature.feed.domain.usecase.RegisterBoardUseCase
import com.cambridge.feature.feed.domain.usecase.TogglePostingBookmarkUseCase
import com.cambridge.feature.feed.presentation.board.BoardCollectCycle
import com.cambridge.feature.feed.presentation.board.BoardDetectionState
import com.cambridge.feature.feed.presentation.board.BoardRegisterEvent
import com.cambridge.feature.feed.presentation.board.BoardRegisterViewModel
import com.cambridge.feature.feed.presentation.board.BoardType
import com.cambridge.feature.feed.presentation.feed.FeedViewModel
import com.cambridge.feature.feed.presentation.feedfilter.FeedDeadlineRangeEndpoint
import com.cambridge.feature.feed.presentation.feedfilter.FeedFilterEvent
import com.cambridge.feature.feed.presentation.feedfilter.FeedMinScoreFilter
import com.cambridge.feature.feed.presentation.feedfilter.FeedSortMenuEvent
import com.cambridge.feature.feed.presentation.feedfilter.FeedSortOption
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
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.LocalDate
import com.cambridge.feature.feed.domain.model.FeedDeadlineFilter as DomainDeadlineFilter
import com.cambridge.feature.feed.presentation.feedfilter.FeedDeadlineFilter as UiDeadlineFilter

/**
 * 프로세스 사망 뒤 피드의 입력이 살아 돌아오는지 — 이슈 #137.
 *
 * ### 왜 재구성이 아니라 이 방식인가
 * Robolectric 의 화면 재구성(회전·테마 변경)으로는 이 결함이 잡히지 않는다. 재구성은 ViewModel 을 **살려 두므로**
 * 저장이 하나도 없어도 상태가 그대로 남는다. 여기서는 진짜 경로를 태운다 — [SavedStateHandle] 을 `Bundle` 로
 * 저장하고, 그 번들을 [Parcel] 에 마샬링했다가 되읽어(안드로이드가 프로세스 경계를 넘길 때 하는 그대로) 새
 * ViewModel 을 세운다. 저장할 수 없는 값을 넣으면 여기서 깨지고, 배선을 빠뜨리면 값이 비어 돌아온다.
 *
 * 무엇을 남기고 무엇을 버리는지의 근거는 `FeedInputDraft`·`BoardRegisterInputDraft` 의 KDoc 에 있다.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class FeedInputRestoreTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val reporter = RecordingErrorReporter()

    // ---- 게시판 등록 폼 ----

    @Test
    fun `게시판 등록 폼에 친 값은 프로세스가 죽어도 남는다`() {
        val handle = SavedStateHandle()
        val before = boardRegisterViewModel(savedStateHandle = handle)
        before.onEvent(BoardRegisterEvent.UrlChanged("https://konkuk.ac.kr/board/notice"))
        before.onEvent(BoardRegisterEvent.NameChanged("건국대 공지사항"))
        before.onEvent(BoardRegisterEvent.TypeSelected(BoardType.Scholarship))
        before.onEvent(BoardRegisterEvent.CycleSelected(BoardCollectCycle.TwiceDaily))

        val after = boardRegisterViewModel(savedStateHandle = handle.acrossProcessDeath())

        val state = after.state.value
        assertEquals("https://konkuk.ac.kr/board/notice", state.url)
        assertEquals("건국대 공지사항", state.name)
        assertEquals(BoardType.Scholarship, state.type)
        assertEquals(BoardCollectCycle.TwiceDaily, state.cycle)
    }

    @Test
    fun `감지 결과와 입력 오류·전송 중 상태는 복원하지 않는다`() {
        val handle = SavedStateHandle()
        val repository = FakeBoardRepository()
        val before = boardRegisterViewModel(repository = repository, savedStateHandle = handle)
        before.onEvent(BoardRegisterEvent.UrlChanged("konkuk.ac.kr/board/notice"))
        before.onEvent(BoardRegisterEvent.DetectClicked)
        before.onEvent(BoardRegisterEvent.NameChanged("건국대 공지사항"))
        // 복원 대상이 아니라는 판정이 의미를 가지려면, 죽기 전에는 그 값이 있었어야 한다.
        assertTrue(before.state.value.detection is BoardDetectionState.Success)
        assertNotNull(before.state.value.detectedUrl)

        val after = boardRegisterViewModel(repository = repository, savedStateHandle = handle.acrossProcessDeath())

        val state = after.state.value
        assertEquals("konkuk.ac.kr/board/notice", state.url)
        assertEquals("건국대 공지사항", state.name)
        assertEquals(BoardDetectionState.Idle, state.detection)
        assertNull(state.detectedUrl)
        assertNull(state.urlError)
        assertFalse(state.isSubmitting)
        assertFalse(state.isRegisterEnabled)
        assertNull(state.message)
    }

    /**
     * 상한을 넘긴 주소는 초안을 남기지 않는다. 이름은 그대로 둔다 — 무엇을 다시 붙여 넣어야 하는지가
     * 사용자에게 보이고, 반쯤 잘린 주소로 감지를 눌러 「지원되지 않는 사이트」를 보는 일도 없다.
     */
    @Test
    fun `한도를 넘게 붙여 넣은 주소는 초안을 남기지 않는다`() {
        val handle = SavedStateHandle()
        val before = boardRegisterViewModel(savedStateHandle = handle)
        before.onEvent(BoardRegisterEvent.UrlChanged(OVERSIZED_URL))
        before.onEvent(BoardRegisterEvent.NameChanged("건국대 공지사항"))

        val after = boardRegisterViewModel(savedStateHandle = handle.acrossProcessDeath())

        assertEquals("", after.state.value.url)
        assertEquals("건국대 공지사항", after.state.value.name)
    }

    // ---- 검색어 ----

    @Test
    fun `검색어는 살아남고 복원한 조건으로 첫 페이지를 다시 조회한다`() =
        runTest(mainDispatcherRule.dispatcher) {
            val handle = SavedStateHandle()
            val repository = FakePostingRepository(initial = listOf(posting(id = 1, title = "카카오 인턴"), posting(id = 2, title = "네이버 공채")))
            val before = feedViewModel(postingRepository = repository, savedStateHandle = handle)
            before.onEvent(FeedUiEvent.SearchQueryChanged("카카오"))
            advanceTimeBy(FeedViewModel.SEARCH_DEBOUNCE_MS + 1)
            val queriesBefore = repository.queries.size

            val after = feedViewModel(postingRepository = repository, savedStateHandle = handle.acrossProcessDeath())

            val state = after.state.value
            assertEquals("카카오", state.searchInput)
            assertEquals("카카오", state.query.searchQuery)
            // 낡은 목록을 되살리는 것이 아니라, 복원한 조건으로 첫 페이지를 새로 읽는다.
            assertTrue(repository.queries.size > queriesBefore)
            assertNull(repository.queries.last().cursor)
            assertEquals(listOf(1L), state.postings.map(Posting::id))
        }

    /** 검색은 300ms 뒤에 조회에 반영된다 — 그 사이에 죽어도 사용자가 친 글자가 곧 의도다. */
    @Test
    fun `디바운스가 돌기 전에 죽어도 검색어는 조회 조건이 된다`() =
        runTest(mainDispatcherRule.dispatcher) {
            val handle = SavedStateHandle()
            val repository = FakePostingRepository(initial = listOf(posting(id = 1, title = "카카오 인턴"), posting(id = 2, title = "네이버 공채")))
            val before = feedViewModel(postingRepository = repository, savedStateHandle = handle)
            before.onEvent(FeedUiEvent.SearchQueryChanged("카카오"))
            assertEquals("", before.state.value.query.searchQuery)

            val after = feedViewModel(postingRepository = repository, savedStateHandle = handle.acrossProcessDeath())

            assertEquals("카카오", after.state.value.query.searchQuery)
            assertEquals(
                listOf(1L),
                after.state.value.postings
                    .map(Posting::id),
            )
        }

    // ---- 적용된 필터·정렬 ----

    @Test
    fun `적용된 필터와 정렬은 살아남아 목록을 다시 세운다`() {
        val handle = SavedStateHandle()
        val repository = FakePostingRepository(initial = listOf(posting(id = 1)))
        val before = feedViewModel(postingRepository = repository, savedStateHandle = handle)
        before.onEvent(FeedUiEvent.FilterRequested)
        before.onFilterEvent(FeedFilterEvent.BoardToggled("2"))
        before.onFilterEvent(FeedFilterEvent.MinScoreSelected(FeedMinScoreFilter.AtLeast70))
        before.onFilterEvent(FeedFilterEvent.UnreadOnlyToggled)
        before.onFilterEvent(FeedFilterEvent.DeadlineSelected(UiDeadlineFilter.Range))
        before.onFilterEvent(FeedFilterEvent.DeadlineRangeEndpointClicked(FeedDeadlineRangeEndpoint.Start))
        before.onFilterEvent(FeedFilterEvent.DeadlineRangeDateSelected(RANGE_START))
        before.onFilterEvent(FeedFilterEvent.ApplyClicked)
        before.onSortEvent(FeedSortMenuEvent.SortSelected(FeedSortOption.DueAsc))

        val after = feedViewModel(postingRepository = repository, savedStateHandle = handle.acrossProcessDeath())

        val query = after.state.value.query
        assertEquals(setOf(2L), query.boardIds)
        assertEquals(70, query.minScore)
        assertTrue(query.unreadOnly)
        assertEquals(DomainDeadlineFilter.Range(start = RANGE_START, end = null), query.deadline)
        assertEquals(PostingSort.DueAsc, query.sort)
        // 서버에 실제로 그 조건이 실려 나갔는가 — 복원한 조건은 목록을 다시 조회할 근거일 뿐이다.
        val sent = repository.queries.last()
        assertEquals(listOf(2L), sent.boardIds)
        assertEquals(70, sent.minScore)
        assertTrue(sent.unreadOnly)
        assertEquals(PostingSort.DueAsc, sent.sort)
        assertNull(sent.cursor)
    }

    @Test
    fun `더 불러온 페이지는 복원하지 않고 첫 페이지부터 다시 읽는다`() {
        val handle = SavedStateHandle()
        val repository = FakePostingRepository(initial = List(PAGE_LIMIT + 5) { posting(id = it + 1L) })
        val before = feedViewModel(postingRepository = repository, savedStateHandle = handle)
        before.onLoadMore()
        assertEquals(PAGE_LIMIT + 5, before.state.value.postings.size)

        val after = feedViewModel(postingRepository = repository, savedStateHandle = handle.acrossProcessDeath())

        assertEquals(PAGE_LIMIT, after.state.value.postings.size)
        assertNotNull(after.state.value.nextCursor)
        assertNull(repository.queries.last().cursor)
    }

    // ---- 필터 시트 초안 ----

    @Test
    fun `시트가 열린 채 죽으면 저절로 열리지는 않지만 다시 열면 고르던 값이 있다`() {
        val handle = SavedStateHandle()
        val before = feedViewModel(savedStateHandle = handle)
        before.onEvent(FeedUiEvent.FilterRequested)
        before.onFilterEvent(FeedFilterEvent.CategorySelected(FeedListingCategory.Scholarship))
        before.onFilterEvent(FeedFilterEvent.BoardToggled("1"))
        before.onFilterEvent(FeedFilterEvent.MinScoreSelected(FeedMinScoreFilter.AtLeast80))

        val after = feedViewModel(savedStateHandle = handle.acrossProcessDeath())

        // 프로세스 사망은 사용자가 의도한 이동이 아니다 — 돌아온 화면에 시트가 떠 있지 않다.
        assertNull(after.state.value.filterDraft)
        // 적용하지 않은 조건이므로 조회는 그대로다.
        assertEquals(emptySet<Long>(), after.state.value.query.boardIds)
        assertNull(after.state.value.query.minScore)

        after.onEvent(FeedUiEvent.FilterRequested)

        val draft = requireNotNull(after.state.value.filterDraft)
        assertEquals(FeedListingCategory.Scholarship, draft.category)
        assertEquals(setOf(1L), draft.boardIds)
        assertEquals(80, draft.minScore)
    }

    @Test
    fun `시트를 닫으면 초안도 함께 버린다`() {
        val handle = SavedStateHandle()
        val before = feedViewModel(savedStateHandle = handle)
        before.onEvent(FeedUiEvent.FilterRequested)
        before.onFilterEvent(FeedFilterEvent.BoardToggled("1"))
        before.onFilterEvent(FeedFilterEvent.DismissClicked)

        val after = feedViewModel(savedStateHandle = handle.acrossProcessDeath())
        after.onEvent(FeedUiEvent.FilterRequested)

        assertEquals(emptySet<Long>(), requireNotNull(after.state.value.filterDraft).boardIds)
    }

    /** 살아난 초안은 조회 조건이 바뀌는 순간 낡은 값이다 — 칩으로 고른 카테고리와 어긋난 채 시트가 열리면 안 된다. */
    @Test
    fun `초안이 살아 있어도 칩으로 조건을 바꾸면 시트는 바뀐 조건에서 시작한다`() {
        val handle = SavedStateHandle()
        val before = feedViewModel(savedStateHandle = handle)
        before.onEvent(FeedUiEvent.FilterRequested)
        before.onFilterEvent(FeedFilterEvent.CategorySelected(FeedListingCategory.Scholarship))

        val after = feedViewModel(savedStateHandle = handle.acrossProcessDeath())
        after.onEvent(FeedUiEvent.FilterSelected(FeedListingCategory.Contest))
        after.onEvent(FeedUiEvent.FilterRequested)

        assertEquals(FeedListingCategory.Contest, requireNotNull(after.state.value.filterDraft).category)
    }

    // ---- 저장소 방어 ----

    /** 낡거나 망가진 번들이 [com.cambridge.feature.feed.domain.model.FeedQuery] 의 계약을 깨뜨려 앱을 죽이지 않는다. */
    @Test
    fun `망가진 초안이 들어와도 계약을 지켜 되살린다`() {
        val handle =
            SavedStateHandle(
                mapOf(
                    "feed.draft.searchInput" to "카카오",
                    // 모르는 유형 이름·선택지 밖의 점수·모르는 정렬 이름·뒤집힌 마감일 범위가 섞인 초안.
                    "feed.draft.query.types" to arrayListOf("Recruit", "사라진유형"),
                    "feed.draft.query.boardIds" to longArrayOf(7L),
                    "feed.draft.query.minScore" to 55,
                    "feed.draft.query.sort" to "사라진정렬",
                    "feed.draft.query.deadline" to "Range",
                    "feed.draft.query.deadline.start" to LocalDate.of(2026, 10, 1).toEpochDay(),
                    "feed.draft.query.deadline.end" to LocalDate.of(2026, 9, 1).toEpochDay(),
                ),
            )

        val query = feedViewModel(savedStateHandle = handle.acrossProcessDeath()).state.value.query

        assertEquals(setOf(PostingType.Recruit), query.types)
        assertEquals(setOf(7L), query.boardIds)
        assertNull(query.minScore)
        assertEquals(PostingSort.CollectedDesc, query.sort)
        assertEquals(DomainDeadlineFilter.All, query.deadline)
        assertEquals("카카오", query.searchQuery)
    }

    // ---- 조립 ----

    /**
     * 안드로이드가 프로세스를 죽였다 되살릴 때 하는 그대로 — 핸들을 `Bundle` 로 저장하고 [Parcel] 에 마샬링했다가
     * 되읽는다. 번들에 담기지 못하는 값을 저장하면 여기서 드러난다.
     */
    private fun SavedStateHandle.acrossProcessDeath(): SavedStateHandle {
        val parcel = Parcel.obtain()
        return try {
            parcel.writeBundle(savedStateProvider().saveState())
            parcel.setDataPosition(0)
            SavedStateHandle.createHandle(parcel.readBundle(javaClass.classLoader), null)
        } finally {
            parcel.recycle()
        }
    }

    private fun feedViewModel(
        postingRepository: FakePostingRepository = FakePostingRepository(initial = List(3) { posting(id = it + 1L) }),
        savedStateHandle: SavedStateHandle = SavedStateHandle(),
    ): FeedViewModel =
        FeedViewModel(
            getFeedPage = GetFeedPageUseCase(postingRepository, FIXED_CLOCK),
            countTodayNewPostings = CountTodayNewPostingsUseCase(postingRepository, FIXED_CLOCK),
            togglePostingBookmark = TogglePostingBookmarkUseCase(postingRepository),
            getBoards = GetBoardsUseCase(FakeBoardRepository(initial = listOf(board(id = 1), board(id = 2)))),
            userProfileRepository = FakeUserProfileRepository(initialProfile = profile()),
            feedSnapshotRepository = FakeFeedSnapshotRepository(),
            errorReporter = reporter,
            clock = FIXED_CLOCK,
            savedStateHandle = savedStateHandle,
        )

    private fun boardRegisterViewModel(
        repository: FakeBoardRepository = FakeBoardRepository(),
        savedStateHandle: SavedStateHandle = SavedStateHandle(),
    ): BoardRegisterViewModel =
        BoardRegisterViewModel(
            detectBoard = DetectBoardUseCase(repository),
            registerBoard = RegisterBoardUseCase(repository),
            errorReporter = reporter,
            savedStateHandle = savedStateHandle,
        )

    private companion object {
        val RANGE_START: LocalDate = LocalDate.of(2026, 9, 10)

        const val PAGE_LIMIT = PostingQuery.DEFAULT_LIMIT

        /** 초안 상한(2,048자)을 한 글자 넘긴 주소 — 사람이 복사한 주소가 아니라 페이지를 통째로 붙여 넣은 경우다. */
        val OVERSIZED_URL = "https://konkuk.ac.kr/" + "a".repeat(2_028)
    }
}
