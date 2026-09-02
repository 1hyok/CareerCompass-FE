package com.cambridge.feature.feed.presentation.shared.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.cambridge.core.ui.theme.CareerCompassTheme

/** Centered progress indicator with a caption, used while a screen's content is loading. */
@Composable
internal fun FeedLoadingContent(
    message: String,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(CareerCompassTheme.spacing.medium),
        ) {
            CircularProgressIndicator(color = CareerCompassTheme.colors.primaryEmphasis)
            Text(
                text = message,
                color = CareerCompassTheme.colors.onSurfaceVariant,
                style = CareerCompassTheme.typography.bodyMedium,
            )
        }
    }
}
