package com.cambridge.feature.feed.presentation

import android.content.res.Configuration
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.android.tools.screenshot.PreviewTest
import com.cambridge.feature.feed.presentation.feed.FeedFailureContent
import com.cambridge.feature.feed.presentation.shared.model.FeedFailureReason
import com.careercompass.core.ui.theme.CareerCompassTheme

@PreviewTest
@Preview(name = "Main feed", widthDp = 360, heightDp = 772)
@Composable
public fun FeedScreenPreview() {
    FeedPreviewSurface(state = feedPreviewState())
}

@PreviewTest
@Preview(name = "Main feed - Dark", uiMode = Configuration.UI_MODE_NIGHT_YES, widthDp = 360, heightDp = 772)
@Composable
public fun FeedScreenDarkPreview() {
    FeedPreviewSurface(state = feedPreviewState(), darkTheme = true)
}

/**
 * 앱에서 가장 오래 머무는 화면 — 검색칸·필터 칩 줄·카드가 한 화면에 겹쳐 있어 큰 글꼴에서 가장
 * 잃을 것이 많다.
 */
@PreviewTest
@Preview(name = "Main feed - Large font", widthDp = 360, heightDp = 772, fontScale = LARGE_FONT_SCALE)
@Composable
public fun FeedScreenLargeFontPreview() {
    FeedPreviewSurface(state = feedPreviewState())
}

/**
 * 빈 피드는 사유마다 다른 화면이다 — 사유별로 골든을 둔다.
 *
 * 가장 나쁜 경우가 첫 줄이다. 온보딩을 막 마쳐 게시판이 0개인 사용자는 바꿀 검색어도 필터도 없으므로,
 * 이 화면에 게시판 등록으로 가는 버튼이 실제로 그려지는지가 골든이 지켜야 할 것이다.
 */
@PreviewTest
@Preview(name = "Empty feed - no boards", widthDp = 360, heightDp = 772)
@Composable
public fun EmptyFeedNoBoardsPreview() {
    FeedPreviewSurface(state = emptyFeedPreviewState(FeedEmptyReason.NoBoards))
}

@PreviewTest
@Preview(name = "Empty feed - search", widthDp = 360, heightDp = 772)
@Composable
public fun EmptyFeedSearchPreview() {
    // 검색어와 필터가 함께 걸린 화면 — 검색어를 먼저 말한다(FeedEmptyReason KDoc 의 우선순위).
    FeedPreviewSurface(
        state =
            emptyFeedPreviewState(FeedEmptyReason.Search("백엔드")).copy(
                searchQuery = "백엔드",
                activeFilterCount = 2,
            ),
    )
}

@PreviewTest
@Preview(name = "Empty feed - filter", widthDp = 360, heightDp = 772)
@Composable
public fun EmptyFeedFilterPreview() {
    FeedPreviewSurface(state = emptyFeedPreviewState(FeedEmptyReason.Filter).copy(activeFilterCount = 2))
}

/**
 * 이슈 #206 — 사라진 게시판 사유는 문구가 둘로 갈린다. 골든도 둘이다.
 *
 * 갈리는 것이 이 사유의 요점이다. 조건이 이것뿐이면 「빼면 보여요」라고 해도 되지만, 다른 조건이 남아
 * 있으면 빼도 같은 빈 화면이 나올 수 있다 — 약속과 결과가 어긋나는 자리라 골든으로 문구를 붙든다.
 */
@PreviewTest
@Preview(name = "Empty feed - missing boards", widthDp = 360, heightDp = 772)
@Composable
public fun EmptyFeedMissingBoardsPreview() {
    FeedPreviewSurface(
        state =
            emptyFeedPreviewState(FeedEmptyReason.MissingBoards(count = 2, isOnlyCondition = true)).copy(
                activeFilterCount = 1,
            ),
    )
}

@PreviewTest
@Preview(name = "Empty feed - missing boards among others", widthDp = 360, heightDp = 772)
@Composable
public fun EmptyFeedMissingBoardsAmongOthersPreview() {
    FeedPreviewSurface(
        state =
            emptyFeedPreviewState(FeedEmptyReason.MissingBoards(count = 1, isOnlyCondition = false)).copy(
                searchQuery = "백엔드",
                activeFilterCount = 2,
            ),
    )
}

@PreviewTest
@Preview(name = "Empty feed - not collected", widthDp = 360, heightDp = 772)
@Composable
public fun EmptyFeedNotCollectedPreview() {
    // 게시판은 있고 조건도 없다 — 되돌릴 것이 없으므로 행동 대신 언제 들어오는지를 말한다.
    FeedPreviewSurface(
        state =
            emptyFeedPreviewState(
                FeedEmptyReason.NotCollected("등록한 게시판을 1일 1회 확인하고 있어요"),
            ),
    )
}

@PreviewTest
@Preview(name = "Empty feed - offline snapshot", widthDp = 360, heightDp = 772)
@Composable
public fun EmptyFeedOfflineSnapshotPreview() {
    // 오프라인 배너와 빈 상태가 각자 다른 사실을 말한다 — 배너는 「저장본을 보는 중」, 본문은 「그 안에 없다」.
    FeedPreviewSurface(
        state =
            emptyFeedPreviewState(FeedEmptyReason.OfflineSnapshot).copy(
                offlineNotice = "오프라인 · 9월 3일 14:20 기준 목록",
            ),
    )
}

@PreviewTest
@Preview(name = "Empty feed - more available", widthDp = 360, heightDp = 772)
@Composable
public fun EmptyFeedMoreAvailablePreview() {
    // 유일하게 조건을 되돌리라고 하지 않는 빈 화면이다 — 검색어가 걸려 있어도 「검색어 지우기」가 아니라
    // 「더 찾아보기」가 나와야 한다. 골든이 지킬 것은 그 행동 하나다.
    FeedPreviewSurface(
        state =
            emptyFeedPreviewState(FeedEmptyReason.MoreAvailable).copy(
                searchQuery = "백엔드",
                activeFilterCount = 2,
            ),
    )
}

// 빈 상태의 큰 글꼴 골든은 두지 않는다 — 같은 부품(core:ui `CareerCompassEmptyState`)의 2.0 배율
// 골든이 이미 있고, 글자 수는 loaded 화면이 최악이다(docs/testing/screenshot.md 「무엇을 넣고 무엇을 뺐나」).

/**
 * 목록 끝에서 자동 이어 읽기가 선 자리 — 멈춘 사유 한 줄과 이어 갈 버튼.
 *
 * 카드를 둘만 두어 목록 아래가 접히지 않게 한다. 골든이 지킬 것은 카드가 아니라 **끝줄**이다.
 */
@PreviewTest
@Preview(name = "Feed - load more paused", widthDp = 360, heightDp = 772)
@Composable
public fun FeedLoadMorePausedPreview() {
    FeedPagingPreviewSurface(loadMore = FeedLoadMoreState.Paused)
}

@PreviewTest
@Preview(name = "Feed - load more failed", widthDp = 360, heightDp = 772)
@Composable
public fun FeedLoadMoreFailedPreview() {
    // 스낵바는 지나가 버린다 — 목록 안에 남는 「다시 시도」가 실제로 그려지는지가 골든의 몫이다.
    FeedPagingPreviewSurface(loadMore = FeedLoadMoreState.Failed)
}

@PreviewTest
@Preview(name = "Loading feed", widthDp = 360, heightDp = 772)
@Composable
public fun LoadingFeedScreenPreview() {
    FeedPreviewSurface(
        state = feedPreviewState().copy(content = FeedContentState.Loading),
    )
}

@PreviewTest
@Preview(name = "Offline feed", widthDp = 360, heightDp = 772)
@Composable
public fun OfflineFeedScreenPreview() {
    // 오프라인 배너 — 목록이 지금 서버 상태가 아니라는 표시. 저장 시각이 문구에 들어간다.
    FeedPreviewSurface(
        state = feedPreviewState().copy(offlineNotice = "오프라인 · 9월 3일 14:20 기준 목록"),
    )
}

@PreviewTest
@Preview(name = "Feed with profile notice", widthDp = 360, heightDp = 772)
@Composable
public fun ProfileNoticeFeedScreenPreview() {
    // 프로필이 비어 점수를 못 내는 목록 — 상단 안내와 카드의 「프로필 필요」 칩이 같은 사유를 말한다.
    FeedPreviewSurface(
        state =
            feedPreviewState().copy(
                isProfileNoticeVisible = true,
                content =
                    FeedContentState.Loaded(
                        feedPreviewListings().map { it.copy(suitability = FeedSuitabilityState.ProfileIncomplete) },
                    ),
            ),
    )
}

@PreviewTest
@Preview(name = "Maintenance feed", widthDp = 360, heightDp = 772)
@Composable
public fun MaintenanceFeedScreenPreview() {
    // 서버 점검(503) — 스냅샷이 있으면 점검 중에도 「오프라인 모드로 보기」가 함께 열린다.
    // 기본 조회가 실패한 자리라 「조건 지우고 다시 보기」는 열지 않는다(되돌릴 조건이 없다).
    CareerCompassTheme {
        Surface(color = CareerCompassTheme.colors.subtleSurface) {
            FeedFailureContent(
                reason = FeedFailureReason.Maintenance,
                onRetryClick = {},
                onOfflineClick = {},
                onResetQueryClick = null,
            )
        }
    }
}

/**
 * 조건 때문에 실패한 자리 — 실패 화면이 헤더를 통째로 대신하므로, 조건에서 빠져나갈 길은 여기밖에 없다(#144).
 *
 * 사유 화면의 행동(새로고침·오프라인 모드)과 「조건 지우고 다시 보기」가 한 화면에 함께 서는 유일한 골든이라
 * 따로 둔다 — 셋이 겹쳐 눌릴 자리를 잃지 않는지, 안내 한 줄이 버튼과 붙어 읽히는지를 여기서만 볼 수 있다.
 */
@PreviewTest
@Preview(name = "Maintenance feed - query reset", widthDp = 360, heightDp = 772)
@Composable
public fun MaintenanceFeedQueryResetPreview() {
    CareerCompassTheme {
        Surface(color = CareerCompassTheme.colors.subtleSurface) {
            FeedFailureContent(
                reason = FeedFailureReason.Maintenance,
                onRetryClick = {},
                onOfflineClick = {},
                onResetQueryClick = {},
            )
        }
    }
}

/**
 * 큰 글꼴에서 탈출구가 살아남는가 — 안내 한 줄과 버튼 셋이 세로로 쌓이는 화면이라 가장 먼저 잘린다.
 */
@PreviewTest
@Preview(
    name = "Maintenance feed - query reset - Large font",
    widthDp = 360,
    heightDp = 772,
    fontScale = LARGE_FONT_SCALE,
)
@Composable
public fun MaintenanceFeedQueryResetLargeFontPreview() {
    CareerCompassTheme {
        Surface(color = CareerCompassTheme.colors.subtleSurface) {
            FeedFailureContent(
                reason = FeedFailureReason.Maintenance,
                onRetryClick = {},
                onOfflineClick = {},
                onResetQueryClick = {},
            )
        }
    }
}

@Composable
private fun FeedPreviewSurface(
    state: FeedUiState,
    darkTheme: Boolean = false,
) {
    CareerCompassTheme(darkTheme = darkTheme) {
        Surface(color = CareerCompassTheme.colors.subtleSurface) {
            FeedScreen(state = state, onEvent = {})
        }
    }
}

@Composable
private fun FeedPagingPreviewSurface(loadMore: FeedLoadMoreState) {
    CareerCompassTheme {
        Surface(color = CareerCompassTheme.colors.subtleSurface) {
            FeedScreen(
                state = feedPreviewState().copy(content = FeedContentState.Loaded(feedPreviewListings().take(2))),
                onEvent = {},
                listState = rememberLazyListState(),
                onLoadMore = {},
                loadMore = loadMore,
            )
        }
    }
}

/**
 * 빈 목록 골든의 공통 바탕 — 헤더·정렬 줄은 그대로 두고 목록 자리만 사유별 빈 상태로 바꾼다.
 *
 * 헤더의 필터 배지는 기본으로 끈다. 사유와 어긋난 배지(게시판 0개인데 「2개 적용」)가 붙어 있으면 골든
 * 한 장이 두 가지를 말하게 되어, 정작 봐야 할 안내·행동이 흐려진다. 조건이 사유의 근거인 화면
 * (검색어·필터)만 배지를 켠다.
 */
private fun emptyFeedPreviewState(reason: FeedEmptyReason): FeedUiState =
    feedPreviewState().copy(
        newListingCount = 0,
        totalListingCount = 0,
        content = FeedContentState.Empty(reason),
        activeFilterCount = 0,
    )

private fun feedPreviewState(): FeedUiState =
    FeedUiState(
        userName = "일혁",
        newListingCount = 12,
        searchQuery = "",
        filters =
            listOf(
                FeedFilterUiModel(FeedListingCategory.All, "전체"),
                FeedFilterUiModel(FeedListingCategory.Employment, "채용"),
                FeedFilterUiModel(FeedListingCategory.Scholarship, "장학금"),
                FeedFilterUiModel(FeedListingCategory.Contest, "공모전"),
                FeedFilterUiModel(FeedListingCategory.ExternalActivity, "대외활동"),
            ),
        selectedFilter = FeedListingCategory.All,
        selectedSort = FeedSortUiModel(id = "fit", label = "적합도 높은순"),
        totalListingCount = 12,
        content = FeedContentState.Loaded(feedPreviewListings()),
        activeFilterCount = 2,
    )

private fun feedPreviewListings(): List<FeedListingUiModel> =
    listOf(
        FeedListingUiModel(
            id = "kakao",
            title = "2026 카카오 SW 인턴십 모집",
            category = FeedListingCategory.Employment,
            categoryLabel = "채용",
            sourceLabel = "공식 채용",
            suitability = FeedSuitabilityState.Scored(88),
            deadlineLabel = "D-7",
            isDeadlineUrgent = false,
            collectedAtLabel = "오늘 수집",
            isNew = true,
            isRead = false,
            isBookmarked = false,
        ),
        FeedListingUiModel(
            id = "scholarship",
            title = "건국대 정보과학대학 우수학생 장학금",
            category = FeedListingCategory.Scholarship,
            categoryLabel = "장학금",
            sourceLabel = "학교 게시판",
            suitability = FeedSuitabilityState.Scored(82),
            deadlineLabel = "D-2",
            isDeadlineUrgent = true,
            collectedAtLabel = "오늘 수집",
            isNew = true,
            isRead = true,
            isBookmarked = false,
        ),
        FeedListingUiModel(
            id = "boostcamp",
            title = "네이버 부스트캠프 9기 모집",
            category = FeedListingCategory.Employment,
            categoryLabel = "채용",
            sourceLabel = "네이버 채용",
            suitability = FeedSuitabilityState.Scored(76),
            deadlineLabel = "D-14",
            isDeadlineUrgent = false,
            collectedAtLabel = "수집 3일 전",
            isNew = false,
            isRead = true,
            isBookmarked = true,
        ),
        FeedListingUiModel(
            id = "contest",
            title = "제 15회 대학생 SW 공모전",
            category = FeedListingCategory.Contest,
            categoryLabel = "공모전",
            sourceLabel = "공식 사이트",
            suitability = FeedSuitabilityState.Scored(64),
            deadlineLabel = "D-30",
            isDeadlineUrgent = false,
            collectedAtLabel = "수집 12일 전",
            isNew = false,
            isRead = false,
            isBookmarked = false,
        ),
        FeedListingUiModel(
            id = "analyzing",
            title = "학생회관 리모델링 공청회 참가자 모집",
            category = FeedListingCategory.Other,
            categoryLabel = "기타",
            sourceLabel = "학교 게시판",
            suitability = FeedSuitabilityState.Analyzing,
            deadlineLabel = "마감 미정",
            isDeadlineUrgent = false,
            collectedAtLabel = "오늘 수집",
            isNew = true,
            isRead = false,
            isBookmarked = false,
        ),
    )
