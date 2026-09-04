package com.careercompass.feature.onboarding.presentation.login

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

public class LoginContractTest {
    @Test
    public fun defaultState_isIdleAndActionable() {
        val state = LoginUiState()

        assertFalse(state.isLoading)
        assertNull(state.errorMessage)
        assertTrue(state.isActionEnabled)
    }

    @Test
    public fun loadingState_disablesActions() {
        assertFalse(LoginUiState(isLoading = true).isActionEnabled)
        assertTrue(LoginUiState(isLoading = false, errorMessage = "실패").isActionEnabled)
    }

    @Test
    public fun blankErrorMessage_isRejected() {
        listOf("", " \t").forEach { blank ->
            val exception =
                assertThrows(IllegalArgumentException::class.java) {
                    LoginUiState(errorMessage = blank)
                }

            assertEquals("errorMessage must be null or non-blank", exception.message)
        }
    }

    @Test
    public fun nonBlankErrorMessage_isAccepted() {
        assertEquals(
            "카카오 로그인에 실패했어요",
            LoginUiState(errorMessage = "카카오 로그인에 실패했어요").errorMessage,
        )
    }
}
