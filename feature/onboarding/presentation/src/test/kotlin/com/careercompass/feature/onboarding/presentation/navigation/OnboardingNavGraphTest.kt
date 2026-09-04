package com.careercompass.feature.onboarding.presentation.navigation

import android.content.Context
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.compose.ComposeNavigator
import androidx.navigation.compose.DialogNavigator
import androidx.navigation.createGraph
import androidx.navigation.testing.TestNavHostController
import androidx.test.core.app.ApplicationProvider
import com.careercompass.feature.onboarding.domain.model.OnboardingStep
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * 그래프 등록만 검증한다 — composable 람다는 실행되지 않으므로 Hilt 없이 라우트 구조를 확인할 수 있다.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
public class OnboardingNavGraphTest {
    private lateinit var navController: TestNavHostController

    @Before
    public fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        navController =
            TestNavHostController(context).apply {
                navigatorProvider.addNavigator(ComposeNavigator())
                navigatorProvider.addNavigator(DialogNavigator())
            }
    }

    @Test
    public fun graph_startsAtGivenDestination() {
        setGraph(startDestination = OnboardingRoute.BiometricLogin)

        val current = navController.currentDestination
        assertNotNull(current)
        assertTrue(current!!.hasRoute<OnboardingRoute.BiometricLogin>())
        assertTrue(navController.currentBackStack.value.any { it.destination.hasRoute<OnboardingGraphRoute>() })
    }

    @Test
    public fun graph_registersEveryOnboardingRoute() {
        setGraph(startDestination = OnboardingRoute.Login)

        navController.navigate(OnboardingRoute.Step1)
        assertTrue(navController.currentDestination!!.hasRoute<OnboardingRoute.Step1>())
        navController.navigate(OnboardingRoute.Step2)
        assertTrue(navController.currentDestination!!.hasRoute<OnboardingRoute.Step2>())
        navController.navigate(OnboardingRoute.Step3)
        assertTrue(navController.currentDestination!!.hasRoute<OnboardingRoute.Step3>())
        navController.navigate(OnboardingRoute.Step4)
        assertTrue(navController.currentDestination!!.hasRoute<OnboardingRoute.Step4>())
        navController.navigate(OnboardingRoute.Complete)
        assertTrue(navController.currentDestination!!.hasRoute<OnboardingRoute.Complete>())
        navController.navigate(OnboardingRoute.BiometricLogin)
        assertTrue(navController.currentDestination!!.hasRoute<OnboardingRoute.BiometricLogin>())
    }

    @Test
    public fun proceedingThroughSteps_buildsBackStackInOrder() {
        setGraph(startDestination = OnboardingRoute.Step1)

        navController.navigate(OnboardingStep.JobPreference.toRoute())
        navController.navigate(OnboardingStep.Experience.toRoute())

        assertTrue(navController.currentDestination!!.hasRoute<OnboardingRoute.Step3>())
        assertTrue(navController.popBackStack())
        assertTrue(navController.currentDestination!!.hasRoute<OnboardingRoute.Step2>())
        assertTrue(navController.popBackStack())
        assertTrue(navController.currentDestination!!.hasRoute<OnboardingRoute.Step1>())
        assertEquals(
            listOf(true),
            listOf(navController.currentBackStack.value.any { it.destination.hasRoute<OnboardingGraphRoute>() }),
        )
    }

    private fun setGraph(startDestination: OnboardingRoute) {
        val graph =
            navController.createGraph(startDestination = OnboardingGraphRoute) {
                onboardingNavGraph(
                    startDestination = startDestination,
                    graphScopedParentEntry = { error("composable lambdas must not run in this test") },
                    actions = recordingActions(mutableListOf()),
                    isSessionExpiryNoticeVisible = false,
                    onSessionExpiryNoticeDismissed = {},
                )
            }
        navController.setGraph(graph, null)
    }
}
