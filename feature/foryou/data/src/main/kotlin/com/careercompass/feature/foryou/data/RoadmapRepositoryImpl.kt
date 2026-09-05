package com.careercompass.feature.foryou.data

import com.careercompass.core.common.result.runCatchingCancellable
import com.careercompass.core.data.failure.mapDataFailure
import com.careercompass.core.network.model.requireData
import com.careercompass.core.network.service.RoadmapApiService
import com.careercompass.feature.foryou.data.mapper.RoadmapMapper
import com.careercompass.feature.foryou.domain.model.RoadmapCohort
import com.careercompass.feature.foryou.domain.model.RoadmapComparison
import com.careercompass.feature.foryou.domain.repository.RoadmapRepository
import javax.inject.Inject

/** `GET /roadmap/compare` 구현 — API_SPEC v0.1 §7. 실패는 추천과 같은 §9 표로 번역한다. */
internal class RoadmapRepositoryImpl
    @Inject
    constructor(
        private val roadmapApiService: RoadmapApiService,
    ) : RoadmapRepository {
        override suspend fun compare(cohort: RoadmapCohort): Result<RoadmapComparison> =
            runCatchingCancellable {
                RoadmapMapper.toComparison(
                    dto = roadmapApiService.compareRoadmap(cohort.wireValue).requireData(),
                    requested = cohort,
                )
            }.mapDataFailure()
    }
