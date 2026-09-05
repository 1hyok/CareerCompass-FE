package com.careercompass.feature.feed.presentation.feed

import com.careercompass.feature.feed.presentation.FeedUiEvent
import com.careercompass.feature.feed.presentation.feedfilter.FeedFilterEvent
import com.careercompass.feature.feed.presentation.feedfilter.FeedSortMenuEvent
import kotlinx.coroutines.flow.StateFlow

/*
 * 테스트가 읽기 쉽도록 [FeedIntent] 를 짧은 손잡이로 감싼다.
 *
 * 프로덕션의 진입점은 [FeedViewModel.onIntent] 하나뿐이다(#246). 이 확장은 테스트 소스에만 있어 시나리오
 * 이름(「필터 시트에서 …」)을 유지해 주고, 프로덕션에 두 번째 진입점을 되살리지 않는다.
 */

internal val FeedViewModel.state: StateFlow<FeedViewState> get() = uiState

internal fun FeedViewModel.onEvent(event: FeedUiEvent) = onIntent(FeedIntent.Screen(event))

internal fun FeedViewModel.onFilterEvent(event: FeedFilterEvent) = onIntent(FeedIntent.Filter(event))

internal fun FeedViewModel.onSortEvent(event: FeedSortMenuEvent) = onIntent(FeedIntent.Sort(event))

internal fun FeedViewModel.onLoadMore() = onIntent(FeedIntent.LoadMore)

internal fun FeedViewModel.refresh() = onIntent(FeedIntent.Refresh)

internal fun FeedViewModel.retry() = onIntent(FeedIntent.Retry)

internal fun FeedViewModel.resetQueryAndRetry() = onIntent(FeedIntent.ResetQueryAndRetry)

internal fun FeedViewModel.refreshBoards() = onIntent(FeedIntent.RefreshBoards)

internal fun FeedViewModel.showOfflineSnapshot() = onIntent(FeedIntent.ShowOfflineSnapshot)

internal fun FeedViewModel.onBoardListRequested() = onIntent(FeedIntent.RequestBoardList)

internal fun FeedViewModel.onBoardRegisterRequested() = onIntent(FeedIntent.RequestBoardRegister)

internal fun FeedViewModel.onNavigationConsumed() = onIntent(FeedIntent.ConsumeNavigation)

internal fun FeedViewModel.onMessageConsumed() = onIntent(FeedIntent.ConsumeMessage)

internal fun FeedViewModel.onSessionEndedConsumed() = onIntent(FeedIntent.ConsumeSessionEnded)
