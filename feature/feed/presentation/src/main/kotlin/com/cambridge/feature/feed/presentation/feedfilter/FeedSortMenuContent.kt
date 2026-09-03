package com.cambridge.feature.feed.presentation.feedfilter

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.cambridge.core.ui.icon.CareerCompassIcons
import com.cambridge.core.ui.theme.CareerCompassTheme
import com.cambridge.feature.feed.presentation.R
import com.cambridge.feature.feed.presentation.shared.component.FEED_ICON_SIZE
import com.cambridge.feature.feed.presentation.shared.component.FeedIconButton

/** Radio list of the feed sort orders (spec F2-3). The host decides how it is anchored. */
@Composable
public fun FeedSortMenuContent(
    state: FeedSortMenuUiState,
    onEvent: (FeedSortMenuEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = CareerCompassTheme.colors
    val spacing = CareerCompassTheme.spacing

    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(start = spacing.large, end = spacing.xxSmall),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(R.string.feed_sort_title),
                modifier =
                    Modifier
                        .weight(1f)
                        .semantics { heading() },
                color = colors.onSurface,
                style = CareerCompassTheme.typography.headline4,
            )
            FeedIconButton(
                icon = CareerCompassIcons.Close,
                contentDescription = stringResource(R.string.feed_sort_close),
                onClick = { onEvent(FeedSortMenuEvent.DismissClicked) },
            )
        }
        Column(modifier = Modifier.selectableGroup()) {
            FeedSortOption.entries.forEach { option ->
                FeedSortOptionRow(
                    option = option,
                    selected = option == state.selected,
                    onClick = { onEvent(FeedSortMenuEvent.SortSelected(option)) },
                )
            }
        }
    }
}

@Composable
private fun FeedSortOptionRow(
    option: FeedSortOption,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val colors = CareerCompassTheme.colors
    val spacing = CareerCompassTheme.spacing
    val optionStateDescription =
        stringResource(
            if (selected) {
                R.string.feed_filter_selected_state
            } else {
                R.string.feed_filter_unselected_state
            },
        )

    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .heightIn(min = 48.dp)
                .semantics { stateDescription = optionStateDescription }
                .selectable(
                    selected = selected,
                    role = Role.RadioButton,
                    onClick = onClick,
                ).padding(horizontal = spacing.large, vertical = spacing.medium),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = stringResource(option.labelRes()),
            color = if (selected) colors.primaryEmphasis else colors.onSurface,
            style =
                CareerCompassTheme.typography.bodyLarge.copy(
                    fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                ),
        )
        if (selected) {
            Icon(
                imageVector = CareerCompassIcons.Check,
                contentDescription = null,
                modifier = Modifier.size(FEED_ICON_SIZE),
                tint = colors.primaryEmphasis,
            )
        }
    }
}

/** Localized label of each sort order, shared with the feed's sort trigger. */
@StringRes
public fun FeedSortOption.labelRes(): Int =
    when (this) {
        FeedSortOption.CollectedDesc -> R.string.feed_sort_collected_desc
        FeedSortOption.DueAsc -> R.string.feed_sort_due_asc
        FeedSortOption.ScoreDesc -> R.string.feed_sort_score_desc
    }
