package com.careercompass.feature.foryou.data

import com.careercompass.core.common.result.runCatchingCancellable
import com.careercompass.core.data.failure.mapDataFailure
import com.careercompass.core.network.model.requireData
import com.careercompass.core.network.service.StrengthExportApiService
import com.careercompass.feature.foryou.data.mapper.StrengthExportMapper
import com.careercompass.feature.foryou.domain.model.ExportFormat
import com.careercompass.feature.foryou.domain.model.ExportSection
import com.careercompass.feature.foryou.domain.model.StrengthExport
import com.careercompass.feature.foryou.domain.repository.StrengthExportRepository
import javax.inject.Inject

/** `POST /export` 구현 — API_SPEC v0.1 §7. 실패는 추천과 같은 §9 표로 번역한다. */
internal class StrengthExportRepositoryImpl
    @Inject
    constructor(
        private val strengthExportApiService: StrengthExportApiService,
    ) : StrengthExportRepository {
        override suspend fun export(
            format: ExportFormat,
            sections: List<ExportSection>,
        ): Result<StrengthExport> =
            runCatchingCancellable {
                StrengthExportMapper.toExport(
                    dto = strengthExportApiService.exportStrengths(StrengthExportMapper.toRequest(format, sections)).requireData(),
                    requested = format,
                )
            }.mapDataFailure()
    }
