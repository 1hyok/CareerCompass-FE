package com.cambridge.feature.onboarding.presentation.basicinfo

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

public class SchoolPickerContractTest {
    @Test
    public fun blankOrDuplicateResults_areRejected() {
        assertThrows(IllegalArgumentException::class.java) {
            SchoolPickerState(results = listOf("건국대학교", " "))
        }
        assertThrows(IllegalArgumentException::class.java) {
            SchoolPickerState(results = listOf("건국대학교", "건국대학교"))
        }
    }

    @Test
    public fun emptyResults_areAllowed() {
        assertEquals(emptyList<String>(), SchoolPickerState(query = "없음", results = emptyList()).results)
    }
}
