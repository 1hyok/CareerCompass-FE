package com.cambridge.core.ui.component

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cambridge.core.ui.theme.CareerCompassTheme

/**
 * A selectable filter tag with a selected and disabled state.
 *
 * [label] must be non-blank. When provided, [stateDescription] must also be non-blank.
 *
 * [stateDescription] overrides the platform checkbox wording only when the caller supplies a
 * localized value. Use [role] to match the caller's selection model: checkbox for independent
 * filters and radio button for a mutually exclusive group.
 */
@Composable
public fun CareerCompassTag(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    stateDescription: String? = null,
    role: Role = Role.Checkbox,
) {
    require(label.isNotBlank()) { "label must not be blank" }
    require(stateDescription == null || stateDescription.isNotBlank()) {
        "stateDescription must be null or non-blank"
    }

    val colors = CareerCompassTheme.colors
    val tagColors =
        when {
            !enabled -> {
                TagColors(
                    container = colors.disabledContainer,
                    content = colors.disabledContent,
                    border = colors.subtleOutline,
                )
            }

            selected -> {
                TagColors(
                    container = colors.inverseSurface,
                    content = colors.inverseOnSurface,
                    border = colors.inverseSurface,
                )
            }

            else -> {
                TagColors(
                    container = colors.surface,
                    content = colors.onSurface,
                    border = colors.interactiveOutline,
                )
            }
        }

    Surface(
        modifier =
            modifier
                .semantics {
                    stateDescription?.let { localizedDescription ->
                        this.stateDescription = localizedDescription
                    }
                }.toggleable(
                    value = selected,
                    enabled = enabled,
                    role = role,
                    onValueChange = { onClick() },
                ),
        shape = CareerCompassTheme.shapes.pill,
        color = tagColors.container,
        contentColor = tagColors.content,
        border = BorderStroke(width = 1.dp, color = tagColors.border),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (selected) {
                Text(
                    text = CHECK_MARK,
                    modifier = Modifier.clearAndSetSemantics {},
                    maxLines = 1,
                    style =
                        CareerCompassTheme.typography.caption.copy(
                            fontWeight = FontWeight.Bold,
                        ),
                )
            }
            Text(
                text = label,
                maxLines = 1,
                style =
                    CareerCompassTheme.typography.labelMedium.copy(
                        fontSize = 13.sp,
                        lineHeight = 19.5.sp,
                        fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
                    ),
            )
        }
    }
}

private data class TagColors(
    val container: Color,
    val content: Color,
    val border: Color,
)

private const val CHECK_MARK = "✓"
