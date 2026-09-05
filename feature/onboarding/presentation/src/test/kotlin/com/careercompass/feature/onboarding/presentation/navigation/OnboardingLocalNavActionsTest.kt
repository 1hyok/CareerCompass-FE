package com.careercompass.feature.onboarding.presentation.navigation

import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import com.careercompass.core.ui.navigation.FeatureStackBoundary
import com.careercompass.feature.onboarding.domain.model.OnboardingStep
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * 온보딩 로컬 백스택의 모양 회귀 기준 — Nav2 시절 `popUpTo` 옵션 조합이 만들던 결과를 스택 모양으로 그대로 못박는다(#259).
 *
 * 이관 전에는 그 결과를 `TestNavHostController` 에서 읽었다(`OnboardingNavGraphTest`). 로컬 스택에선 결과 상태를 직접
 * 만들므로 컴포지션이 필요 없고, Robolectric 없이 돈다.
 */
public class OnboardingLocalNavActionsTest {
    private var exits = 0
    private val external = RecordingExternalActions()

    private fun actionsOn(start: OnboardingRoute): Pair<OnboardingLocalNavActions, NavBackStack<NavKey>> {
        val backStack = NavBackStack<NavKey>(start)
        val actions =
            OnboardingLocalNavActions(
                backStack = backStack,
                boundary = FeatureStackBoundary { exits += 1 },
                externalActions = external,
            )
        return actions to backStack
    }

    private fun NavBackStack<NavKey>.shape(): List<String> = map { it::class.simpleName!! }

    @Test
    public fun `신규 가입은 로그인 화면을 남기지 않고 Step 1 하나로 수렴한다`() {
        val (actions, stack) = actionsOn(OnboardingRoute.Login)

        actions.replaceLoginWithOnboarding()

        assertEquals(listOf("Step1"), stack.shape())
    }

    @Test
    public fun `지문 뒤 온보딩 미완료는 지문 화면을 남기지 않는다`() {
        val (actions, stack) = actionsOn(OnboardingRoute.BiometricLogin)

        actions.replaceAuthWithOnboarding()

        assertEquals(listOf("Step1"), stack.shape())
    }

    @Test
    public fun `지문 화면의 다른 방법 로그인은 로그인 하나로 교체한다`() {
        val (actions, stack) = actionsOn(OnboardingRoute.BiometricLogin)

        actions.navigateToLoginFromBiometric()

        assertEquals(listOf("Login"), stack.shape())
        assertEquals(0, external.authSessionExpired)
    }

    @Test
    public fun `지문 뒤 세션 만료는 사유를 먼저 올리고 로그인으로 교체한다`() {
        val (actions, stack) = actionsOn(OnboardingRoute.BiometricLogin)

        actions.navigateToLoginAfterSessionExpiry()

        assertEquals(listOf("Login"), stack.shape())
        assertEquals(1, external.authSessionExpired)
    }

    @Test
    public fun `단계는 순서대로 쌓이고 뒤로가기로 한 칸씩 내려온다`() {
        val (actions, stack) = actionsOn(OnboardingRoute.Step1)

        actions.proceedToStep(OnboardingStep.JobPreference)
        actions.proceedToStep(OnboardingStep.Experience)
        actions.proceedToStep(OnboardingStep.PastApplication)
        assertEquals(listOf("Step1", "Step2", "Step3", "Step4"), stack.shape())

        actions.popBack()
        actions.popBack()
        assertEquals(listOf("Step1", "Step2"), stack.shape())
        assertEquals(0, exits)
    }

    @Test
    public fun `같은 단계를 연달아 요청해도 두 번 쌓이지 않는다`() {
        val (actions, stack) = actionsOn(OnboardingRoute.Step1)

        actions.proceedToStep(OnboardingStep.JobPreference)
        actions.proceedToStep(OnboardingStep.JobPreference)

        assertEquals(listOf("Step1", "Step2"), stack.shape())
    }

    @Test
    public fun `완료 화면은 단계 전부를 걷어내고 하나만 남는다`() {
        val (actions, stack) = actionsOn(OnboardingRoute.Step1)
        actions.proceedToStep(OnboardingStep.JobPreference)
        actions.proceedToStep(OnboardingStep.Experience)
        actions.proceedToStep(OnboardingStep.PastApplication)

        actions.proceedToComplete()

        assertEquals(listOf("Complete"), stack.shape())
    }

    @Test
    public fun `스택 바닥에서의 뒤로가기는 스택을 비우지 않고 셸에 넘긴다`() {
        val (actions, stack) = actionsOn(OnboardingRoute.Step1)

        actions.popBack()

        assertEquals(listOf("Step1"), stack.shape())
        assertEquals(1, exits)
    }

    @Test
    public fun `그래프 밖 이동과 세션 판정은 스택을 건드리지 않고 셸로 나간다`() {
        val (actions, stack) = actionsOn(OnboardingRoute.Login)

        actions.replaceAuthWithFeed()
        actions.replaceOnboardingWithFeed()
        actions.replaceOnboardingWithBoardRegister()
        actions.onSessionEnded()

        assertEquals(2, external.main)
        assertEquals(1, external.boardRegister)
        assertEquals(1, external.sessionEnded)
        assertEquals(listOf("Login"), stack.shape())
    }

    private class RecordingExternalActions : OnboardingExternalActions {
        var main = 0
        var boardRegister = 0
        var authSessionExpired = 0
        var sessionEnded = 0

        override fun navigateToMain() {
            main += 1
        }

        override fun navigateToBoardRegister() {
            boardRegister += 1
        }

        override fun onAuthSessionExpired() {
            authSessionExpired += 1
        }

        override fun onSessionEnded() {
            sessionEnded += 1
        }
    }
}
