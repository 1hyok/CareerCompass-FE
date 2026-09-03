package com.cambridge.feature.feed.presentation.feed

import android.content.res.Resources
import com.cambridge.core.model.posting.PostingType
import com.cambridge.core.model.user.UserProfile
import com.cambridge.feature.feed.domain.model.FeedQuery
import com.cambridge.feature.feed.presentation.FIXED_CLOCK
import com.cambridge.feature.feed.presentation.FeedContentState
import com.cambridge.feature.feed.presentation.FeedEmptyReason
import com.cambridge.feature.feed.presentation.FeedListingUiModel
import com.cambridge.feature.feed.presentation.FeedLoadMoreState
import com.cambridge.feature.feed.presentation.FeedSuitabilityState
import com.cambridge.feature.feed.presentation.NOON_TODAY
import com.cambridge.feature.feed.presentation.board
import com.cambridge.feature.feed.presentation.posting
import com.cambridge.feature.feed.presentation.profile
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

/** 상태 조합(프로필 미입력·입력됨·모름 × 점수 있음·없음)이 화면 계약으로 어떻게 옮겨지는지. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class FeedStateMappingTest {
    private val resources: Resources = RuntimeEnvironment.getApplication().resources

    private val emptyProfile: UserProfile = profile(jobInterests = emptyList(), tags = emptyList())

    private fun uiState(profile: UserProfile?) =
        FeedViewState(
            profile = profile,
            postings = listOf(posting(id = 1), posting(id = 2, score = 88)),
            loadState = FeedLoadState.Loaded,
        ).toFeedUiState(resources, FIXED_CLOCK)

    private fun suitabilities(profile: UserProfile?): List<FeedSuitabilityState> {
        val content = uiState(profile).content
        return (content as FeedContentState.Loaded).listings.map(FeedListingUiModel::suitability)
    }

    @Test
    fun `프로필이 비면 점수 없는 카드만 프로필 미입력이고 안내가 켜진다`() {
        assertEquals(
            listOf(FeedSuitabilityState.ProfileIncomplete, FeedSuitabilityState.Scored(88)),
            suitabilities(emptyProfile),
        )
        assertTrue(uiState(emptyProfile).isProfileNoticeVisible)
    }

    @Test
    fun `프로필이 채워져 있으면 점수 없는 카드는 분석 중이다`() {
        assertEquals(
            listOf(FeedSuitabilityState.Analyzing, FeedSuitabilityState.Scored(88)),
            suitabilities(profile()),
        )
        assertFalse(uiState(profile()).isProfileNoticeVisible)
    }

    @Test
    fun `프로필을 모르면 점수 없는 카드는 분석 중이고 안내도 없다`() {
        assertEquals(
            listOf(FeedSuitabilityState.Analyzing, FeedSuitabilityState.Scored(88)),
            suitabilities(null),
        )
        assertFalse(uiState(null).isProfileNoticeVisible)
    }

    @Test
    fun `빈 목록에서 더 찾아보는 중에는 빈 상태 대신 진행 표시를 낸다`() {
        // 목록이 없으면 진행 표시를 붙일 자리도 없다 — 아무 반응이 없으면 「더 찾아보기」가 죽은 버튼이 된다.
        val state =
            FeedViewState(
                loadState = FeedLoadState.Loaded,
                nextCursor = "100",
                loadMore = FeedLoadMoreState.Loading,
            )

        assertEquals(FeedContentState.Loading, state.toFeedUiState(resources, FIXED_CLOCK).content)
    }

    @Test
    fun `사용자 이름은 프로필에서 오고 없으면 기본 호칭이다`() {
        assertEquals("일혁", uiState(profile()).userName)
        assertEquals("회원", uiState(profile(name = null)).userName)
        assertEquals("회원", uiState(null).userName)
    }
}

/**
 * 빈 목록의 사유 판정 — 우선순위와 근거는 [FeedEmptyReason] KDoc 에 있다.
 *
 * 사유가 하나만 걸린 경우보다 **겹친 경우**가 중요하다. 겹칠 때 잘못 고르면 사용자가 되돌릴 수 없는
 * 조건을 되돌리라는 안내를 받는다.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class FeedEmptyReasonTest {
    private val resources: Resources = RuntimeEnvironment.getApplication().resources

    private fun reasonOf(state: FeedViewState): FeedEmptyReason {
        val content = state.copy(loadState = FeedLoadState.Loaded, postings = emptyList()).toFeedUiState(resources, FIXED_CLOCK).content
        return (content as FeedContentState.Empty).reason
    }

    @Test
    fun `게시판이 0개면 등록을 안내한다`() {
        assertEquals(
            FeedEmptyReason.NoBoards,
            reasonOf(FeedViewState(boards = emptyList(), boardsLoaded = true)),
        )
    }

    @Test
    fun `게시판 목록을 못 받았으면 0개라고 단정하지 않는다`() {
        // 게시판 조회는 피드와 따로 실패한다. 못 받은 것을 0개로 읽으면 20개를 등록해 둔 사용자에게도
        // 「등록한 게시판이 없어요」가 나간다.
        assertEquals(
            FeedEmptyReason.NotCollected(collectNotice = null),
            reasonOf(FeedViewState(boards = emptyList(), boardsLoaded = false)),
        )
    }

    @Test
    fun `게시판이 0개면 검색어가 걸려 있어도 등록이 먼저다`() {
        val state =
            FeedViewState(
                boards = emptyList(),
                boardsLoaded = true,
                query = FeedQuery(searchQuery = "백엔드"),
            )

        assertEquals(FeedEmptyReason.NoBoards, reasonOf(state))
    }

    @Test
    fun `검색어와 필터가 함께 걸리면 검색어를 말한다`() {
        val state =
            FeedViewState(
                boards = listOf(board(id = 1)),
                boardsLoaded = true,
                query = FeedQuery(searchQuery = "백엔드", unreadOnly = true, types = setOf(PostingType.Recruit)),
            )

        assertEquals(FeedEmptyReason.Search("백엔드"), reasonOf(state))
    }

    @Test
    fun `검색어가 없으면 카테고리 칩만으로도 필터 사유가 된다`() {
        // 배지(activeFilterCount)는 칩을 세지 않지만, 칩도 풀면 결과가 달라지므로 사유로는 센다.
        val state =
            FeedViewState(
                boards = listOf(board(id = 1)),
                boardsLoaded = true,
                query = FeedQuery(types = setOf(PostingType.Recruit)),
            )

        assertEquals(FeedEmptyReason.Filter, reasonOf(state))
    }

    @Test
    fun `오프라인 스냅샷을 보는 중이면 조건을 되돌리라고 하지 않는다`() {
        // 조건을 되돌리는 행동은 곧 재조회다 — 오프라인에서 권하면 실패 화면으로 튄다.
        val state =
            FeedViewState(
                boards = emptyList(),
                boardsLoaded = true,
                isOffline = true,
                offlineSavedAt = NOON_TODAY,
                query = FeedQuery(searchQuery = "백엔드"),
            )

        assertEquals(FeedEmptyReason.OfflineSnapshot, reasonOf(state))
    }

    @Test
    fun `조건이 없으면 수집 주기를 근거로 언제 들어오는지 덧붙인다`() {
        val state = FeedViewState(boards = listOf(board(id = 1, cycleHours = 12)), boardsLoaded = true)

        assertEquals(
            FeedEmptyReason.NotCollected("등록한 게시판을 1일 2회 확인하고 있어요"),
            reasonOf(state),
        )
    }

    @Test
    fun `주기가 게시판마다 다르면 가장 짧은 주기를 말하고 그렇다고 밝힌다`() {
        val state =
            FeedViewState(
                boards = listOf(board(id = 1, cycleHours = 168), board(id = 2, cycleHours = 12)),
                boardsLoaded = true,
            )

        assertEquals(
            FeedEmptyReason.NotCollected("가장 자주 보는 게시판을 1일 2회 확인하고 있어요"),
            reasonOf(state),
        )
    }

    @Test
    fun `커서가 남았으면 검색어나 필터 탓으로 돌리지 않는다`() {
        // 검색어·마감일은 받아 온 페이지 안에서만 걸러진다 — 앞쪽 페이지가 통째로 걸러진 것과 서버에
        // 정말 없는 것이 똑같이 빈 목록이다. 그 둘을 가르는 근거는 커서뿐이다.
        val state =
            FeedViewState(
                boards = listOf(board(id = 1)),
                boardsLoaded = true,
                nextCursor = "100",
                query = FeedQuery(searchQuery = "백엔드", unreadOnly = true),
            )

        assertEquals(FeedEmptyReason.MoreAvailable, reasonOf(state))
    }

    @Test
    fun `커서가 남아도 게시판이 0개면 등록이 먼저다`() {
        // 모을 곳이 없으면 아무리 이어 읽어도 나올 공고가 없다.
        val state = FeedViewState(boards = emptyList(), boardsLoaded = true, nextCursor = "100")

        assertEquals(FeedEmptyReason.NoBoards, reasonOf(state))
    }

    @Test
    fun `오프라인 스냅샷에는 커서가 없어 판정이 어긋나지 않는다`() {
        // showOfflineSnapshot 이 nextCursor 를 비운다 — 저장본을 보면서 「더 찾아보기」를 권하면
        // 눌러도 갈 곳이 없다.
        val state =
            FeedViewState(
                boards = listOf(board(id = 1)),
                boardsLoaded = true,
                isOffline = true,
                offlineSavedAt = NOON_TODAY,
            )

        assertEquals(FeedEmptyReason.OfflineSnapshot, reasonOf(state))
    }

    @Test
    fun `커서가 없으면 조건 사유로 내려간다`() {
        val state =
            FeedViewState(
                boards = listOf(board(id = 1)),
                boardsLoaded = true,
                nextCursor = null,
                query = FeedQuery(searchQuery = "백엔드"),
            )

        assertEquals(FeedEmptyReason.Search("백엔드"), reasonOf(state))
    }

    @Test
    fun `꺼 둔 게시판의 주기는 세지 않고 전부 꺼져 있으면 아무 말도 하지 않는다`() {
        val paused = board(id = 1, cycleHours = 12, isActive = false)
        val active = board(id = 2, cycleHours = 24)

        assertEquals(
            FeedEmptyReason.NotCollected("등록한 게시판을 1일 1회 확인하고 있어요"),
            reasonOf(FeedViewState(boards = listOf(paused, active), boardsLoaded = true)),
        )
        assertEquals(
            FeedEmptyReason.NotCollected(collectNotice = null),
            reasonOf(FeedViewState(boards = listOf(paused), boardsLoaded = true)),
        )
    }
}
