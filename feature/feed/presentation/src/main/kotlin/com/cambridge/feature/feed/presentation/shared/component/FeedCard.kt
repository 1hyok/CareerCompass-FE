package com.cambridge.feature.feed.presentation.shared.component

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.cambridge.core.ui.theme.CareerCompassTheme

/**
 * White 16dp-radius card with a hairline border, matching the feed listing card.
 *
 * Pass `null` for [onClick] to render a static card; a non-null handler makes the whole card a button.
 */
@Composable
internal fun FeedCard(
    onClick: (() -> Unit)?,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    val colors = CareerCompassTheme.colors
    val shape = RoundedCornerShape(16.dp)

    Surface(
        modifier =
            modifier
                .fillMaxWidth()
                .clip(shape)
                .then(
                    if (onClick != null) {
                        Modifier.clickable(role = Role.Button, onClick = onClick)
                    } else {
                        Modifier
                    },
                ),
        shape = shape,
        color = colors.surface,
        border = BorderStroke(width = 1.dp, color = colors.subtleOutline),
    ) {
        Column(
            modifier = Modifier.padding(CareerCompassTheme.spacing.large),
            verticalArrangement = Arrangement.spacedBy(CareerCompassTheme.spacing.medium),
            content = content,
        )
    }
}
