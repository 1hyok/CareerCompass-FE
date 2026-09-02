package com.cambridge.feature.feed.presentation.postingdetail.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cambridge.core.ui.component.CareerCompassBadge
import com.cambridge.core.ui.component.CareerCompassScoreLevel
import com.cambridge.core.ui.theme.CareerCompassTheme
import com.cambridge.feature.feed.presentation.R

/** Large score readout with a level badge and a determinate progress bar (spec F3-3). */
@Composable
internal fun SuitabilityGauge(
    score: Int,
    levelLabel: String,
    level: CareerCompassScoreLevel,
    modifier: Modifier = Modifier,
) {
    val colors = CareerCompassTheme.colors
    val gaugeDescription =
        stringResource(
            R.string.feed_posting_detail_suitability_content_description,
            score,
            levelLabel,
        )

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(CareerCompassTheme.spacing.medium),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = score.toString(),
                modifier = Modifier.alignByBaseline(),
                color = colors.primary,
                style =
                    CareerCompassTheme.typography.displayLarge.copy(
                        fontSize = 44.sp,
                        lineHeight = 52.sp,
                    ),
            )
            Text(
                text = stringResource(R.string.feed_posting_detail_score_max),
                modifier =
                    Modifier
                        .alignByBaseline()
                        .padding(start = 4.dp),
                color = colors.mutedContent,
                style = CareerCompassTheme.typography.bodyLarge,
            )
            Spacer(modifier = Modifier.weight(1f))
            CareerCompassBadge(
                label = levelLabel,
                tone = level.badgeTone(),
            )
        }
        LinearProgressIndicator(
            progress = { score / MAX_SCORE },
            modifier =
                Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(CareerCompassTheme.shapes.pill)
                    .semantics { contentDescription = gaugeDescription },
            color = colors.primary,
            trackColor = colors.surfaceVariant,
            strokeCap = StrokeCap.Round,
            gapSize = 0.dp,
            drawStopIndicator = {},
        )
    }
}

private const val MAX_SCORE = 100f
