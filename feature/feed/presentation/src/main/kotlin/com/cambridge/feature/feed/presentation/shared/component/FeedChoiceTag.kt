package com.cambridge.feature.feed.presentation.shared.component

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import com.cambridge.feature.feed.presentation.R
import com.careercompass.core.ui.component.CareerCompassTag

/** [CareerCompassTag] with the feed's localized selected/unselected state descriptions. */
@Composable
internal fun FeedChoiceTag(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    role: Role,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    CareerCompassTag(
        label = label,
        selected = selected,
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
        stateDescription =
            stringResource(
                if (selected) {
                    R.string.feed_filter_selected_state
                } else {
                    R.string.feed_filter_unselected_state
                },
            ),
        role = role,
    )
}
