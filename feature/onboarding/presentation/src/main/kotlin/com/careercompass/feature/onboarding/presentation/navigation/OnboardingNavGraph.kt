package com.careercompass.feature.onboarding.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation.compose.navigation
import com.careercompass.feature.onboarding.presentation.biometric.BiometricLoginScreen
import com.careercompass.feature.onboarding.presentation.complete.OnboardingCompleteEntry
import com.careercompass.feature.onboarding.presentation.flow.OnboardingStep1Entry
import com.careercompass.feature.onboarding.presentation.flow.OnboardingStep2Entry
import com.careercompass.feature.onboarding.presentation.flow.OnboardingStep3Entry
import com.careercompass.feature.onboarding.presentation.flow.OnboardingStep4Entry
import com.careercompass.feature.onboarding.presentation.flow.OnboardingViewModel
import com.careercompass.feature.onboarding.presentation.login.LoginScreen

/**
 * 온보딩 중첩 그래프를 등록한다.
 *
 * Step 1~4 와 완료 화면은 [OnboardingViewModel] 하나를 공유한다 — [graphScopedParentEntry] 가 돌려주는
 * [OnboardingGraphRoute] 의 back stack entry 를 ViewModel 소유자로 쓴다(앱 셸은
 * `navController.getBackStackEntry<OnboardingGraphRoute>()` 를 넘긴다). 로그인·지문 화면은 각자의 ViewModel 을 쓴다.
 *
 * @param isSessionExpiryNoticeVisible 로그인 화면에 「로그인이 만료됐다」를 알릴지. 세션 판정은 앱 셸이 하고 이
 *   그래프는 결론만 전달한다 — 지문 화면으로 시작해 그 안에서 로그인으로 옮겨 온 경우에도 같은 값이 걸려 있다(#128).
 * @param onSessionExpiryNoticeDismissed 그 안내를 닫았거나 다시 로그인을 시도했다.
 */
public fun NavGraphBuilder.onboardingNavGraph(
    startDestination: OnboardingRoute,
    graphScopedParentEntry: () -> NavBackStackEntry,
    actions: OnboardingNavActions,
    isSessionExpiryNoticeVisible: Boolean,
    onSessionExpiryNoticeDismissed: () -> Unit,
) {
    navigation<OnboardingGraphRoute>(startDestination = startDestination) {
        composable<OnboardingRoute.Login> {
            LoginScreen(
                onLoginSuccess = actions.replaceAuthWithFeed,
                onNewUserOnboarding = actions.replaceLoginWithOnboarding,
                isSessionExpiryNoticeVisible = isSessionExpiryNoticeVisible,
                onSessionExpiryNoticeDismissed = onSessionExpiryNoticeDismissed,
            )
        }
        composable<OnboardingRoute.BiometricLogin> {
            BiometricLoginScreen(
                onLoginSuccess = actions.replaceAuthWithFeed,
                onOnboardingRequired = actions.replaceAuthWithOnboarding,
                onOtherMethodLogin = actions.navigateToLoginFromBiometric,
                onSessionExpired = actions.navigateToLoginAfterSessionExpiry,
            )
        }
        composable<OnboardingRoute.Step1> { entry ->
            OnboardingStep1Entry(
                viewModel = entry.onboardingViewModel(graphScopedParentEntry),
                onNavigate = actions::navigate,
                onBack = actions.popBack,
                onSessionEnded = actions.onSessionEnded,
            )
        }
        composable<OnboardingRoute.Step2> { entry ->
            OnboardingStep2Entry(
                viewModel = entry.onboardingViewModel(graphScopedParentEntry),
                onNavigate = actions::navigate,
                onBack = actions.popBack,
                onSessionEnded = actions.onSessionEnded,
            )
        }
        composable<OnboardingRoute.Step3> { entry ->
            OnboardingStep3Entry(
                viewModel = entry.onboardingViewModel(graphScopedParentEntry),
                onNavigate = actions::navigate,
                onBack = actions.popBack,
                onSessionEnded = actions.onSessionEnded,
            )
        }
        composable<OnboardingRoute.Step4> { entry ->
            OnboardingStep4Entry(
                viewModel = entry.onboardingViewModel(graphScopedParentEntry),
                onNavigate = actions::navigate,
                onBack = actions.popBack,
                onSessionEnded = actions.onSessionEnded,
            )
        }
        composable<OnboardingRoute.Complete> { entry ->
            OnboardingCompleteEntry(
                viewModel = entry.onboardingViewModel(graphScopedParentEntry),
                onNavigate = actions::navigate,
            )
        }
    }
}

@Composable
private fun NavBackStackEntry.onboardingViewModel(graphScopedParentEntry: () -> NavBackStackEntry): OnboardingViewModel {
    val parentEntry = remember(this) { graphScopedParentEntry() }
    return hiltViewModel(parentEntry)
}
