package com.careercompass.feature.foryou.domain.usecase

import com.careercompass.feature.foryou.domain.error.ForYouFailure
import com.careercompass.feature.foryou.domain.model.ExportFormat
import com.careercompass.feature.foryou.domain.model.ExportSection
import com.careercompass.feature.foryou.domain.model.StrengthExport
import com.careercompass.feature.foryou.domain.repository.StrengthExportRepository
import javax.inject.Inject

/**
 * 강점 데이터를 내보낸다 — `POST /export`.
 *
 * 구획을 하나도 고르지 않았으면 요청 없이 [ForYouFailure.NoExportSection] 으로 끝낸다 — 빈 문서를
 * 받아 봐야 저장할 것이 없고, 서버 왕복 한 번이 그대로 낭비다.
 * 같은 구획을 두 번 고른 경우는 처음 고른 자리를 남기고 지운다 — 문서에 같은 절이 두 번 실릴 이유가 없다.
 */
public class ExportStrengthsUseCase
    @Inject
    constructor(
        private val strengthExportRepository: StrengthExportRepository,
    ) {
        public suspend operator fun invoke(
            format: ExportFormat,
            sections: List<ExportSection>,
        ): Result<StrengthExport> {
            val requested = sections.distinct()
            if (requested.isEmpty()) return Result.failure(ForYouFailure.NoExportSection())
            return strengthExportRepository.export(format, requested)
        }
    }
