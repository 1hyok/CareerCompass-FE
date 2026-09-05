package com.careercompass.feature.feed.presentation.postingdetail

import kotlinx.coroutines.flow.StateFlow

/*
 * 테스트가 읽기 쉽도록 [PostingDetailIntent] 를 짧은 손잡이로 감싼다. 프로덕션의 진입점은
 * [PostingDetailViewModel.onIntent] 하나뿐이다(#246).
 */

internal val PostingDetailViewModel.state: StateFlow<PostingDetailViewState> get() = uiState

internal fun PostingDetailViewModel.onEvent(event: PostingDetailEvent) = onIntent(PostingDetailIntent.Screen(event))

internal fun PostingDetailViewModel.onNavigationConsumed() = onIntent(PostingDetailIntent.ConsumeNavigation)

internal fun PostingDetailViewModel.onShareConsumed() = onIntent(PostingDetailIntent.ConsumeShare)

internal fun PostingDetailViewModel.onMessageConsumed() = onIntent(PostingDetailIntent.ConsumeMessage)

internal fun PostingDetailViewModel.onSessionEndedConsumed() = onIntent(PostingDetailIntent.ConsumeSessionEnded)
