package com.careercompass.feature.onboarding.presentation.biometric

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

public class BiometricLoginContractTest {
    @Test
    public fun defaultState_isIdleWithOptionalFieldsAbsent() {
        val state = BiometricLoginUiState(userName = "일혁", accountLabel = null)

        assertFalse(state.isAuthenticating)
        assertNull(state.accountLabel)
        assertNull(state.errorMessage)
        assertTrue(state.isBiometricEnabled)
    }

    @Test
    public fun authenticatingState_disablesBiometricAction() {
        assertFalse(sampleState().copy(isAuthenticating = true).isBiometricEnabled)
    }

    @Test
    public fun blankRequiredAndOptionalStrings_areRejected() {
        val invalidFactories: List<Pair<String, () -> Any>> =
            listOf(
                "userName must not be blank" to { sampleState().copy(userName = " \t") },
                "accountLabel must be null or non-blank" to { sampleState().copy(accountLabel = "") },
                "errorMessage must be null or non-blank" to { sampleState().copy(errorMessage = " ") },
            )

        invalidFactories.forEach { (expectedMessage, factory) ->
            val exception =
                assertThrows(IllegalArgumentException::class.java) {
                    factory()
                }

            assertEquals(expectedMessage, exception.message)
        }
    }

    private fun sampleState(): BiometricLoginUiState =
        BiometricLoginUiState(
            userName = "일혁",
            accountLabel = "1hyok@konkuk.ac.kr",
        )
}
