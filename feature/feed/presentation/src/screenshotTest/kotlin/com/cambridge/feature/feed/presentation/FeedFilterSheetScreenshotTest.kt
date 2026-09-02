package com.cambridge.feature.feed.presentation

import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.android.tools.screenshot.PreviewTest
import com.cambridge.core.ui.theme.CareerCompassTheme
import com.cambridge.feature.feed.presentation.feedfilter.FeedBoardFilterUiModel
import com.cambridge.feature.feed.presentation.feedfilter.FeedDeadlineFilter
import com.cambridge.feature.feed.presentation.feedfilter.FeedDeadlineRange
import com.cambridge.feature.feed.presentation.feedfilter.FeedFilterSheetContent
import com.cambridge.feature.feed.presentation.feedfilter.FeedFilterUiState
import com.cambridge.feature.feed.presentation.feedfilter.FeedMinScoreFilter
import java.time.LocalDate

@PreviewTest
@Preview(name = "Feed filter sheet", widthDp = 360, heightDp = 860)
@Composable
public fun FeedFilterSheetPreview() {
    FeedFilterPreviewSurface(state = feedFilterPreviewState())
}

@PreviewTest
@Preview(name = "Feed filter sheet without boards", widthDp = 360, heightDp = 860)
@Composable
public fun FeedFilterSheetNoBoardsPreview() {
    FeedFilterPreviewSurface(
        state =
            feedFilterPreviewState().copy(
                boards = emptyList(),
                selectedBoardIds = emptySet(),
                deadline = FeedDeadlineFilter.All,
                minScore = FeedMinScoreFilter.All,
                unreadOnly = false,
                matchingCount = null,
            ),
    )
}

@PreviewTest
@Preview(name = "Feed filter sheet with deadline range", widthDp = 360, heightDp = 860)
@Composable
public fun FeedFilterSheetDeadlineRangePreview() {
    FeedFilterPreviewSurface(
        state =
            feedFilterPreviewState().copy(
                deadline = FeedDeadlineFilter.Range,
                deadlineRange = FeedDeadlineRange(start = NOVEMBER_FIRST, end = NOVEMBER_LAST),
            ),
    )
}

/** 뒤집힌 범위 — 이유 문구가 뜨고 「적용」이 잠긴 모습을 골든으로 남긴다. */
@PreviewTest
@Preview(name = "Feed filter sheet with invalid deadline range", widthDp = 360, heightDp = 860)
@Composable
public fun FeedFilterSheetInvalidDeadlineRangePreview() {
    FeedFilterPreviewSurface(
        state =
            feedFilterPreviewState().copy(
                deadline = FeedDeadlineFilter.Range,
                deadlineRange = FeedDeadlineRange(start = NOVEMBER_LAST, end = NOVEMBER_FIRST),
            ),
    )
}

@Composable
private fun FeedFilterPreviewSurface(state: FeedFilterUiState) {
    CareerCompassTheme {
        Surface(color = CareerCompassTheme.colors.subtleSurface) {
            FeedFilterSheetContent(state = state, onEvent = {})
        }
    }
}

private fun feedFilterPreviewState(): FeedFilterUiState =
    FeedFilterUiState(
        categories =
            listOf(
                FeedFilterUiModel(FeedListingCategory.All, "전체"),
                FeedFilterUiModel(FeedListingCategory.Employment, "채용"),
                FeedFilterUiModel(FeedListingCategory.Scholarship, "장학금"),
                FeedFilterUiModel(FeedListingCategory.Contest, "공모전"),
                FeedFilterUiModel(FeedListingCategory.ExternalActivity, "대외활동"),
            ),
        selectedCategory = FeedListingCategory.Employment,
        boards =
            listOf(
                FeedBoardFilterUiModel(id = "school", name = "학교 게시판"),
                FeedBoardFilterUiModel(id = "official", name = "공식 채용"),
                FeedBoardFilterUiModel(id = "naver", name = "네이버 채용"),
                FeedBoardFilterUiModel(id = "contest", name = "공모전 사이트"),
            ),
        selectedBoardIds = setOf("school", "official"),
        deadline = FeedDeadlineFilter.WithinWeek,
        deadlineRange = null,
        minScore = FeedMinScoreFilter.AtLeast70,
        unreadOnly = true,
        matchingCount = 12,
    )

private val NOVEMBER_FIRST: LocalDate = LocalDate.of(2026, 11, 1)
private val NOVEMBER_LAST: LocalDate = LocalDate.of(2026, 11, 30)
