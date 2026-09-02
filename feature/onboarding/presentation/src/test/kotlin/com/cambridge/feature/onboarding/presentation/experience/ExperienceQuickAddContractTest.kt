package com.cambridge.feature.onboarding.presentation.experience

import com.cambridge.core.model.experience.ExperienceType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

public class ExperienceQuickAddContractTest {
    @Test
    public fun submitEnabled_requiresTitleAndIdle() {
        assertFalse(ExperienceEditorState().isSubmitEnabled)
        assertTrue(ExperienceEditorState(title = "제목").isSubmitEnabled)
        assertFalse(ExperienceEditorState(title = "제목", isSubmitting = true).isSubmitEnabled)
        assertFalse(ExperienceEditorState(isSubmitting = true).isInputEnabled)
    }

    @Test
    public fun rules_followSpecTable() {
        assertTrue(ExperienceEditorRules.isStartDateRequired(ExperienceType.Project))
        assertTrue(ExperienceEditorRules.isStartDateRequired(ExperienceType.Intern))
        assertFalse(ExperienceEditorRules.isStartDateRequired(ExperienceType.Award))
        assertFalse(ExperienceEditorRules.hasEndDate(ExperienceType.Award))
        assertFalse(ExperienceEditorRules.hasEndDate(ExperienceType.Certificate))
        assertTrue(ExperienceEditorRules.hasEndDate(ExperienceType.Activity))
        assertTrue(ExperienceEditorRules.isPrimaryRequired(ExperienceType.Award))
        assertTrue(ExperienceEditorRules.isPrimaryRequired(ExperienceType.Intern))
        assertTrue(ExperienceEditorRules.isPrimaryRequired(ExperienceType.Activity))
        assertFalse(ExperienceEditorRules.isPrimaryRequired(ExperienceType.Project))
        assertFalse(ExperienceEditorRules.hasSecondary(ExperienceType.Certificate))
        assertTrue(ExperienceEditorRules.isSecondaryRequired(ExperienceType.Intern))
        assertFalse(ExperienceEditorRules.isSecondaryRequired(ExperienceType.Project))
    }

    @Test
    public fun parseYearMonth_acceptsOnlyYearDotMonth() {
        assertEquals(LocalDate.of(2025, 9, 1), ExperienceEditorRules.parseYearMonth(" 2025.09 "))
        assertNull(ExperienceEditorRules.parseYearMonth("2025.13"))
        assertNull(ExperienceEditorRules.parseYearMonth("2025-09"))
        assertNull(ExperienceEditorRules.parseYearMonth(""))
    }
}
