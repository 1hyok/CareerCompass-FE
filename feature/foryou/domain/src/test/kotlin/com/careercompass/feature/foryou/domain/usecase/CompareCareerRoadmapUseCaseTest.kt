package com.careercompass.feature.foryou.domain.usecase

import com.careercompass.feature.foryou.domain.model.RoadmapCohort
import com.careercompass.feature.foryou.domain.model.RoadmapComparison
import com.careercompass.feature.foryou.domain.model.RoadmapMetric
import com.careercompass.feature.foryou.domain.testing.FakeRoadmapRepository
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Test
import java.io.IOException

class CompareCareerRoadmapUseCaseTest {
    private val repository = FakeRoadmapRepository()
    private val useCase = CompareCareerRoadmapUseCase(repository)

    @Test
    fun `고른 집단을 그대로 물어본다`() =
        runTest {
            repository.returns(
                Result.success(
                    RoadmapComparison(
                        cohort = RoadmapCohort.Senior,
                        sampleSize = 12,
                        metrics = listOf(RoadmapMetric(name = "프로젝트 수", mine = 4.0, cohortAverage = 2.0)),
                        suggestions = emptyList(),
                    ),
                ),
            )

            val comparison = useCase(RoadmapCohort.Senior).getOrThrow()

            assertEquals(listOf(RoadmapCohort.Senior), repository.requested.toList())
            assertEquals(12, comparison.sampleSize)
        }

    @Test
    fun `실패는 그대로 흘려보낸다`() =
        runTest {
            val failure = IOException("offline")
            repository.onCompare = { Result.failure(failure) }

            assertSame(failure, useCase(RoadmapCohort.MeOnly).exceptionOrNull())
        }
}
