package com.careercompass.feature.foryou.data

import com.careercompass.core.domain.error.CoreDataFailure
import com.careercompass.core.network.dto.RoadmapComparisonDto
import com.careercompass.core.network.dto.RoadmapMetricDto
import com.careercompass.core.network.model.ApiException
import com.careercompass.core.network.model.BaseResponse
import com.careercompass.core.network.service.RoadmapApiService
import com.careercompass.feature.foryou.domain.model.RoadmapCohort
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RoadmapRepositoryImplTest {
    private class FakeRoadmapApi(
        private val throws: Throwable? = null,
    ) : RoadmapApiService {
        val requested = mutableListOf<String>()

        override suspend fun compareRoadmap(cohort: String): BaseResponse<RoadmapComparisonDto> {
            requested += cohort
            throws?.let { throw it }
            return BaseResponse(
                ok = true,
                data =
                    RoadmapComparisonDto(
                        cohort = cohort,
                        sampleSize = 86,
                        metrics = listOf(RoadmapMetricDto(name = "프로젝트 수", me = 4.0, peerAvg = 2.0)),
                        suggestions = emptyList(),
                    ),
            )
        }
    }

    @Test
    fun `집단은 명세의 wire 값으로 실린다`() =
        runTest {
            val api = FakeRoadmapApi()

            val comparison = RoadmapRepositoryImpl(api).compare(RoadmapCohort.MeOnly).getOrThrow()

            assertEquals(listOf("me_only"), api.requested)
            assertEquals(RoadmapCohort.MeOnly, comparison.cohort)
            assertEquals(4.0, comparison.metrics.single().mine, 0.0)
        }

    @Test
    fun `로드맵도 422 를 같은 사유로 접는다`() =
        runTest {
            val api =
                FakeRoadmapApi(
                    throws =
                        ApiException(
                            code = "PROFILE_INCOMPLETE",
                            serverMessage = null,
                            fallbackMessage = "요청에 실패했습니다.",
                            status = 422,
                        ),
                )

            val failure = RoadmapRepositoryImpl(api).compare(RoadmapCohort.Peer).exceptionOrNull()

            assertTrue("실제 사유: $failure", failure is CoreDataFailure.ProfileIncomplete)
        }
}
