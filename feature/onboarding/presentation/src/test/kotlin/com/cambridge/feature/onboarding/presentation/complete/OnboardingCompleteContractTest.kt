package com.cambridge.feature.onboarding.presentation.complete

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Test

public class OnboardingCompleteContractTest {
    @Test
    public fun defaultState_hasNoUserName() {
        assertNull(OnboardingCompleteUiState().userName)
    }

    @Test
    public fun blankUserName_isRejected() {
        listOf("", " \t").forEach { blank ->
            val exception =
                assertThrows(IllegalArgumentException::class.java) {
                    OnboardingCompleteUiState(userName = blank)
                }

            assertEquals("userName must be null or non-blank", exception.message)
        }
    }

    @Test
    public fun nonBlankUserName_isAccepted() {
        assertEquals("일혁", OnboardingCompleteUiState(userName = "일혁").userName)
    }
}
