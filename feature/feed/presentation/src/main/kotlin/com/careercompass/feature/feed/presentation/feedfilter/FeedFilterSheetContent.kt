package com.careercompass.feature.feed.presentation.feedfilter

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
import com.careercompass.core.ui.component.CareerCompassButton
import com.careercompass.core.ui.component.CareerCompassButtonSize
import com.careercompass.core.ui.component.CareerCompassButtonVariant
import com.careercompass.core.ui.icon.CareerCompassIcons
import com.careercompass.core.ui.theme.CareerCompassTheme
import com.careercompass.feature.feed.presentation.R
import com.careercompass.feature.feed.presentation.shared.component.FeedChoiceTag
import com.careercompass.feature.feed.presentation.shared.component.FeedIconButton
import com.careercompass.feature.feed.presentation.shared.component.FeedSectionTitle

/**
 * Body of the feed filter bottom sheet (spec F2-3).
 *
 * The caller wraps this in a `ModalBottomSheet`; keeping the sheet chrome outside makes the content
 * testable and previewable on its own.
 */
@Composable
internal fun FeedFilterSheetContent(
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
                icon = CareerCompassIcons.Close,
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
                // 목록이 비어 있어도 조건에 남은 게시판이 있으면 그것만은 그린다 — 그 태그가 조건을 끄는
                // 유일한 손잡이라, 「등록된 게시판이 없어요」 한 줄로 덮으면 다시 끌 수 없는 조건이 된다.
                if (state.boards.isEmpty() && state.missingBoards == null) {
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
                        state.missingBoards?.let { missing ->
                            FeedChoiceTag(
                                label = stringResource(missing.labelRes(), missing.count),
                                selected = true,
                                onClick = { onEvent(FeedFilterEvent.MissingBoardsCleared) },
                                role = Role.Checkbox,
                            )
                        }
                    }
                    // 왜 이름 없는 태그가 켜져 있는지는 태그 혼자 말하지 못한다 — 이유와 끄는 방법을 붙인다.
                    state.missingBoards?.let { missing ->
                        Text(
                            text = stringResource(missing.noticeRes()),
                            color = colors.mutedContent,
                            style = CareerCompassTheme.typography.caption,
                        )
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
                // 「직접 지정」일 때만 그린다 — 프리셋과 배타적이라 함께 보이면 무엇이 걸렸는지 흐려진다.
                state.deadlineRange?.let { range ->
                    FeedDeadlineRangeEditor(range = range, onEvent = onEvent)
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
                enabled = state.isApplyEnabled,
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

private fun FeedMissingBoardsUiModel.labelRes(): Int =
    when (reason) {
        FeedMissingBoardsReason.Deleted -> R.string.feed_filter_board_missing_deleted
        FeedMissingBoardsReason.Unverified -> R.string.feed_filter_board_missing_unverified
    }

private fun FeedMissingBoardsUiModel.noticeRes(): Int =
    when (reason) {
        FeedMissingBoardsReason.Deleted -> R.string.feed_filter_board_missing_deleted_notice
        FeedMissingBoardsReason.Unverified -> R.string.feed_filter_board_missing_unverified_notice
    }

private fun FeedDeadlineFilter.labelRes(): Int =
    when (this) {
        FeedDeadlineFilter.All -> R.string.feed_filter_deadline_all
        FeedDeadlineFilter.WithinWeek -> R.string.feed_filter_deadline_within_week
        FeedDeadlineFilter.WithinMonth -> R.string.feed_filter_deadline_within_month
        FeedDeadlineFilter.IncludeExpired -> R.string.feed_filter_deadline_include_expired
        FeedDeadlineFilter.Range -> R.string.feed_filter_deadline_range
    }

private fun FeedMinScoreFilter.labelRes(): Int =
    when (this) {
        FeedMinScoreFilter.All -> R.string.feed_filter_min_score_all
        FeedMinScoreFilter.AtLeast60 -> R.string.feed_filter_min_score_60
        FeedMinScoreFilter.AtLeast80 -> R.string.feed_filter_min_score_80
    }
