package com.cambridge.feature.feed.presentation.feedfilter

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import com.cambridge.core.ui.component.CareerCompassButton
import com.cambridge.core.ui.component.CareerCompassButtonSize
import com.cambridge.core.ui.component.CareerCompassButtonVariant
import com.cambridge.core.ui.theme.CareerCompassTheme
import com.cambridge.feature.feed.presentation.R
import com.cambridge.feature.feed.presentation.shared.component.FeedChoiceTag
import com.cambridge.feature.feed.presentation.shared.component.FeedIconButton
import com.cambridge.feature.feed.presentation.shared.component.FeedSectionTitle

/**
 * Body of the feed filter bottom sheet (spec F2-3).
 *
 * The caller wraps this in a `ModalBottomSheet`; keeping the sheet chrome outside makes the content
 * testable and previewable on its own.
 */
@Composable
public fun FeedFilterSheetContent(
    state: FeedFilterUiState,
    onEvent: (FeedFilterEvent) -> Unit,
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
                text = stringResource(R.string.feed_filter_title),
                modifier =
                    Modifier
                        .weight(1f)
                        .semantics { heading() },
                color = colors.onSurface,
                style = CareerCompassTheme.typography.headline2,
            )
            FeedIconButton(
                icon = stringResource(R.string.feed_icon_close),
                contentDescription = stringResource(R.string.feed_filter_close),
                onClick = { onEvent(FeedFilterEvent.DismissClicked) },
            )
        }
        Column(
            modifier =
                Modifier
                    .weight(weight = 1f, fill = false)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = spacing.large, vertical = spacing.small),
            verticalArrangement = Arrangement.spacedBy(spacing.xLarge),
        ) {
            FeedFilterSection(title = stringResource(R.string.feed_filter_category_title)) {
                FlowRow(
                    modifier = Modifier.selectableGroup(),
                    horizontalArrangement = Arrangement.spacedBy(spacing.xSmall),
                    verticalArrangement = Arrangement.spacedBy(spacing.xSmall),
                ) {
                    state.categories.forEach { filter ->
                        FeedChoiceTag(
                            label = filter.label,
                            selected = filter.category == state.selectedCategory,
                            onClick = { onEvent(FeedFilterEvent.CategorySelected(filter.category)) },
                            role = Role.RadioButton,
                        )
                    }
                }
            }
            FeedFilterSection(title = stringResource(R.string.feed_filter_board_title)) {
                if (state.boards.isEmpty()) {
                    Text(
                        text = stringResource(R.string.feed_filter_board_empty),
                        color = colors.mutedContent,
                        style = CareerCompassTheme.typography.caption,
                    )
                } else {
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(spacing.xSmall),
                        verticalArrangement = Arrangement.spacedBy(spacing.xSmall),
                    ) {
                        state.boards.forEach { board ->
                            FeedChoiceTag(
                                label = board.name,
                                selected = board.id in state.selectedBoardIds,
                                onClick = { onEvent(FeedFilterEvent.BoardToggled(board.id)) },
                                role = Role.Checkbox,
                            )
                        }
                    }
                }
            }
            FeedFilterSection(title = stringResource(R.string.feed_filter_deadline_title)) {
                FlowRow(
                    modifier = Modifier.selectableGroup(),
                    horizontalArrangement = Arrangement.spacedBy(spacing.xSmall),
                    verticalArrangement = Arrangement.spacedBy(spacing.xSmall),
                ) {
                    FeedDeadlineFilter.entries.forEach { deadline ->
                        FeedChoiceTag(
                            label = stringResource(deadline.labelRes()),
                            selected = deadline == state.deadline,
                            onClick = { onEvent(FeedFilterEvent.DeadlineSelected(deadline)) },
                            role = Role.RadioButton,
                        )
                    }
                }
            }
            FeedFilterSection(title = stringResource(R.string.feed_filter_min_score_title)) {
                FlowRow(
                    modifier = Modifier.selectableGroup(),
                    horizontalArrangement = Arrangement.spacedBy(spacing.xSmall),
                    verticalArrangement = Arrangement.spacedBy(spacing.xSmall),
                ) {
                    FeedMinScoreFilter.entries.forEach { minScore ->
                        FeedChoiceTag(
                            label = stringResource(minScore.labelRes()),
                            selected = minScore == state.minScore,
                            onClick = { onEvent(FeedFilterEvent.MinScoreSelected(minScore)) },
                            role = Role.RadioButton,
                        )
                    }
                }
            }
            FeedUnreadOnlyRow(
                unreadOnly = state.unreadOnly,
                onToggle = { onEvent(FeedFilterEvent.UnreadOnlyToggled) },
            )
        }
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(spacing.large),
            horizontalArrangement = Arrangement.spacedBy(spacing.small),
        ) {
            CareerCompassButton(
                text = stringResource(R.string.feed_filter_reset),
                onClick = { onEvent(FeedFilterEvent.ResetClicked) },
                variant = CareerCompassButtonVariant.Ghost,
                size = CareerCompassButtonSize.Large,
            )
            CareerCompassButton(
                text =
                    state.matchingCount?.let { count ->
                        stringResource(R.string.feed_filter_apply_with_count, count)
                    } ?: stringResource(R.string.feed_filter_apply),
                onClick = { onEvent(FeedFilterEvent.ApplyClicked) },
                modifier = Modifier.weight(1f),
                variant = CareerCompassButtonVariant.Primary,
                size = CareerCompassButtonSize.Large,
            )
        }
    }
}

@Composable
private fun FeedFilterSection(
    title: String,
    content: @Composable () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(CareerCompassTheme.spacing.small)) {
        FeedSectionTitle(text = title)
        content()
    }
}

@Composable
private fun FeedUnreadOnlyRow(
    unreadOnly: Boolean,
    onToggle: () -> Unit,
) {
    val label = stringResource(R.string.feed_filter_unread_only)

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            modifier = Modifier.weight(1f),
            color = CareerCompassTheme.colors.onSurface,
            style = CareerCompassTheme.typography.headline4,
        )
        Switch(
            checked = unreadOnly,
            onCheckedChange = { onToggle() },
            modifier = Modifier.semantics { contentDescription = label },
        )
    }
}

private fun FeedDeadlineFilter.labelRes(): Int =
    when (this) {
        FeedDeadlineFilter.All -> R.string.feed_filter_deadline_all
        FeedDeadlineFilter.WithinWeek -> R.string.feed_filter_deadline_within_week
        FeedDeadlineFilter.WithinMonth -> R.string.feed_filter_deadline_within_month
        FeedDeadlineFilter.IncludeExpired -> R.string.feed_filter_deadline_include_expired
    }

private fun FeedMinScoreFilter.labelRes(): Int =
    when (this) {
        FeedMinScoreFilter.All -> R.string.feed_filter_min_score_all
        FeedMinScoreFilter.AtLeast60 -> R.string.feed_filter_min_score_60
        FeedMinScoreFilter.AtLeast70 -> R.string.feed_filter_min_score_70
        FeedMinScoreFilter.AtLeast80 -> R.string.feed_filter_min_score_80
    }
