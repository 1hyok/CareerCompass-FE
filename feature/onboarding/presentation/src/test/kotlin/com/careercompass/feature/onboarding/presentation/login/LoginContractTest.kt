package com.careercompass.feature.onboarding.presentation.login

import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

public class LoginContractTest {
    @Test
    public fun defaultState_isIdleAndActionable() {
        val state = LoginUiState()

        assertFalse(state.isLoading)
        assertNull(state.failure)
        assertNull(state.pendingNavigation)
        assertFalse(state.isBusy)
        assertTrue(state.isActionEnabled)
    }

    @Test
    public fun loadingState_disablesActions() {
        assertFalse(LoginUiState(isLoading = true).isActionEnabled)
        assertTrue(LoginUiState(isLoading = false, failure = LoginFailureReason.Rejected).isActionEnabled)
    }

    /** 이동이 대기 중인 동안 버튼이 살아 있으면 이미 로그인한 사용자가 SDK 를 한 번 더 열 수 있다. */
    @Test
    public fun pendingNavigation_keepsScreenBusy() {
        val state = LoginUiState(isLoading = false, pendingNavigation = LoginDestination.Feed)

        assertTrue(state.isBusy)
        assertFalse(state.isActionEnabled)
    }
}
