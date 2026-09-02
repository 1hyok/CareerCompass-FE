package com.cambridge.feature.onboarding.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation.compose.navigation
import com.cambridge.feature.onboarding.presentation.biometric.BiometricLoginEntry
import com.cambridge.feature.onboarding.presentation.complete.OnboardingCompleteEntry
import com.cambridge.feature.onboarding.presentation.flow.OnboardingStep1Entry
import com.cambridge.feature.onboarding.presentation.flow.OnboardingStep2Entry
import com.cambridge.feature.onboarding.presentation.flow.OnboardingStep3Entry
import com.cambridge.feature.onboarding.presentation.flow.OnboardingStep4Entry
import com.cambridge.feature.onboarding.presentation.flow.OnboardingViewModel
import com.cambridge.feature.onboarding.presentation.login.LoginEntry

/**
 * 온보딩 중첩 그래프를 등록한다.
 *
 * Step 1~4 와 완료 화면은 [OnboardingViewModel] 하나를 공유한다 — [graphScopedParentEntry] 가 돌려주는
 * [OnboardingGraphRoute] 의 back stack entry 를 ViewModel 소유자로 쓴다(앱 셸은
 * `navController.getBackStackEntry<OnboardingGraphRoute>()` 를 넘긴다). 로그인·지문 화면은 각자의 ViewModel 을 쓴다.
 */
public fun NavGraphBuilder.onboardingNavGraph(
    startDestination: OnboardingRoute,
    graphScopedParentEntry: () -> NavBackStackEntry,
    actions: OnboardingNavActions,
) {
    navigation<OnboardingGraphRoute>(startDestination = startDestination) {
        composable<OnboardingRoute.Login> {
            LoginEntry(
                onLoginSuccess = actions.replaceAuthWithFeed,
                onNewUserOnboarding = actions.replaceLoginWithOnboarding,
            )
        }
        composable<OnboardingRoute.BiometricLogin> {
            BiometricLoginEntry(
                onLoginSuccess = actions.replaceAuthWithFeed,
                onOnboardingRequired = actions.replaceAuthWithOnboarding,
                onOtherMethodLogin = actions.navigateToLoginFromBiometric,
            )
        }
        composable<OnboardingRoute.Step1> { entry ->
            OnboardingStep1Entry(
                viewModel = entry.onboardingViewModel(graphScopedParentEntry),
                onNavigate = actions::navigate,
                onBack = actions.popBack,
            )
        }
        composable<OnboardingRoute.Step2> { entry ->
            OnboardingStep2Entry(
                viewModel = entry.onboardingViewModel(graphScopedParentEntry),
                onNavigate = actions::navigate,
                onBack = actions.popBack,
            )
        }
        composable<OnboardingRoute.Step3> { entry ->
            OnboardingStep3Entry(
                viewModel = entry.onboardingViewModel(graphScopedParentEntry),
                onNavigate = actions::navigate,
                onBack = actions.popBack,
            )
        }
        composable<OnboardingRoute.Step4> { entry ->
            OnboardingStep4Entry(
                viewModel = entry.onboardingViewModel(graphScopedParentEntry),
                onNavigate = actions::navigate,
                onBack = actions.popBack,
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
