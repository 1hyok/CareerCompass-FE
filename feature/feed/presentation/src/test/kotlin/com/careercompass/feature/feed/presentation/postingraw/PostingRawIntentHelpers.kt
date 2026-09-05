package com.careercompass.feature.feed.presentation.postingraw

import kotlinx.coroutines.flow.StateFlow

/*
 * 테스트가 읽기 쉽도록 [PostingRawIntent] 를 짧은 손잡이로 감싼다. 프로덕션의 진입점은
 * [PostingRawViewModel.onIntent] 하나뿐이다(#246).
 */

internal val PostingRawViewModel.state: StateFlow<PostingRawViewState> get() = uiState

internal fun PostingRawViewModel.onEvent(event: PostingRawEvent) = onIntent(PostingRawIntent.Screen(event))

internal fun PostingRawViewModel.retry() = onIntent(PostingRawIntent.Retry)

internal fun PostingRawViewModel.onBackConsumed() = onIntent(PostingRawIntent.ConsumeBack)

internal fun PostingRawViewModel.onOpenUrlConsumed() = onIntent(PostingRawIntent.ConsumeOpenUrl)

internal fun PostingRawViewModel.onOpenUrlRejectedConsumed() = onIntent(PostingRawIntent.ConsumeOpenUrlRejected)

internal fun PostingRawViewModel.onSessionEndedConsumed() = onIntent(PostingRawIntent.ConsumeSessionEnded)
