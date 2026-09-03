package com.cambridge.core.ui.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cambridge.core.ui.theme.CareerCompassTheme

/** Visual emphasis levels for [CareerCompassScoreChip]. */
public enum class CareerCompassScoreLevel {
    High,
    Mid,
    Low,
}

/** Displays a score from 0 through 100 with a single accessibility description. */
@Composable
public fun CareerCompassScoreChip(
    label: String,
    score: Int,
    level: CareerCompassScoreLevel,
    modifier: Modifier = Modifier,
    contentDescription: String = "$label $score",
) {
    require(score in SCORE_RANGE) {
        "score must be between ${SCORE_RANGE.first} and ${SCORE_RANGE.last}: $score"
    }

    val colors = CareerCompassTheme.colors
    val scoreColors =
        when (level) {
            CareerCompassScoreLevel.High -> {
                ScoreColors(
                    container = colors.primaryContainer,
                    label = colors.mutedContent,
                    score = colors.onSuccessContainer,
                )
            }

            CareerCompassScoreLevel.Mid -> {
                ScoreColors(
                    container = colors.surfaceVariant,
                    label = colors.onSurfaceVariant,
                    score = colors.onSurface,
                )
            }

            CareerCompassScoreLevel.Low -> {
                ScoreColors(
                    container = colors.subtleSurface,
                    label = colors.mutedContent,
                    score = colors.mutedContent,
                )
            }
        }

    Surface(
        modifier =
            modifier.semantics(mergeDescendants = true) {
                this.contentDescription = contentDescription
            },
        shape = CareerCompassTheme.shapes.pill,
        color = scoreColors.container,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = label,
                modifier = Modifier.clearAndSetSemantics {},
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                color = scoreColors.label,
                style =
                    CareerCompassTheme.typography.caption.copy(
                        lineHeight = 16.5.sp,
                        fontWeight = FontWeight.Medium,
                    ),
            )
            Text(
                text = score.toString(),
                modifier = Modifier.clearAndSetSemantics {},
                maxLines = 1,
                color = scoreColors.score,
                style =
                    CareerCompassTheme.typography.labelMedium.copy(
                        fontSize = 13.sp,
                        lineHeight = 19.5.sp,
                        fontWeight = FontWeight.Bold,
                    ),
            )
        }
    }
}

private data class ScoreColors(
    val container: Color,
    val label: Color,
    val score: Color,
)

private val SCORE_RANGE = 0..100
