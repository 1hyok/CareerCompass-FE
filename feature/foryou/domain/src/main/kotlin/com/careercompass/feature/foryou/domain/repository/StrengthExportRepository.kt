package com.careercompass.feature.foryou.domain.repository

import com.careercompass.feature.foryou.domain.model.ExportFormat
import com.careercompass.feature.foryou.domain.model.ExportSection
import com.careercompass.feature.foryou.domain.model.StrengthExport

/** 강점 Export 계약 — API_SPEC v0.1 §7 `POST /export`. */
public interface StrengthExportRepository {
    /** 고른 [sections] 를 [format] 으로 묶는다. 빈 목록은 호출부가 먼저 막는다. */
    public suspend fun export(
        format: ExportFormat,
        sections: List<ExportSection>,
    ): Result<StrengthExport>
}
