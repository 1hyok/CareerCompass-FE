package com.cambridge.feature.feed.presentation.shared.component

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cambridge.core.ui.theme.CareerCompassTheme

/** A 48dp glyph button. [contentDescription] is the only accessible name, so it must be non-blank. */
@Composable
internal fun FeedIconButton(
    icon: String,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    tint: Color = CareerCompassTheme.colors.onSurface,
) {
    require(contentDescription.isNotBlank()) { "contentDescription must not be blank" }

    Box(
        modifier =
            modifier
                .size(48.dp)
                .clickable(role = Role.Button, onClick = onClick)
                .semantics {
                    this.contentDescription = contentDescription
                    role = Role.Button
                },
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = icon,
            modifier = Modifier.clearAndSetSemantics {},
            color = tint,
            style =
                CareerCompassTheme.typography.headline2.copy(
                    fontSize = 22.sp,
                    lineHeight = 33.sp,
                    fontWeight = FontWeight.Normal,
                ),
        )
    }
}
