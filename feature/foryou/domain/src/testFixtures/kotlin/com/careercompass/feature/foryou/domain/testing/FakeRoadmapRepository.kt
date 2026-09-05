package com.careercompass.feature.foryou.domain.testing

import com.careercompass.feature.foryou.domain.model.RoadmapCohort
import com.careercompass.feature.foryou.domain.model.RoadmapComparison
import com.careercompass.feature.foryou.domain.repository.RoadmapRepository
import java.util.concurrent.CopyOnWriteArrayList

/** [RoadmapRepository] fake 정본 — 요청한 [RoadmapCohort] 를 순서대로 기록한다. */
public class FakeRoadmapRepository(
    private var result: Result<RoadmapComparison>? = null,
    public var onCompare: (suspend (RoadmapCohort) -> Result<RoadmapComparison>)? = null,
) : RoadmapRepository {
    public val requested: CopyOnWriteArrayList<RoadmapCohort> = CopyOnWriteArrayList()

    public fun returns(value: Result<RoadmapComparison>) {
        result = value
    }

    override suspend fun compare(cohort: RoadmapCohort): Result<RoadmapComparison> {
        requested += cohort
        onCompare?.let { return it(cohort) }
        return checkNotNull(result) { "FakeRoadmapRepository 에 결과를 먼저 넣어야 한다" }
    }
}
