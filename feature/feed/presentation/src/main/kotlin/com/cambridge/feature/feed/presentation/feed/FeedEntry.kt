package com.cambridge.feature.feed.presentation.feed

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.rememberModalBottomSheetState
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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.cambridge.core.ui.component.CareerCompassEmptyState
import com.cambridge.core.ui.component.CareerCompassNetworkErrorState
import com.cambridge.core.ui.theme.CareerCompassTheme
import com.cambridge.feature.feed.presentation.FeedScreen
import com.cambridge.feature.feed.presentation.R
import com.cambridge.feature.feed.presentation.feedfilter.FeedFilterEvent
import com.cambridge.feature.feed.presentation.feedfilter.FeedFilterSheetContent
import com.cambridge.feature.feed.presentation.feedfilter.FeedSortMenuContent
import com.cambridge.feature.feed.presentation.feedfilter.FeedSortMenuEvent
import com.cambridge.feature.feed.presentation.feedfilter.FeedSortMenuUiState
import com.cambridge.feature.feed.presentation.shared.util.toSortOption
import kotlinx.coroutines.launch

/**
 * 피드 홈 진입점 — [FeedViewModel] 상태를 [FeedScreen] 계약으로 옮기고 단발 신호를 소비한다.
 *
 * 네트워크 단절은 `CareerCompassNetworkErrorState`, 그 밖의 실패는 재시도 안내로 그린다. 필터·정렬은
 * `ModalBottomSheet` 로 띄운다.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
public fun FeedEntry(
    onPostingClick: (Long) -> Unit,
    onNotificationsClick: () -> Unit,
    onBoardRegisterClick: () -> Unit,
    onBoardListClick: () -> Unit,
    onSessionEnded: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: FeedViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val resources = LocalResources.current
    val snackbarHostState = remember { SnackbarHostState() }
    val snackbarScope = rememberCoroutineScope()

    val pendingNavigation = state.pendingNavigation
    LaunchedEffect(pendingNavigation) {
        if (pendingNavigation == null) return@LaunchedEffect
        viewModel.onNavigationConsumed()
        when (pendingNavigation) {
            is FeedDestination.PostingDetail -> onPostingClick(pendingNavigation.postingId)
            FeedDestination.Notifications -> onNotificationsClick()
            FeedDestination.BoardRegister -> onBoardRegisterClick()
            FeedDestination.BoardList -> onBoardListClick()
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
            is FeedLoadState.Failed -> {
                if (loadState.isNetworkUnavailable) {
                    CareerCompassNetworkErrorState(
                        onRetryClick = viewModel::retry,
                        onOfflineClick = null,
                    )
                } else {
                    CareerCompassEmptyState(
                        title = stringResource(R.string.feed_error_title),
                        description = stringResource(R.string.feed_error_description),
                        actionText = stringResource(R.string.feed_error_retry),
                        onActionClick = viewModel::retry,
                    )
                }
            }

            FeedLoadState.Loading,
            FeedLoadState.Loaded,
            -> {
                val uiState = remember(state, resources) { state.toFeedUiState(resources, viewModel.clock) }
                val listState = rememberLazyListState()
                PullToRefreshBox(
                    isRefreshing = state.isRefreshing,
                    onRefresh = viewModel::refresh,
                ) {
                    FeedScreen(
                        state = uiState,
                        onEvent = viewModel::onEvent,
                        listState = listState,
                        onLoadMore = viewModel::onLoadMore,
                        isLoadingMore = state.isLoadingMore,
                    )
                }
            }
        }
        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.align(Alignment.BottomCenter),
        )
    }

    val filterDraft = state.filterDraft
    if (filterDraft != null) {
        ModalBottomSheet(
            onDismissRequest = { viewModel.onFilterEvent(FeedFilterEvent.DismissClicked) },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            containerColor = CareerCompassTheme.colors.surface,
        ) {
            FeedFilterSheetContent(
                state = remember(filterDraft, state.boards, resources) { filterDraft.toFilterUiState(resources, state.boards) },
                onEvent = viewModel::onFilterEvent,
            )
        }
    }
    if (state.isSortMenuVisible) {
        ModalBottomSheet(
            onDismissRequest = { viewModel.onSortEvent(FeedSortMenuEvent.DismissClicked) },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            containerColor = CareerCompassTheme.colors.surface,
        ) {
            FeedSortMenuContent(
                state = FeedSortMenuUiState(selected = state.query.sort.toSortOption()),
                onEvent = viewModel::onSortEvent,
            )
        }
    }
}
