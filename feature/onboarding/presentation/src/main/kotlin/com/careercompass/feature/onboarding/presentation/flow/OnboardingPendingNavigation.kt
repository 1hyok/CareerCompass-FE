package com.careercompass.feature.onboarding.presentation.flow

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState

/**
 * [OnboardingViewModel] 의 단발 이동 신호를 소비한다.
 *
 * 이동과 소비를 같은 effect 안에서 연속으로 처리해, 다음 화면이 합성될 때는 이미 신호가 비어 있게 한다 — 그래프
 * 스코프 ViewModel 을 여러 Screen 이 함께 보므로 두 번 이동하지 않기 위한 순서다.
 */
@Composable
internal fun ConsumePendingNavigation(
    destination: OnboardingDestination?,
    onNavigate: (OnboardingDestination) -> Unit,
    onConsumed: () -> Unit,
) {
    val currentOnNavigate by rememberUpdatedState(onNavigate)
    val currentOnConsumed by rememberUpdatedState(onConsumed)

    LaunchedEffect(destination) {
        if (destination == null) return@LaunchedEffect
        currentOnNavigate(destination)
        currentOnConsumed()
    }
}
