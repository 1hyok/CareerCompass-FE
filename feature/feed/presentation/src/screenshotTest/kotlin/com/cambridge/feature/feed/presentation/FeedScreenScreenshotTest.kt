package com.cambridge.feature.feed.presentation

import android.content.res.Configuration
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.android.tools.screenshot.PreviewTest
import com.cambridge.core.ui.theme.CareerCompassTheme

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
            suitabilityScore = 88,
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
            suitabilityScore = 82,
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
            suitabilityScore = 76,
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
            suitabilityScore = 64,
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
            suitabilityScore = null,
            deadlineLabel = "마감 미정",
            isDeadlineUrgent = false,
            isNew = true,
            isBookmarked = false,
        ),
    )
