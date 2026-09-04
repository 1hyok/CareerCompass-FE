package com.careercompass.core.ui.component

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.careercompass.core.ui.theme.CareerCompassTheme

/**
 * A bordered CareerCompass surface for grouping related content.
 *
 * Supplying [onClick] makes the entire card a button. When it is `null`, the card exposes no click
 * semantics.
 */
@Composable
public fun CareerCompassCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    contentPadding: PaddingValues = PaddingValues(16.dp),
    content: @Composable ColumnScope.() -> Unit,
) {
    val colors = CareerCompassTheme.colors
    val shape = CareerCompassTheme.shapes.card
    val clickModifier =
        if (onClick != null) {
            Modifier
                .clip(shape)
                .clickable(
                    role = Role.Button,
                    onClick = onClick,
                )
        } else {
            Modifier
        }

    Surface(
        modifier = modifier.then(clickModifier),
        shape = shape,
        color = colors.surface,
        contentColor = colors.onSurface,
        border = BorderStroke(1.dp, colors.outline),
    ) {
        Column(
            modifier = Modifier.padding(contentPadding),
            content = content,
        )
    }
}
