package com.careercompass.feature.foryou.data.mapper

import com.careercompass.core.network.dto.StrengthExportDto
import com.careercompass.feature.foryou.domain.model.ExportFormat
import com.careercompass.feature.foryou.domain.model.ExportSection
import org.junit.Assert.assertEquals
import org.junit.Test

class StrengthExportMapperTest {
    @Test
    fun `요청은 고른 순서 그대로 wire 값으로 옮긴다`() {
        val request =
            StrengthExportMapper.toRequest(
                format = ExportFormat.Markdown,
                sections = listOf(ExportSection.Basic, ExportSection.Projects),
            )

        assertEquals("markdown", request.format)
        assertEquals(listOf("basic", "projects"), request.sections)
    }

    @Test
    fun `응답 형식이 요청과 달라도 서버가 만든 형식을 따른다`() {
        val export =
            StrengthExportMapper.toExport(
                StrengthExportDto(format = "html", content = "<h1>정일혁</h1>"),
                requested = ExportFormat.Markdown,
            )

        assertEquals(ExportFormat.Html, export.format)
    }

    @Test
    fun `모르는 형식은 요청한 형식으로 되돌린다`() {
        val export =
            StrengthExportMapper.toExport(
                StrengthExportDto(format = "pdf", content = "본문"),
                requested = ExportFormat.Plain,
            )

        assertEquals(ExportFormat.Plain, export.format)
        assertEquals("본문", export.content)
    }
}
