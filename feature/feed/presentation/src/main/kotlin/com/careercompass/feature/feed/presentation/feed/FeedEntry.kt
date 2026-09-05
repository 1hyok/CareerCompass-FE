package com.careercompass.feature.feed.presentation.feed

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
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.careercompass.core.ui.theme.CareerCompassTheme
import com.careercompass.feature.feed.presentation.FeedScreen
import com.careercompass.feature.feed.presentation.feedfilter.FeedFilterEvent
import com.careercompass.feature.feed.presentation.feedfilter.FeedFilterSheetContent
import com.careercompass.feature.feed.presentation.feedfilter.FeedSortMenuContent
import com.careercompass.feature.feed.presentation.feedfilter.FeedSortMenuEvent
import com.careercompass.feature.feed.presentation.feedfilter.FeedSortMenuUiState
import com.careercompass.feature.feed.presentation.shared.util.toSortOption
import kotlinx.coroutines.launch

/**
 * 피드 홈 진입점 — [FeedViewModel] 상태를 [FeedScreen] 계약으로 옮기고 단발 신호를 소비한다.
 *
 * 실패는 사유별로 [FeedFailureContent] 가 그린다. 필터·정렬은 `ModalBottomSheet` 로 띄운다. 프로필 입력
 * 안내는 [onProfileClick] 으로 앱 셸(마이 탭)에 맡긴다 — 공고 상세의 같은 안내와 목적지를 맞춘다.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
public fun FeedEntry(
    onPostingClick: (Long) -> Unit,
    onNotificationsClick: () -> Unit,
    onBoardRegisterClick: () -> Unit,
    onBoardListClick: () -> Unit,
    onProfileClick: () -> Unit,
    onSessionEnded: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: FeedViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val resources = LocalResources.current
    val snackbarHostState = remember { SnackbarHostState() }
    val snackbarScope = rememberCoroutineScope()

    // 게시판 등록에서 돌아온 길을 위해서다 — 빈 피드가 「등록한 게시판이 없어요」라고 보낸 사용자가
    // 등록을 마치고 돌아오면, 게시판을 다시 읽어야 같은 안내가 남지 않는다.
    LifecycleResumeEffect(Unit) {
        viewModel.onIntent(FeedIntent.RefreshBoards)
        onPauseOrDispose { }
    }
    val pendingNavigation = state.pendingNavigation
    LaunchedEffect(pendingNavigation) {
        if (pendingNavigation == null) return@LaunchedEffect
        viewModel.onIntent(FeedIntent.ConsumeNavigation)
        when (pendingNavigation) {
            is FeedDestination.PostingDetail -> onPostingClick(pendingNavigation.postingId)
            FeedDestination.Notifications -> onNotificationsClick()
            FeedDestination.BoardRegister -> onBoardRegisterClick()
            FeedDestination.BoardList -> onBoardListClick()
            FeedDestination.Profile -> onProfileClick()
        }
    }
    val sessionEnded = state.sessionEnded
    LaunchedEffect(sessionEnded) {
        if (sessionEnded) {
            viewModel.onIntent(FeedIntent.ConsumeSessionEnded)
            onSessionEnded()
        }
    }
    val message = state.message
    LaunchedEffect(message) {
        if (message == null) return@LaunchedEffect
        viewModel.onIntent(FeedIntent.ConsumeMessage)
        snackbarScope.launch { snackbarHostState.showSnackbar(resources.getString(message.messageRes())) }
    }

    Box(modifier = modifier.fillMaxSize()) {
        when (val loadState = state.loadState) {
            is FeedLoadState.Failed -> {
                // 저장해 둔 스냅샷이 있을 때만 「오프라인 모드로 보기」를 연다 — 눌러도 보여 줄 것이 없는
                // 버튼을 그리지 않는다. 점검 중에도 같은 길을 열어 둔다.
                //
                // 「조건 지우고 다시 보기」도 같은 규칙이다 — 되돌릴 조건이 실제로 걸려 있고 그 실패가
                // 조건 탓일 여지가 있을 때만 연다(FeedViewState.canResetFailedQuery). 이 화면이
                // FeedScreen 을 통째로 대신해 헤더의 조작이 전부 사라지므로, 이것이 조건에서 빠져나갈
                // 유일한 길이다(#144).
                FeedFailureContent(
                    reason = loadState.reason,
                    onRetryClick = { viewModel.onIntent(FeedIntent.Retry) },
                    onOfflineClick = state.offlineSnapshot?.let { { viewModel.onIntent(FeedIntent.ShowOfflineSnapshot) } },
                    onResetQueryClick = if (state.canResetFailedQuery) ({ viewModel.onIntent(FeedIntent.ResetQueryAndRetry) }) else null,
                )
            }

            FeedLoadState.Loading,
            FeedLoadState.Loaded,
            -> {
                val uiState = remember(state, resources) { state.toFeedUiState(resources, viewModel.clock) }
                val listState = rememberLazyListState()
                PullToRefreshBox(
                    isRefreshing = state.isRefreshing,
                    onRefresh = { viewModel.onIntent(FeedIntent.Refresh) },
                ) {
                    FeedScreen(
                        state = uiState,
                        onEvent = { viewModel.onIntent(FeedIntent.Screen(it)) },
                        listState = listState,
                        // 커서가 남았을 때만 자동 페이징을 연다 — 「받은 것이 없다」와 「서버에 없다」를
                        // 가르는 근거가 커서뿐이라, 목록이 비어 있어도 커서가 남았으면 아직 끝이 아니다.
                        onLoadMore = if (state.hasNext) ({ viewModel.onIntent(FeedIntent.LoadMore) }) else null,
                        loadMore = state.loadMore,
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
            onDismissRequest = { viewModel.onIntent(FeedIntent.Filter(FeedFilterEvent.DismissClicked)) },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            containerColor = CareerCompassTheme.colors.surface,
        ) {
            FeedFilterSheetContent(
                state =
                    remember(filterDraft, state.boards, state.boardsLoaded, resources) {
                        filterDraft.toFilterUiState(resources, state.boards, state.boardsLoaded)
                    },
                onEvent = { viewModel.onIntent(FeedIntent.Filter(it)) },
            )
        }
    }
    if (state.isSortMenuVisible) {
        ModalBottomSheet(
            onDismissRequest = { viewModel.onIntent(FeedIntent.Sort(FeedSortMenuEvent.DismissClicked)) },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            containerColor = CareerCompassTheme.colors.surface,
        ) {
            FeedSortMenuContent(
                state = FeedSortMenuUiState(selected = state.query.sort.toSortOption()),
                onEvent = { viewModel.onIntent(FeedIntent.Sort(it)) },
            )
        }
    }
}
