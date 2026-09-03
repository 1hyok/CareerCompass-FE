package com.cambridge.feature.feed.presentation

import android.content.res.Configuration
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.android.tools.screenshot.PreviewTest
import com.cambridge.core.ui.theme.CareerCompassTheme
import com.cambridge.feature.feed.presentation.feed.FeedFailureContent
import com.cambridge.feature.feed.presentation.shared.model.FeedFailureReason

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

@PreviewTest
@Preview(name = "Empty feed", widthDp = 360, heightDp = 772)
@Composable
public fun EmptyFeedScreenPreview() {
    FeedPreviewSurface(
        state =
            feedPreviewState().copy(
                totalListingCount = 0,
                content = FeedContentState.Empty,
            ),
    )
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
    CareerCompassTheme {
        Surface(color = CareerCompassTheme.colors.subtleSurface) {
            FeedFailureContent(
                reason = FeedFailureReason.Maintenance,
                onRetryClick = {},
                onOfflineClick = {},
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
            isNew = true,
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
            isNew = true,
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
            isNew = false,
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
            isNew = false,
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
            isNew = true,
            isBookmarked = false,
        ),
    )
