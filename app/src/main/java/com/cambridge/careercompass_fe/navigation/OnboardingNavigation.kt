package com.cambridge.careercompass_fe.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.navigation.NavHostController
import com.cambridge.feature.onboarding.presentation.navigation.OnboardingGraphRoute
import com.cambridge.feature.onboarding.presentation.navigation.OnboardingNavActions
import com.cambridge.feature.onboarding.presentation.navigation.OnboardingRoute
import com.cambridge.feature.onboarding.presentation.navigation.toRoute

/**
 * 온보딩 그래프가 앱 셸에 요청하는 이동의 구현.
 *
 * @param onExitRequest Step 1 에서 뒤로 가기 — 그래프에 더 걷어낼 화면이 없으면 앱을 나간다.
 * @param navigateToMain 인증·온보딩을 끝낸 뒤 메인(피드)로 — 백스택을 전부 비운다.
 * @param navigateToBoardRegister 완료 화면 「게시판 먼저 등록하기」.
 */
@Composable
internal fun rememberOnboardingNavActions(
    navController: NavHostController,
    onExitRequest: () -> Unit,
    navigateToMain: () -> Unit,
    navigateToBoardRegister: () -> Unit,
): OnboardingNavActions {
    val onExitRequestState by rememberUpdatedState(onExitRequest)
    val navigateToMainState by rememberUpdatedState(navigateToMain)
    val navigateToBoardRegisterState by rememberUpdatedState(navigateToBoardRegister)
    return remember(navController) {
        OnboardingNavActions(
            replaceLoginWithOnboarding = {
                // 신규 가입 — 로그인 화면을 걷어내고 Step 1 로. 뒤로가기로 로그인에 돌아가지 않는다.
                navController.navigate(OnboardingRoute.Step1) {
                    popUpTo<OnboardingRoute.Login> { inclusive = true }
                    launchSingleTop = true
                }
            },
            replaceAuthWithFeed = { navigateToMainState() },
            navigateToLoginFromBiometric = {
                navController.navigate(OnboardingRoute.Login) {
                    popUpTo<OnboardingRoute.BiometricLogin> { inclusive = true }
                    launchSingleTop = true
                }
            },
            proceedToStep = { step ->
                navController.navigate(step.toRoute()) { launchSingleTop = true }
            },
            proceedToComplete = {
                // 완료 화면에서 뒤로가기로 단계 화면에 돌아가지 않도록 Step 1 부터 걷어낸다.
                navController.navigate(OnboardingRoute.Complete) {
                    popUpTo<OnboardingRoute.Step1> { inclusive = true }
                    launchSingleTop = true
                }
            },
            popBack = {
                if (!navController.popBackStack()) onExitRequestState()
            },
            replaceOnboardingWithFeed = { navigateToMainState() },
            replaceOnboardingWithBoardRegister = { navigateToBoardRegisterState() },
        )
    }
}

/** 온보딩 그래프 루트의 back stack entry — Step 1~4·완료 화면이 공유하는 ViewModel 의 소유자. */
internal fun NavHostController.onboardingGraphEntry() = getBackStackEntry<OnboardingGraphRoute>()
