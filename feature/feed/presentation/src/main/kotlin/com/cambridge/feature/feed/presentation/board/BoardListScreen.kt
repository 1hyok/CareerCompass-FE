package com.cambridge.feature.feed.presentation.board

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cambridge.core.ui.component.CareerCompassBadge
import com.cambridge.core.ui.component.CareerCompassBadgeTone
import com.cambridge.core.ui.component.CareerCompassButton
import com.cambridge.core.ui.component.CareerCompassButtonSize
import com.cambridge.core.ui.component.CareerCompassButtonVariant
import com.cambridge.core.ui.theme.CareerCompassTheme
import com.cambridge.feature.feed.presentation.R
import com.cambridge.feature.feed.presentation.shared.component.FeedCard
import com.cambridge.feature.feed.presentation.shared.component.FeedLoadingContent
import com.cambridge.feature.feed.presentation.shared.component.FeedTopBar

/** Stateless list of registered boards with per-board activation, retry, and delete actions (spec F2-1/F2-2). */
@Composable
public fun BoardListScreen(
    state: BoardListUiState,
    onEvent: (BoardListEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier =
            modifier
                .fillMaxSize()
                .background(CareerCompassTheme.colors.subtleSurface)
                .windowInsetsPadding(WindowInsets.safeDrawing),
    ) {
        FeedTopBar(
            title = stringResource(R.string.feed_board_list_title),
            onBackClick = { onEvent(BoardListEvent.BackClicked) },
            actions = {
                CareerCompassButton(
                    text = stringResource(R.string.feed_board_list_add),
                    onClick = { onEvent(BoardListEvent.AddBoardClicked) },
                    variant = CareerCompassButtonVariant.Ghost,
                    size = CareerCompassButtonSize.Small,
                    contentDescription = stringResource(R.string.feed_board_list_add_content_description),
                )
            },
        )
        when (val content = state.content) {
            BoardListContentState.Loading -> {
                FeedLoadingContent(
                    message = stringResource(R.string.feed_board_list_loading),
                    modifier = Modifier.weight(1f),
                )
            }

            BoardListContentState.Empty -> {
                BoardListEmpty(
                    onAddBoardClick = { onEvent(BoardListEvent.AddBoardClicked) },
                    modifier = Modifier.weight(1f),
                )
            }

            is BoardListContentState.Loaded -> {
                BoardList(
                    boards = content.boards,
                    maxBoardCount = state.maxBoardCount,
                    onEvent = onEvent,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
private fun BoardList(
    boards: List<BoardUiModel>,
    maxBoardCount: Int,
    onEvent: (BoardListEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    val spacing = CareerCompassTheme.spacing

    LazyColumn(
        modifier = modifier.fillMaxWidth(),
        contentPadding =
            PaddingValues(
                start = spacing.large,
                end = spacing.large,
                bottom = spacing.large,
            ),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        item(key = "count") {
            Text(
                text = stringResource(R.string.feed_board_list_count, boards.size, maxBoardCount),
                modifier = Modifier.padding(vertical = spacing.xSmall),
                color = CareerCompassTheme.colors.onSurfaceVariant,
                style =
                    CareerCompassTheme.typography.caption.copy(
                        fontSize = 12.sp,
                        lineHeight = 18.sp,
                        fontWeight = FontWeight.SemiBold,
                    ),
            )
        }
        items(items = boards, key = BoardUiModel::id) { board ->
            BoardCard(board = board, onEvent = onEvent)
        }
    }
}

@Composable
private fun BoardCard(
    board: BoardUiModel,
    onEvent: (BoardListEvent) -> Unit,
) {
    val colors = CareerCompassTheme.colors
    val spacing = CareerCompassTheme.spacing
    val toggleDescription = stringResource(R.string.feed_board_toggle_content_description, board.name)

    FeedCard(onClick = { onEvent(BoardListEvent.BoardSelected(board.id)) }) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                Text(
                    text = board.name,
                    color = colors.onSurface,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    style = CareerCompassTheme.typography.headline4,
                )
                Text(
                    text = board.url,
                    color = colors.mutedContent,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style =
                        CareerCompassTheme.typography.caption.copy(
                            fontSize = 12.sp,
                            lineHeight = 18.sp,
                        ),
                )
            }
            Spacer(modifier = Modifier.width(spacing.small))
            Switch(
                checked = board.isActive,
                onCheckedChange = { onEvent(BoardListEvent.BoardToggled(board.id)) },
                modifier = Modifier.semantics { contentDescription = toggleDescription },
            )
        }
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(spacing.xSmall),
            verticalArrangement = Arrangement.spacedBy(spacing.xSmall),
        ) {
            CareerCompassBadge(
                label = board.typeLabel,
                tone = board.type.badgeTone(),
            )
            BoardStatusBadge(status = board.status, failCount = board.failCount)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(spacing.small)) {
            Text(
                text =
                    board.lastCollectedLabel?.let { label ->
                        stringResource(R.string.feed_board_last_collected, label)
                    } ?: stringResource(R.string.feed_board_never_collected),
                color = colors.onSurfaceVariant,
                style = CareerCompassTheme.typography.caption,
            )
            board.postingCount?.let { postingCount ->
                Text(
                    text = stringResource(R.string.feed_board_posting_count, postingCount),
                    color = colors.onSurfaceVariant,
                    style = CareerCompassTheme.typography.caption,
                )
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(spacing.small)) {
            if (board.status == BoardStatus.Failing) {
                CareerCompassButton(
                    text = stringResource(R.string.feed_board_retry),
                    onClick = { onEvent(BoardListEvent.RetryClicked(board.id)) },
                    variant = CareerCompassButtonVariant.Secondary,
                    size = CareerCompassButtonSize.Small,
                    contentDescription = stringResource(R.string.feed_board_retry_content_description, board.name),
                )
            }
            CareerCompassButton(
                text = stringResource(R.string.feed_board_delete),
                onClick = { onEvent(BoardListEvent.DeleteClicked(board.id)) },
                variant = CareerCompassButtonVariant.Danger,
                size = CareerCompassButtonSize.Small,
                contentDescription = stringResource(R.string.feed_board_delete_content_description, board.name),
            )
        }
    }
}

@Composable
private fun BoardStatusBadge(
    status: BoardStatus,
    failCount: Int,
) {
    when (status) {
        BoardStatus.Active -> {
            CareerCompassBadge(
                label = stringResource(R.string.feed_board_status_active),
                tone = CareerCompassBadgeTone.Brand,
            )
        }

        BoardStatus.Paused -> {
            CareerCompassBadge(
                label = stringResource(R.string.feed_board_status_paused),
                tone = CareerCompassBadgeTone.Neutral,
            )
        }

        BoardStatus.Failing -> {
            CareerCompassBadge(
                label = stringResource(R.string.feed_board_status_failing, failCount),
                tone = CareerCompassBadgeTone.Error,
            )
        }
    }
}

@Composable
private fun BoardListEmpty(
    onAddBoardClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = CareerCompassTheme.colors
    val spacing = CareerCompassTheme.spacing

    Box(modifier = modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        Column(
            modifier = Modifier.padding(spacing.xxLarge),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(spacing.small),
        ) {
            Text(
                text = stringResource(R.string.feed_icon_board_empty),
                modifier = Modifier.clearAndSetSemantics {},
                style = CareerCompassTheme.typography.displayLarge.copy(fontSize = 40.sp, lineHeight = 48.sp),
            )
            Text(
                text = stringResource(R.string.feed_board_list_empty_title),
                color = colors.onSurface,
                textAlign = TextAlign.Center,
                style = CareerCompassTheme.typography.headline4,
            )
            Text(
                text = stringResource(R.string.feed_board_list_empty_description),
                color = colors.mutedContent,
                textAlign = TextAlign.Center,
                style = CareerCompassTheme.typography.bodyMedium,
            )
            Spacer(modifier = Modifier.height(spacing.small))
            CareerCompassButton(
                text = stringResource(R.string.feed_board_list_empty_action),
                onClick = onAddBoardClick,
            )
        }
    }
}
