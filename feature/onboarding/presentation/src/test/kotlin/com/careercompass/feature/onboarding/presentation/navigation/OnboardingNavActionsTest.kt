package com.careercompass.feature.onboarding.presentation.navigation

import com.careercompass.feature.onboarding.domain.model.OnboardingStep
import com.careercompass.feature.onboarding.presentation.flow.OnboardingDestination
import org.junit.Assert.assertEquals
import org.junit.Test

public class OnboardingNavActionsTest {
    @Test
    public fun destinations_mapToActions() {
        val actions = RecordingOnboardingNavActions()

        actions.navigate(OnboardingDestination.Step(OnboardingStep.Experience))
        actions.navigate(OnboardingDestination.Complete)
        actions.navigate(OnboardingDestination.Feed)
        actions.navigate(OnboardingDestination.BoardRegister)

        assertEquals(listOf("step:Experience", "complete", "feed", "board"), actions.calls)
    }

    @Test
    public fun steps_mapToRoutes() {
        assertEquals(OnboardingRoute.Step1, OnboardingStep.BasicInfo.toRoute())
        assertEquals(OnboardingRoute.Step2, OnboardingStep.JobPreference.toRoute())
        assertEquals(OnboardingRoute.Step3, OnboardingStep.Experience.toRoute())
        assertEquals(OnboardingRoute.Step4, OnboardingStep.PastApplication.toRoute())
    }
}

/** 호출 순서만 기록하는 [OnboardingNavActions] — `navigate` 의 갈래 매핑을 재는 용도다. */
internal class RecordingOnboardingNavActions : OnboardingNavActions {
    val calls = mutableListOf<String>()

    override fun replaceLoginWithOnboarding() {
        calls += "login->onboarding"
    }

    override fun replaceAuthWithFeed() {
        calls += "auth->feed"
    }

    override fun replaceAuthWithOnboarding() {
        calls += "auth->onboarding"
    }

    override fun navigateToLoginFromBiometric() {
        calls += "biometric->login"
    }

    override fun navigateToLoginAfterSessionExpiry() {
        calls += "biometric->login(expired)"
    }

    override fun onSessionEnded() {
        calls += "session-ended"
    }

    override fun proceedToStep(step: OnboardingStep) {
        calls += "step:${step.name}"
    }

    override fun proceedToComplete() {
        calls += "complete"
    }

    override fun popBack() {
        calls += "back"
    }

    override fun replaceOnboardingWithFeed() {
        calls += "feed"
    }

    override fun replaceOnboardingWithBoardRegister() {
        calls += "board"
    }
}
