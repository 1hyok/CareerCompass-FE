package com.careercompass.feature.foryou.domain.repository

import com.careercompass.feature.foryou.domain.model.RoadmapCohort
import com.careercompass.feature.foryou.domain.model.RoadmapComparison

/** 커리어 로드맵 비교 계약 — API_SPEC v0.1 §7 `GET /roadmap/compare`. */
public interface RoadmapRepository {
    /** 고른 집단과 나를 비교한다. 추천과 같은 근거를 쓰므로 실패도 같은 사유로 온다. */
    public suspend fun compare(cohort: RoadmapCohort): Result<RoadmapComparison>
}
