package com.careercompass.feature.feed.presentation.board

import kotlinx.coroutines.flow.StateFlow

/*
 * 테스트가 읽기 쉽도록 [BoardListIntent]·[BoardRegisterIntent] 를 짧은 손잡이로 감싼다.
 *
 * 프로덕션의 진입점은 각 ViewModel 의 `onIntent` 하나뿐이다(#246). 이 확장은 테스트 소스에만 있어 시나리오
 * 이름을 유지해 주고, 프로덕션에 두 번째 진입점을 되살리지 않는다.
 */

internal val BoardListViewModel.state: StateFlow<BoardListViewState> get() = uiState

internal fun BoardListViewModel.onEvent(event: BoardListEvent) = onIntent(BoardListIntent.Screen(event))

internal fun BoardListViewModel.onEditEvent(event: BoardEditEvent) = onIntent(BoardListIntent.Edit(event))

internal fun BoardListViewModel.refresh() = onIntent(BoardListIntent.Refresh)

internal fun BoardListViewModel.retryLoad() = onIntent(BoardListIntent.RetryLoad)

internal fun BoardListViewModel.confirmDelete() = onIntent(BoardListIntent.ConfirmDelete)

internal fun BoardListViewModel.dismissDelete() = onIntent(BoardListIntent.DismissDelete)

internal fun BoardListViewModel.onNavigationConsumed() = onIntent(BoardListIntent.ConsumeNavigation)

internal fun BoardListViewModel.onMessageConsumed() = onIntent(BoardListIntent.ConsumeMessage)

internal fun BoardListViewModel.onSessionEndedConsumed() = onIntent(BoardListIntent.ConsumeSessionEnded)

internal val BoardRegisterViewModel.state: StateFlow<BoardRegisterViewState> get() = uiState

internal fun BoardRegisterViewModel.onEvent(event: BoardRegisterEvent) = onIntent(BoardRegisterIntent.Screen(event))

internal fun BoardRegisterViewModel.onBackConsumed() = onIntent(BoardRegisterIntent.ConsumeBack)

internal fun BoardRegisterViewModel.onMessageConsumed() = onIntent(BoardRegisterIntent.ConsumeMessage)

internal fun BoardRegisterViewModel.onSessionEndedConsumed() = onIntent(BoardRegisterIntent.ConsumeSessionEnded)
