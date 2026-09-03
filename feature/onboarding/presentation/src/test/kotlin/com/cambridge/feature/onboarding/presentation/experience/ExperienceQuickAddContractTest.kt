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
    public fun detailRules_matchSpecTable() {
        // F1-3 「유형별 입력 필드」 — 사용 기술·링크는 프로젝트에만, 자유 서술 상세는 인턴·대외활동에만.
        assertTrue(ExperienceEditorRules.hasTechTags(ExperienceType.Project))
        assertFalse(ExperienceEditorRules.hasTechTags(ExperienceType.Intern))
        assertTrue(ExperienceEditorRules.hasLink(ExperienceType.Project))
        assertFalse(ExperienceEditorRules.hasLink(ExperienceType.Activity))
        assertTrue(ExperienceEditorRules.hasDetail(ExperienceType.Intern))
        assertTrue(ExperienceEditorRules.hasDetail(ExperienceType.Activity))
        assertFalse(ExperienceEditorRules.hasDetail(ExperienceType.Project))
        // 수상·자격증은 공통 필드만으로 전 필드가 채워져 「자세히」 영역 자체가 없다.
        assertFalse(ExperienceEditorRules.hasDetailSection(ExperienceType.Award))
        assertFalse(ExperienceEditorRules.hasDetailSection(ExperienceType.Certificate))
        assertTrue(ExperienceEditorRules.hasDetailSection(ExperienceType.Project))
        assertTrue(ExperienceEditorRules.hasDetailSection(ExperienceType.Intern))
        assertTrue(ExperienceEditorRules.hasDetailSection(ExperienceType.Activity))
    }

    @Test
    public fun detailLimits_areDeclaredOnce() {
        assertEquals(10, ExperienceEditorRules.MAX_TECH_TAGS)
        assertEquals(20, ExperienceEditorRules.MAX_TECH_TAG_LENGTH)
        assertEquals(200, ExperienceEditorRules.MAX_LINK_LENGTH)
    }

    @Test
    public fun normalizeTechTag_dropsHashAndSpaces() {
        assertEquals("Kotlin", ExperienceEditorRules.normalizeTechTag("  #Kotlin  "))
        assertEquals("Jetpack Compose", ExperienceEditorRules.normalizeTechTag("# Jetpack Compose"))
        assertEquals("", ExperienceEditorRules.normalizeTechTag("   #  "))
    }

    @Test
    public fun isValidLink_acceptsOnlyHttpAndHttps() {
        assertTrue(ExperienceEditorRules.isValidLink("https://github.com/Team-CamBridge/CareerCompass-FE"))
        assertTrue(ExperienceEditorRules.isValidLink(" http://example.com "))
        assertFalse(ExperienceEditorRules.isValidLink("javascript:alert(1)"))
        assertFalse(ExperienceEditorRules.isValidLink("ftp://example.com"))
        assertFalse(ExperienceEditorRules.isValidLink("github.com/foo"))
        assertFalse(ExperienceEditorRules.isValidLink("https://"))
        assertFalse(ExperienceEditorRules.isValidLink(""))
        assertFalse(ExperienceEditorRules.isValidLink("https://example.com/" + "a".repeat(ExperienceEditorRules.MAX_LINK_LENGTH)))
    }

    @Test
    public fun hasDetailValues_seesEachDetailField() {
        assertFalse(ExperienceEditorState().hasDetailValues)
        assertTrue(ExperienceEditorState(techs = listOf("Kotlin")).hasDetailValues)
        assertTrue(ExperienceEditorState(link = "https://example.com").hasDetailValues)
        assertTrue(ExperienceEditorState(detail = "주요 업무").hasDetailValues)
        // 입력칸에만 남은 글자는 아직 태그가 아니다 — 「입력됨」 표시가 붙지 않는다.
        assertFalse(ExperienceEditorState(techInput = "Kotlin").hasDetailValues)
    }

    @Test
    public fun parseYearMonth_acceptsOnlyYearDotMonth() {
        assertEquals(LocalDate.of(2025, 9, 1), ExperienceEditorRules.parseYearMonth(" 2025.09 "))
        assertNull(ExperienceEditorRules.parseYearMonth("2025.13"))
        assertNull(ExperienceEditorRules.parseYearMonth("2025-09"))
        assertNull(ExperienceEditorRules.parseYearMonth(""))
    }
}
