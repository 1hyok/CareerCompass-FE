package com.cambridge.feature.feed.presentation.postingdetail.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.cambridge.core.ui.theme.CareerCompassTheme
import com.cambridge.feature.feed.presentation.R
import com.cambridge.feature.feed.presentation.postingdetail.SuitabilityAxisUiModel
import com.cambridge.feature.feed.presentation.shared.component.HIGH_SCORE_THRESHOLD

/** One analysis axis: label, weight caption, score, and a thin score bar (spec F3-2). */
@Composable
internal fun SuitabilityBreakdownRow(
    axis: SuitabilityAxisUiModel,
    modifier: Modifier = Modifier,
) {
    val colors = CareerCompassTheme.colors
    val spacing = CareerCompassTheme.spacing
    val axisDescription =
        stringResource(
            R.string.feed_posting_detail_axis_content_description,
            axis.label,
            axis.weightLabel,
            axis.score,
        )
    val barColor = if (axis.score >= HIGH_SCORE_THRESHOLD) colors.primary else colors.inverseSurface

    Column(
        modifier =
            modifier
                .fillMaxWidth()
                .semantics(mergeDescendants = true) { contentDescription = axisDescription },
        verticalArrangement = Arrangement.spacedBy(spacing.xSmall),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = axis.label,
                color = colors.onSurface,
                style = CareerCompassTheme.typography.bodyMedium,
            )
            Spacer(modifier = Modifier.width(spacing.xxSmall))
            Text(
                text = axis.weightLabel,
                color = colors.mutedContent,
                style = CareerCompassTheme.typography.caption,
            )
            Spacer(modifier = Modifier.weight(1f))
            Text(
                text = axis.score.toString(),
                color = barColor,
                style =
                    CareerCompassTheme.typography.labelMedium.copy(
                        fontWeight = FontWeight.Bold,
                    ),
            )
        }
        Box(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(CareerCompassTheme.shapes.pill)
                    .background(colors.surfaceVariant),
        ) {
            Box(
                modifier =
                    Modifier
                        .fillMaxWidth(fraction = axis.score / MAX_SCORE)
                        .fillMaxHeight()
                        .background(barColor, CareerCompassTheme.shapes.pill),
            )
        }
    }
}

private const val MAX_SCORE = 100f
