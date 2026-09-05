package com.careercompass.feature.foryou.data.mapper

import com.careercompass.core.network.dto.StrengthExportDto
import com.careercompass.core.network.dto.StrengthExportRequestDto
import com.careercompass.feature.foryou.domain.model.ExportFormat
import com.careercompass.feature.foryou.domain.model.ExportSection
import com.careercompass.feature.foryou.domain.model.StrengthExport

/**
 * 강점 Export wire ↔ 도메인 변환.
 *
 * 응답의 `format` 을 못 읽으면 [requested] 로 되돌린다 — 내용은 이미 받았고, 형식 이름 하나 때문에
 * 문서를 버릴 이유가 없다.
 */
internal object StrengthExportMapper {
    fun toRequest(
        format: ExportFormat,
        sections: List<ExportSection>,
    ): StrengthExportRequestDto =
        StrengthExportRequestDto(
            format = format.wireValue,
            sections = sections.map(ExportSection::wireValue),
        )

    fun toExport(
        dto: StrengthExportDto,
        requested: ExportFormat,
    ): StrengthExport =
        StrengthExport(
            format = ExportFormat.fromWireValue(dto.format) ?: requested,
            content = dto.content,
        )
}
