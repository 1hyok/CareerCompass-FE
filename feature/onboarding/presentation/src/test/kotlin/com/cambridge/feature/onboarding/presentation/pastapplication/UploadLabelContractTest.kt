package com.cambridge.feature.onboarding.presentation.pastapplication

import com.careercompass.core.model.application.UploadFile
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.ByteArrayInputStream

public class UploadLabelContractTest {
    @Test
    public fun submitEnabled_requiresNonBlankLabel() {
        assertFalse(state(label = "").isSubmitEnabled)
        assertFalse(state(label = "   ").isSubmitEnabled)
        assertTrue(state(label = "2024 카카오 인턴 자소서").isSubmitEnabled)
    }

    @Test
    public fun fileName_comesFromTheSelectedFile() {
        assertEquals("이력서_최종_v3(2).pdf", state(label = "라벨").fileName)
    }

    private fun state(label: String) =
        UploadLabelState(
            file = UploadFile(fileName = "이력서_최종_v3(2).pdf", sizeBytes = 16L) { ByteArrayInputStream(ByteArray(16)) },
            label = label,
        )
}
