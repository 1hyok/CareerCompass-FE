package com.cambridge.core.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.cambridge.core.ui.R
import com.cambridge.core.ui.theme.CareerCompassTheme

/**
 * CareerCompass top application bar with optional navigation, subtitle, and actions.
 *
 * [title] must be non-blank. When provided, [subtitle] must also be non-blank.
 * Passing `null` for [onBackClick] removes the back control from the composition.
 */
@Composable
public fun CareerCompassTopAppBar(
    title: String,
    onBackClick: (() -> Unit)?,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    actions: (@Composable RowScope.() -> Unit)? = null,
) {
    require(title.isNotBlank()) { "title must not be blank" }
    require(subtitle == null || subtitle.isNotBlank()) {
        "subtitle must be null or non-blank"
    }

    val colors = CareerCompassTheme.colors
    val spacing = CareerCompassTheme.spacing

    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .height(56.dp)
                .background(colors.subtleSurface),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (onBackClick != null) {
            BackButton(onClick = onBackClick)
        }

        Column(
            modifier =
                Modifier
                    .weight(1f)
                    .padding(
                        start = if (onBackClick == null) spacing.large else spacing.xxSmall,
                        end = spacing.small,
                    ),
        ) {
            Text(
                text = title,
                color = colors.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                style = CareerCompassTheme.typography.headline4,
            )
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    color = colors.mutedContent,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = CareerCompassTheme.typography.caption,
                )
            }
        }

        if (actions != null) {
            CompositionLocalProvider(LocalContentColor provides colors.onSurface) {
                Row(
                    modifier = Modifier.padding(end = spacing.xxSmall),
                    verticalAlignment = Alignment.CenterVertically,
                    content = actions,
                )
            }
        }
    }
}

@Composable
private fun BackButton(onClick: () -> Unit) {
    val colors = CareerCompassTheme.colors
    val spacing = CareerCompassTheme.spacing
    val backDescription = stringResource(R.string.core_ui_back)

    Box(
        modifier =
            Modifier
                .padding(start = spacing.xxSmall)
                .size(48.dp)
                .clickable(
                    role = Role.Button,
                    onClick = onClick,
                ).semantics {
                    contentDescription = backDescription
                    role = Role.Button
                },
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = stringResource(R.string.core_ui_back_icon),
            modifier = Modifier.clearAndSetSemantics {},
            color = colors.onSurface,
            style = CareerCompassTheme.typography.headline2,
        )
    }
}
