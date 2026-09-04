package com.careercompass.feature.feed.presentation

import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.android.tools.screenshot.PreviewTest
import com.careercompass.core.ui.theme.CareerCompassTheme
import com.careercompass.feature.feed.presentation.feedfilter.FeedSortMenuContent
import com.careercompass.feature.feed.presentation.feedfilter.FeedSortMenuUiState
import com.careercompass.feature.feed.presentation.feedfilter.FeedSortOption

@PreviewTest
@Preview(name = "Feed sort menu", widthDp = 360, heightDp = 772)
@Composable
public fun FeedSortMenuPreview() {
    CareerCompassTheme {
        Surface(color = CareerCompassTheme.colors.subtleSurface) {
            FeedSortMenuContent(
                state = FeedSortMenuUiState(selected = FeedSortOption.ScoreDesc),
                onEvent = {},
            )
        }
    }
}
