package com.careercompass.feature.foryou.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/** wire 값은 API_SPEC v0.1 §7 의 문자열 그대로여야 한다 — 서버와 앱이 같은 낱말을 써야 요청이 통한다. */
class ForYouWireValueTest {
    @Test
    fun `cohort 는 명세의 세 값을 그대로 쓴다`() {
        assertEquals(listOf("peer", "senior", "me_only"), RoadmapCohort.entries.map(RoadmapCohort::wireValue))
        assertEquals(RoadmapCohort.MeOnly, RoadmapCohort.fromWireValue("me_only"))
        assertNull(RoadmapCohort.fromWireValue("classmates"))
    }

    @Test
    fun `export 형식은 명세의 네 값을 그대로 쓴다`() {
        assertEquals(listOf("markdown", "notion", "html", "plain"), ExportFormat.entries.map(ExportFormat::wireValue))
        assertEquals(ExportFormat.Html, ExportFormat.fromWireValue("html"))
        assertNull(ExportFormat.fromWireValue("pdf"))
    }

    @Test
    fun `export 구획은 명세의 다섯 값을 그대로 쓴다`() {
        assertEquals(
            listOf("basic", "skills", "projects", "awards", "summary"),
            ExportSection.entries.map(ExportSection::wireValue),
        )
        assertNull(ExportSection.fromWireValue("cover_letter"))
    }
}
