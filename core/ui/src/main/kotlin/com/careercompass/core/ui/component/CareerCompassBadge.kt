package com.careercompass.core.ui.component

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.careercompass.core.ui.theme.CareerCompassTheme

/** Semantic color variants for [CareerCompassBadge]. */
public enum class CareerCompassBadgeTone {
    Brand,
    Neutral,
    Warning,
    Error,
    Info,
    Dark,
}

/** A compact, non-interactive label used to communicate status or category. */
@Composable
public fun CareerCompassBadge(
    label: String,
    tone: CareerCompassBadgeTone,
    modifier: Modifier = Modifier,
) {
    val colors = CareerCompassTheme.colors
    val badgeColors =
        when (tone) {
            CareerCompassBadgeTone.Brand -> {
                BadgeColors(
                    container = colors.successContainer,
                    content = colors.onSuccessContainer,
                )
            }

            CareerCompassBadgeTone.Neutral -> {
                BadgeColors(
                    container = colors.surfaceVariant,
                    content = colors.onSurfaceVariant,
                )
            }

            CareerCompassBadgeTone.Warning -> {
                BadgeColors(
                    container = colors.warningContainer,
                    content = colors.onWarningContainer,
                )
            }

            CareerCompassBadgeTone.Error -> {
                BadgeColors(
                    container = colors.errorContainer,
                    content = colors.onErrorContainer,
                )
            }

            CareerCompassBadgeTone.Info -> {
                BadgeColors(
                    container = colors.infoContainer,
                    content = colors.onInfoContainer,
                )
            }

            CareerCompassBadgeTone.Dark -> {
                BadgeColors(
                    container = colors.inverseSurface,
                    content = colors.inverseOnSurface,
                )
            }
        }

    Surface(
        modifier = modifier.semantics(mergeDescendants = true) {},
        shape = CareerCompassTheme.shapes.pill,
        color = badgeColors.container,
        contentColor = badgeColors.content,
    ) {
        Text(
            text = label,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            style =
                CareerCompassTheme.typography.caption.copy(
                    lineHeight = 16.5.sp,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 0.2.sp,
                ),
        )
    }
}

private data class BadgeColors(
    val container: Color,
    val content: Color,
)
