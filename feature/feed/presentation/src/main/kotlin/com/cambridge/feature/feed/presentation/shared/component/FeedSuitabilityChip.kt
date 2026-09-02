package com.cambridge.feature.feed.presentation.shared.component

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cambridge.core.ui.component.CareerCompassScoreChip
import com.cambridge.core.ui.component.CareerCompassScoreLevel
import com.cambridge.core.ui.theme.CareerCompassTheme
import com.cambridge.feature.feed.presentation.R

/**
 * Suitability readout of a listing card: the score chip when a score exists, otherwise an
 * "analyzing" pill of the same height so cards with and without a score line up (spec F2-3·F3-1).
 */
@Composable
internal fun FeedSuitabilityChip(
    score: Int?,
    modifier: Modifier = Modifier,
) {
    if (score == null) {
        FeedAnalyzingChip(modifier = modifier)
    } else {
        CareerCompassScoreChip(
            label = stringResource(R.string.feed_suitability_label),
            score = score,
            level = score.suitabilityLevel(),
            modifier = modifier,
            contentDescription = stringResource(R.string.feed_suitability_content_description, score),
        )
    }
}

@Composable
private fun FeedAnalyzingChip(modifier: Modifier = Modifier) {
    val colors = CareerCompassTheme.colors
    val analyzingDescription = stringResource(R.string.feed_suitability_analyzing_content_description)

    Surface(
        modifier =
            modifier.semantics(mergeDescendants = true) {
                contentDescription = analyzingDescription
            },
        shape = CareerCompassTheme.shapes.pill,
        color = colors.subtleSurface,
    ) {
        Text(
            text = stringResource(R.string.feed_suitability_analyzing),
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
            maxLines = 1,
            color = colors.mutedContent,
            style =
                CareerCompassTheme.typography.caption.copy(
                    lineHeight = 16.5.sp,
                    fontWeight = FontWeight.Medium,
                ),
        )
    }
}

/** Score level thresholds shared by the feed card and the detail screen (spec F3-2). */
internal fun Int.suitabilityLevel(): CareerCompassScoreLevel =
    when {
        this >= HIGH_SCORE_THRESHOLD -> CareerCompassScoreLevel.High
        this >= MID_SCORE_THRESHOLD -> CareerCompassScoreLevel.Mid
        else -> CareerCompassScoreLevel.Low
    }

internal const val HIGH_SCORE_THRESHOLD: Int = 80
internal const val MID_SCORE_THRESHOLD: Int = 60
