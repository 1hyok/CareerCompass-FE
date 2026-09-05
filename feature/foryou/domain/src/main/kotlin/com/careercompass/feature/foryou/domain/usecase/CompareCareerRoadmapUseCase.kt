package com.careercompass.feature.foryou.domain.usecase

import com.careercompass.feature.foryou.domain.model.RoadmapCohort
import com.careercompass.feature.foryou.domain.model.RoadmapComparison
import com.careercompass.feature.foryou.domain.repository.RoadmapRepository
import javax.inject.Inject

/** 커리어 로드맵 비교 — `GET /roadmap/compare?cohort=`. 집단을 바꾸면 같은 화면이 다시 부른다. */
public class CompareCareerRoadmapUseCase
    @Inject
    constructor(
        private val roadmapRepository: RoadmapRepository,
    ) {
        public suspend operator fun invoke(cohort: RoadmapCohort): Result<RoadmapComparison> = roadmapRepository.compare(cohort)
    }
