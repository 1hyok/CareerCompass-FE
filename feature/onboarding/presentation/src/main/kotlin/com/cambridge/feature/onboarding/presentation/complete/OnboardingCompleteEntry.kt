package com.cambridge.feature.onboarding.presentation.complete

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.cambridge.feature.onboarding.presentation.flow.ConsumePendingNavigation
import com.cambridge.feature.onboarding.presentation.flow.OnboardingDestination
import com.cambridge.feature.onboarding.presentation.flow.OnboardingViewModel

/** 완료 화면의 상태 배선 — 그래프 스코프 [OnboardingViewModel] 의 사용자 이름을 인사에 쓴다. */
@Composable
public fun OnboardingCompleteEntry(
    viewModel: OnboardingViewModel,
    onNavigate: (OnboardingDestination) -> Unit,
    modifier: Modifier = Modifier,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    ConsumePendingNavigation(
        destination = state.pendingNavigation,
        onNavigate = onNavigate,
        onConsumed = viewModel::onNavigationConsumed,
    )

    OnboardingCompleteScreen(
        state = OnboardingCompleteUiState(userName = state.userName),
        onEvent = viewModel::onCompleteEvent,
        modifier = modifier,
    )
}
