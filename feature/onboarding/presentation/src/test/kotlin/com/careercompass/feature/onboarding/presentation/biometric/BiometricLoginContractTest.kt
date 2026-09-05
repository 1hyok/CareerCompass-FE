package com.careercompass.feature.onboarding.presentation.biometric

import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

public class BiometricLoginContractTest {
    @Test
    public fun defaultState_isIdleWithNothingKnownYet() {
        val state = BiometricLoginUiState()

        assertNull(state.userName)
        assertFalse(state.isBiometricEnabled)
        assertFalse(state.isAuthenticating)
        assertNull(state.failure)
        assertNull(state.pendingNavigation)
        assertTrue(state.isActionEnabled)
    }

    @Test
    public fun authenticatingState_disablesBiometricAction() {
        assertFalse(BiometricLoginUiState(userName = "일혁", isAuthenticating = true).isActionEnabled)
        assertTrue(BiometricLoginUiState(userName = "일혁", failure = BiometricFailureReason.Failed).isActionEnabled)
    }
}
