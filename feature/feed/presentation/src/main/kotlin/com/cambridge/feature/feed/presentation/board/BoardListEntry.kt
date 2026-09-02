package com.cambridge.feature.feed.presentation.board

import android.content.res.Resources
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.stringResource
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.cambridge.core.model.board.MAX_BOARDS
import com.cambridge.core.ui.component.CareerCompassButton
import com.cambridge.core.ui.component.CareerCompassButtonSize
import com.cambridge.core.ui.component.CareerCompassButtonVariant
import com.cambridge.core.ui.component.CareerCompassEmptyState
import com.cambridge.core.ui.component.CareerCompassNetworkErrorState
import com.cambridge.core.ui.theme.CareerCompassTheme
import com.cambridge.feature.feed.presentation.R
import com.cambridge.feature.feed.presentation.shared.component.FeedTopBar
import com.cambridge.feature.feed.presentation.shared.util.toBoardUiModel
import kotlinx.coroutines.launch
import java.time.Clock

/** 내 게시판 진입점 — 삭제는 확인 다이얼로그를 거치고, 화면에 돌아올 때마다 목록을 다시 읽는다. */
@Composable
public fun BoardListEntry(
    onBackClick: () -> Unit,
    onAddBoardClick: () -> Unit,
    onSessionEnded: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: BoardListViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val resources = LocalResources.current
    val snackbarHostState = remember { SnackbarHostState() }
    val snackbarScope = rememberCoroutineScope()

    LifecycleResumeEffect(Unit) {
        viewModel.refresh()
        onPauseOrDispose { }
    }
    val pendingNavigation = state.pendingNavigation
    LaunchedEffect(pendingNavigation) {
        if (pendingNavigation == null) return@LaunchedEffect
        viewModel.onNavigationConsumed()
        when (pendingNavigation) {
            BoardListDestination.Back -> onBackClick()
            BoardListDestination.Register -> onAddBoardClick()
        }
    }
    val sessionEnded = state.sessionEnded
    LaunchedEffect(sessionEnded) {
        if (sessionEnded) {
            viewModel.onSessionEndedConsumed()
            onSessionEnded()
        }
    }
    val message = state.message
    LaunchedEffect(message) {
        if (message == null) return@LaunchedEffect
        viewModel.onMessageConsumed()
        snackbarScope.launch { snackbarHostState.showSnackbar(resources.getString(message.messageRes())) }
    }

    Box(modifier = modifier.fillMaxSize()) {
        when (val loadState = state.loadState) {
            is BoardListLoadState.Failed -> {
                BoardListErrorChrome(onBackClick = { viewModel.onEvent(BoardListEvent.BackClicked) }) {
                    if (loadState.isNetworkUnavailable) {
                        CareerCompassNetworkErrorState(
                            onRetryClick = viewModel::retryLoad,
                            onOfflineClick = null,
                        )
                    } else {
                        CareerCompassEmptyState(
                            title = stringResource(R.string.feed_board_list_error_title),
                            description = stringResource(R.string.feed_board_list_error_description),
                            actionText = stringResource(R.string.feed_board_list_error_retry),
                            onActionClick = viewModel::retryLoad,
                        )
                    }
                }
            }

            BoardListLoadState.Loading,
            is BoardListLoadState.Loaded,
            -> {
                val uiState = remember(loadState, resources) { loadState.toUiState(resources, viewModel.clock) }
                BoardListScreen(
                    state = uiState,
                    onEvent = viewModel::onEvent,
                )
            }
        }
        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.align(Alignment.BottomCenter),
        )
    }

    val pendingDeletion = state.pendingDeletion
    if (pendingDeletion != null) {
        AlertDialog(
            onDismissRequest = viewModel::dismissDelete,
            confirmButton = {
                CareerCompassButton(
                    text = stringResource(R.string.feed_board_delete_dialog_confirm),
                    onClick = viewModel::confirmDelete,
                    variant = CareerCompassButtonVariant.Danger,
                    size = CareerCompassButtonSize.Small,
                )
            },
            dismissButton = {
                CareerCompassButton(
                    text = stringResource(R.string.feed_board_delete_dialog_cancel),
                    onClick = viewModel::dismissDelete,
                    variant = CareerCompassButtonVariant.Ghost,
                    size = CareerCompassButtonSize.Small,
                )
            },
            title = { Text(text = stringResource(R.string.feed_board_delete_dialog_title)) },
            text = { Text(text = stringResource(R.string.feed_board_delete_dialog_message, pendingDeletion.name)) },
            containerColor = CareerCompassTheme.colors.surface,
            titleContentColor = CareerCompassTheme.colors.onSurface,
            textContentColor = CareerCompassTheme.colors.onSurfaceVariant,
        )
    }
}

@Composable
private fun BoardListErrorChrome(
    onBackClick: () -> Unit,
    content: @Composable () -> Unit,
) {
    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .background(CareerCompassTheme.colors.subtleSurface)
                .windowInsetsPadding(WindowInsets.safeDrawing),
    ) {
        FeedTopBar(
            title = stringResource(R.string.feed_board_list_title),
            onBackClick = onBackClick,
            actions = null,
        )
        content()
    }
}

internal fun BoardListLoadState.toUiState(
    resources: Resources,
    clock: Clock,
): BoardListUiState =
    BoardListUiState(
        content =
            when (this) {
                is BoardListLoadState.Loaded -> {
                    if (boards.isEmpty()) {
                        BoardListContentState.Empty
                    } else {
                        BoardListContentState.Loaded(boards.distinctBy { it.id }.map { it.toBoardUiModel(resources, clock) })
                    }
                }

                BoardListLoadState.Loading,
                is BoardListLoadState.Failed,
                -> {
                    BoardListContentState.Loading
                }
            },
        maxBoardCount = MAX_BOARDS,
    )

private fun BoardListMessage.messageRes(): Int =
    when (this) {
        BoardListMessage.ToggleFailed -> R.string.feed_board_toggle_failed
        BoardListMessage.RetryFailed -> R.string.feed_board_retry_failed
        BoardListMessage.RetryRequested -> R.string.feed_board_retry_requested
        BoardListMessage.DeleteFailed -> R.string.feed_board_delete_failed
    }
