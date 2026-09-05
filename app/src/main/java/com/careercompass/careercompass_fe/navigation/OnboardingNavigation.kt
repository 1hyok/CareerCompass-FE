package com.careercompass.careercompass_fe.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.navigation.NavHostController
import com.careercompass.core.ui.navigation.FeatureStackBoundary
import com.careercompass.feature.onboarding.presentation.navigation.OnboardingExternalActions

/**
 * 온보딩 로컬 스택이 앱 셸에 남긴 이동의 구현. 그래프 안의 push/pop 은 `OnboardingNavHost` 가 제 백스택으로 처리한다.
 *
 * @param navigateToMain 인증·온보딩을 끝낸 뒤 메인(피드)로 — 백스택을 전부 비운다.
 * @param navigateToBoardRegister 완료 화면 「게시판 먼저 등록하기」.
 * @param onAuthSessionExpired 지문 뒤 세션 검증이 만료를 알렸다 — 셸이 로그인 화면에 남길 사유를 받는다.
 * @param onSessionEnded Step 1~4 가 401 을 물었다 — 셸이 시작 목적지를 다시 계산해 로그인 화면으로 보낸다(#211).
 *   지문 경로([onAuthSessionExpired])와 갈리는 자리다: 그쪽은 로컬 스택이 스스로 옮기므로 재계산이 없다.
 */
@Composable
internal fun rememberOnboardingExternalActions(
    navigateToMain: () -> Unit,
    navigateToBoardRegister: () -> Unit,
    onAuthSessionExpired: () -> Unit,
    onSessionEnded: () -> Unit,
): OnboardingExternalActions {
    val navigateToMainState by rememberUpdatedState(navigateToMain)
    val navigateToBoardRegisterState by rememberUpdatedState(navigateToBoardRegister)
    val onAuthSessionExpiredState by rememberUpdatedState(onAuthSessionExpired)
    val onSessionEndedState by rememberUpdatedState(onSessionEnded)
    return remember {
        object : OnboardingExternalActions {
            override fun navigateToMain() = navigateToMainState()

            override fun navigateToBoardRegister() = navigateToBoardRegisterState()

            override fun onAuthSessionExpired() = onAuthSessionExpiredState()

            override fun onSessionEnded() = onSessionEndedState()
        }
    }
}

/**
 * 로컬 스택 바닥의 back 을 루트 백스택 pop 으로 돌려주는 경계.
 *
 * 루트가 Nav2 인 동안은 `NavController.popBackStack()` 이고, 루트가 `NavDisplay` 로 바뀌면 루트 백스택의 pop 이 된다 —
 * 구현만 갈리고 계약은 그대로다(#260).
 *
 * @param onRootEmpty 루트에 더 걷어낼 화면이 없다 — 온보딩 첫 화면에서의 back 처럼 앱을 나가야 하는 자리. 루트 바닥에서 할 일이
 *   없는 그래프는 null.
 * @param onAtRootChanged 바텀바 판정에 깊이를 합성해야 하는 그래프만 넘긴다. 바텀바가 없는 그래프는 null.
 */
@Composable
internal fun rememberRootPopBoundary(
    navController: NavHostController,
    onRootEmpty: (() -> Unit)?,
    onAtRootChanged: ((Boolean) -> Unit)?,
): FeatureStackBoundary {
    val onRootEmptyState by rememberUpdatedState(onRootEmpty)
    val onAtRootChangedState by rememberUpdatedState(onAtRootChanged)
    return remember(navController) {
        object : FeatureStackBoundary {
            override fun exit() {
                if (!navController.popBackStack()) onRootEmptyState?.invoke()
            }

            override fun onAtRootChanged(isAtRoot: Boolean) {
                onAtRootChangedState?.invoke(isAtRoot)
            }
        }
    }
}
