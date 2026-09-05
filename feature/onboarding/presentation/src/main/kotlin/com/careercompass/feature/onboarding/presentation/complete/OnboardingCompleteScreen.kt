package com.careercompass.feature.onboarding.presentation.complete

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.careercompass.feature.onboarding.presentation.biometric.BiometricEnrollGate
import com.careercompass.feature.onboarding.presentation.flow.ConsumePendingNavigation
import com.careercompass.feature.onboarding.presentation.flow.OnboardingDestination
import com.careercompass.feature.onboarding.presentation.flow.OnboardingIntent
import com.careercompass.feature.onboarding.presentation.flow.OnboardingViewModel

/**
 * 완료 화면의 상태 배선 — 그래프 스코프 [OnboardingViewModel] 의 사용자 이름을 인사에 쓴다.
 *
 * 「공고 보러 가기」로 피드에 나가는 길목에는 [BiometricEnrollGate] 가 있다 — 신규 사용자에게 지문 등록을 한 번
 * 제안하고 끝나면 이동을 이어 준다(#98). 이 자리인 이유는 온보딩을 마친 시점에는 프로필이 확실히 있어서, 등록을
 * 계정에 귀속하는 규칙(#81)이 성립하기 때문이다. 「게시판 먼저 등록하기」는 묻지 않고 그대로 보낸다 — 등록하러
 * 가는 사용자를 붙잡지 않고, 다음 로그인 때 로그인 화면의 같은 관문이 한 번 묻는다.
 */
@Composable
public fun OnboardingCompleteScreen(
    viewModel: OnboardingViewModel,
    onNavigate: (OnboardingDestination) -> Unit,
    modifier: Modifier = Modifier,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val pendingNavigation = state.pendingNavigation
    ConsumePendingNavigation(
        destination = pendingNavigation.takeIf { it != OnboardingDestination.Feed },
        onNavigate = onNavigate,
        onConsumed = { viewModel.onIntent(OnboardingIntent.ConsumeNavigation) },
    )

    OnboardingCompleteContent(
        state = OnboardingCompleteUiState(userName = state.userName),
        onEvent = { viewModel.onIntent(OnboardingIntent.Complete(it)) },
        modifier = modifier,
    )

    BiometricEnrollGate(
        isRequested = pendingNavigation == OnboardingDestination.Feed,
        onProceed = {
            onNavigate(OnboardingDestination.Feed)
            viewModel.onIntent(OnboardingIntent.ConsumeNavigation)
        },
    )
}
