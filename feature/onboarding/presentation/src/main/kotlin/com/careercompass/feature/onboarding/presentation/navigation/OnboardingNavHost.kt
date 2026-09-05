package com.careercompass.feature.onboarding.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import com.careercompass.core.ui.navigation.FeatureNavDisplay
import com.careercompass.core.ui.navigation.FeatureStackBoundary
import com.careercompass.feature.onboarding.presentation.biometric.BiometricLoginScreen
import com.careercompass.feature.onboarding.presentation.complete.OnboardingCompleteScreen
import com.careercompass.feature.onboarding.presentation.flow.OnboardingStep1Screen
import com.careercompass.feature.onboarding.presentation.flow.OnboardingStep2Screen
import com.careercompass.feature.onboarding.presentation.flow.OnboardingStep3Screen
import com.careercompass.feature.onboarding.presentation.flow.OnboardingStep4Screen
import com.careercompass.feature.onboarding.presentation.flow.OnboardingViewModel
import com.careercompass.feature.onboarding.presentation.login.LoginScreen

/**
 * 온보딩 피처가 소유하는 로컬 Navigation 3 스택.
 *
 * 흐름: 로그인 / 지문 → Step 1~4 → 완료(피드 이동)
 *
 * ## 공유 ViewModel 수명
 *
 * Nav2 에서 Step 1~4·완료가 공유하는 [OnboardingViewModel] 은 온보딩 **그래프 엔트리**의 `ViewModelStore` 에 묶여
 * 있었다(`getBackStackEntry<Graph>()` + `hiltViewModel(parentEntry)`). Nav3 엔 그래프라는 중간 계층이 없으므로, 같은 수명을
 * **host 자신의 스코프**로 옮긴다 — 이 컴포저블은 온보딩을 담은 루트 엔트리 안에서 실행되니 여기서 만든 ViewModel 은
 * 그 엔트리가 루트 백스택에서 내려갈 때 정리된다. 즉 단계 사이 재진입에는 살아남고 온보딩을 벗어나면 사라지는, 이관
 * 전과 같은 특성이다. 세션 종료로 셸이 루트 스택을 새로 세우면 루트 엔트리와 함께 이 ViewModel 과 입력 초안도
 * 버려진다(#133 의 규칙 그대로).
 *
 * 로그인·지문 화면은 각자의 ViewModel 을 `entry { }` 안에서 만든다 — entry 범위(= 그 화면이 스택에서 빠지면 정리)다.
 *
 * @param startDestination 시작 화면. 로그인 / 지문 / 재개 단계 중 앱 셸이 고른다.
 * @param isSessionExpiryNoticeVisible 로그인 화면에 「로그인이 만료됐다」를 알릴지. 세션 판정은 앱 셸이 하고 이 스택은
 *   결론만 전달한다 — 지문 화면으로 시작해 그 안에서 로그인으로 옮겨 온 경우에도 같은 값이 걸려 있다(#128).
 * @param onSessionExpiryNoticeDismissed 그 안내를 닫았거나 다시 로그인을 시도했다.
 */
@Composable
public fun OnboardingNavHost(
    startDestination: OnboardingRoute,
    boundary: FeatureStackBoundary,
    externalActions: OnboardingExternalActions,
    isSessionExpiryNoticeVisible: Boolean,
    onSessionExpiryNoticeDismissed: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val backStack = rememberNavBackStack(startDestination)
    val actions =
        remember(backStack, boundary, externalActions) {
            OnboardingLocalNavActions(backStack, boundary, externalActions)
        }

    // 단계 화면 전체가 공유하는 ViewModel — 상세는 KDoc 참고. entry 안에서 만들면 그 화면이 pop 될 때 함께 사라져
    // 이관 전(그래프 스코프)과 수명이 달라진다.
    val onboardingViewModel: OnboardingViewModel = hiltViewModel()

    FeatureNavDisplay(
        backStack = backStack,
        boundary = boundary,
        modifier = modifier,
        entryProvider =
            entryProvider {
                entry<OnboardingRoute.Login> {
                    LoginScreen(
                        onLoginSuccess = actions::replaceAuthWithFeed,
                        onNewUserOnboarding = actions::replaceLoginWithOnboarding,
                        isSessionExpiryNoticeVisible = isSessionExpiryNoticeVisible,
                        onSessionExpiryNoticeDismissed = onSessionExpiryNoticeDismissed,
                    )
                }
                entry<OnboardingRoute.BiometricLogin> {
                    BiometricLoginScreen(
                        onLoginSuccess = actions::replaceAuthWithFeed,
                        onOnboardingRequired = actions::replaceAuthWithOnboarding,
                        onOtherMethodLogin = actions::navigateToLoginFromBiometric,
                        onSessionExpired = actions::navigateToLoginAfterSessionExpiry,
                    )
                }
                entry<OnboardingRoute.Step1> {
                    OnboardingStep1Screen(
                        viewModel = onboardingViewModel,
                        onNavigate = actions::navigate,
                        onBack = actions::popBack,
                        onSessionEnded = actions::onSessionEnded,
                    )
                }
                entry<OnboardingRoute.Step2> {
                    OnboardingStep2Screen(
                        viewModel = onboardingViewModel,
                        onNavigate = actions::navigate,
                        onBack = actions::popBack,
                        onSessionEnded = actions::onSessionEnded,
                    )
                }
                entry<OnboardingRoute.Step3> {
                    OnboardingStep3Screen(
                        viewModel = onboardingViewModel,
                        onNavigate = actions::navigate,
                        onBack = actions::popBack,
                        onSessionEnded = actions::onSessionEnded,
                    )
                }
                entry<OnboardingRoute.Step4> {
                    OnboardingStep4Screen(
                        viewModel = onboardingViewModel,
                        onNavigate = actions::navigate,
                        onBack = actions::popBack,
                        onSessionEnded = actions::onSessionEnded,
                    )
                }
                entry<OnboardingRoute.Complete> {
                    OnboardingCompleteScreen(
                        viewModel = onboardingViewModel,
                        onNavigate = actions::navigate,
                    )
                }
            },
    )
}
