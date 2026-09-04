package com.cambridge.feature.feed.presentation.shared.component

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cambridge.feature.feed.presentation.R
import com.careercompass.core.ui.icon.CareerCompassIcons
import com.careercompass.core.ui.theme.CareerCompassTheme

/**
 * Top bar shared by the feed detail screens: a 48dp back target, a heading, and optional actions.
 *
 * Pass `null` for [actions] when the screen has no trailing controls.
 */
@Composable
internal fun FeedTopBar(
    title: String,
    onBackClick: () -> Unit,
    actions: (@Composable RowScope.() -> Unit)?,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .height(56.dp)
                .padding(horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        FeedIconButton(
            icon = CareerCompassIcons.ArrowBack,
            contentDescription = stringResource(R.string.feed_back),
            onClick = onBackClick,
        )
        Text(
            text = title,
            modifier =
                Modifier
                    .weight(1f)
                    .padding(horizontal = 4.dp)
                    .semantics { heading() },
            color = CareerCompassTheme.colors.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            style =
                CareerCompassTheme.typography.headline4.copy(
                    fontSize = 18.sp,
                    lineHeight = 26.sp,
                ),
        )
        if (actions != null) {
            actions()
        }
    }
}
