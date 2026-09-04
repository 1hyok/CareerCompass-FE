package com.careercompass.feature.onboarding.presentation.navigation

import com.careercompass.feature.onboarding.domain.model.OnboardingStep
import com.careercompass.feature.onboarding.presentation.flow.OnboardingDestination
import org.junit.Assert.assertEquals
import org.junit.Test

public class OnboardingNavActionsTest {
    @Test
    public fun destinations_mapToShellActions() {
        val calls = mutableListOf<String>()
        val actions = recordingActions(calls)

        actions.navigate(OnboardingDestination.Step(OnboardingStep.Experience))
        actions.navigate(OnboardingDestination.Complete)
        actions.navigate(OnboardingDestination.Feed)
        actions.navigate(OnboardingDestination.BoardRegister)

        assertEquals(listOf("step:Experience", "complete", "feed", "board"), calls)
    }

    @Test
    public fun steps_mapToRoutes() {
        assertEquals(OnboardingRoute.Step1, OnboardingStep.BasicInfo.toRoute())
        assertEquals(OnboardingRoute.Step2, OnboardingStep.JobPreference.toRoute())
        assertEquals(OnboardingRoute.Step3, OnboardingStep.Experience.toRoute())
        assertEquals(OnboardingRoute.Step4, OnboardingStep.PastApplication.toRoute())
    }
}

internal fun recordingActions(calls: MutableList<String>): OnboardingNavActions =
    OnboardingNavActions(
        replaceLoginWithOnboarding = { calls += "login->onboarding" },
        replaceAuthWithFeed = { calls += "auth->feed" },
        replaceAuthWithOnboarding = { calls += "auth->onboarding" },
        navigateToLoginFromBiometric = { calls += "biometric->login" },
        navigateToLoginAfterSessionExpiry = { calls += "biometric->login(expired)" },
        onSessionEnded = { calls += "session-ended" },
        proceedToStep = { calls += "step:${it.name}" },
        proceedToComplete = { calls += "complete" },
        popBack = { calls += "back" },
        replaceOnboardingWithFeed = { calls += "feed" },
        replaceOnboardingWithBoardRegister = { calls += "board" },
    )
