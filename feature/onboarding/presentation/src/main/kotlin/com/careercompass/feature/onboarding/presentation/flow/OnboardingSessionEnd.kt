package com.careercompass.feature.onboarding.presentation.flow

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState

/**
 * [OnboardingViewModel] 의 세션 종료 신호를 소비해 앱 셸에 넘긴다 — 피드·게시판의 `sessionEnded` 배선과 같은
 * 모양이다(#211).
 *
 * 소비를 먼저, 넘기기를 나중에 하는 순서는 [ConsumePendingNavigation] 과 같은 이유다 — Step 1~4 가 그래프 스코프
 * ViewModel 하나를 공유하므로, 화면 전환 중 두 Entry 가 같은 신호를 읽고 셸을 두 번 부르지 않게 한다.
 *
 * 이 컴포저블을 Step 화면마다 두는 이유는 신호가 뜨는 자리가 화면마다 다르기 때문이다(저장·업로드·삭제).
 * 넘긴 뒤 무엇을 할지는 셸이 정한다 — 온보딩은 「끝났다」까지만 안다.
 */
@Composable
internal fun ConsumeSessionEnd(
    sessionEnded: Boolean,
    onSessionEnded: () -> Unit,
    onConsumed: () -> Unit,
) {
    val currentOnSessionEnded by rememberUpdatedState(onSessionEnded)
    val currentOnConsumed by rememberUpdatedState(onConsumed)

    LaunchedEffect(sessionEnded) {
        if (!sessionEnded) return@LaunchedEffect
        currentOnConsumed()
        currentOnSessionEnded()
    }
}
