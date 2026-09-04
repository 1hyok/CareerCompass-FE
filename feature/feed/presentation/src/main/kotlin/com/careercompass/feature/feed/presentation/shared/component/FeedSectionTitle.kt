package com.careercompass.feature.feed.presentation.shared.component

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import com.careercompass.core.ui.theme.CareerCompassTheme

/** Section heading used inside the feed detail, filter, and board screens. */
@Composable
internal fun FeedSectionTitle(
    text: String,
    modifier: Modifier = Modifier,
) {
    Text(
        text = text,
        modifier = modifier.semantics { heading() },
        color = CareerCompassTheme.colors.onSurface,
        style = CareerCompassTheme.typography.headline4,
    )
}
