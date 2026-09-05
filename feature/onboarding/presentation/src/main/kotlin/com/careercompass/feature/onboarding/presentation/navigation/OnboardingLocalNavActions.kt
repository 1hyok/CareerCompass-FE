package com.careercompass.feature.onboarding.presentation.navigation

import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import com.careercompass.core.ui.navigation.FeatureStackBoundary
import com.careercompass.core.ui.navigation.popOrExit
import com.careercompass.core.ui.navigation.pushSingleTop
import com.careercompass.core.ui.navigation.replaceAllWith
import com.careercompass.feature.onboarding.domain.model.OnboardingStep

/**
 * 온보딩 화면 콜백을 로컬 백스택 조작으로 잇는다.
 *
 * 컴포저블이 아니라 평범한 클래스다 — 백스택 모양은 컴포지션 없이 그대로 잴 수 있어야 회귀 기준을 JVM 테스트로
 * 고정할 수 있기 때문이다(`OnboardingLocalNavActionsTest`).
 *
 * Nav2 시절 `popUpTo(inclusive = true)` + `navigate` 로 적던 결과 상태는 전부 [replaceAllWith] 다 — 로그인·지문·단계는
 * 「되돌아가면 안 되는」 화면이라 그 위에 쌓지 않고 스택을 새 화면 하나로 수렴시킨다.
 */
internal class OnboardingLocalNavActions(
    private val backStack: NavBackStack<NavKey>,
    private val boundary: FeatureStackBoundary,
    private val externalActions: OnboardingExternalActions,
) : OnboardingNavActions {
    /** 신규 가입 — 로그인 화면을 남기지 않는다. 뒤로가기로 로그인에 돌아가지 않는다. */
    override fun replaceLoginWithOnboarding(): Unit = backStack.replaceAllWith(OnboardingRoute.Step1)

    override fun replaceAuthWithFeed(): Unit = externalActions.navigateToMain()

    /** 지문 확인 뒤 온보딩 미완료 — 지문 화면을 남기지 않는다. */
    override fun replaceAuthWithOnboarding(): Unit = backStack.replaceAllWith(OnboardingRoute.Step1)

    override fun navigateToLoginFromBiometric(): Unit = backStack.replaceAllWith(OnboardingRoute.Login)

    override fun navigateToLoginAfterSessionExpiry() {
        // 사유를 먼저 싣는다 — 로그인 화면이 그려질 때 안내가 이미 켜져 있어야 한 프레임 늦게 뜨지 않는다.
        externalActions.onAuthSessionExpired()
        backStack.replaceAllWith(OnboardingRoute.Login)
    }

    override fun onSessionEnded(): Unit = externalActions.onSessionEnded()

    override fun proceedToStep(step: OnboardingStep): Unit = backStack.pushSingleTop(step.toRoute())

    /** 완료 화면에서 뒤로가기로 단계 화면에 돌아가지 않도록 단계 전부를 걷어낸다. */
    override fun proceedToComplete(): Unit = backStack.replaceAllWith(OnboardingRoute.Complete)

    override fun popBack(): Unit = backStack.popOrExit(boundary)

    override fun replaceOnboardingWithFeed(): Unit = externalActions.navigateToMain()

    override fun replaceOnboardingWithBoardRegister(): Unit = externalActions.navigateToBoardRegister()
}
