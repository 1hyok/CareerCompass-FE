package com.careercompass.feature.foryou.data.mapper

import com.careercompass.core.network.dto.RoadmapComparisonDto
import com.careercompass.core.network.dto.RoadmapMetricDto
import com.careercompass.core.network.dto.RoadmapSuggestionDto
import com.careercompass.feature.foryou.domain.model.RoadmapCohort
import org.junit.Assert.assertEquals
import org.junit.Test

class RoadmapMapperTest {
    @Test
    fun `지표는 내 값과 집단 평균으로 읽는다`() {
        val comparison = RoadmapMapper.toComparison(dto(cohort = "peer"), requested = RoadmapCohort.Peer)

        assertEquals(RoadmapCohort.Peer, comparison.cohort)
        assertEquals(86, comparison.sampleSize)
        assertEquals(4.0, comparison.metrics.first().mine, 0.0)
        assertEquals(0.8, comparison.metrics.last().cohortAverage, 0.0)
        assertEquals("3-2", comparison.suggestions.single().semester)
    }

    @Test
    fun `모르는 cohort 는 물어본 값으로 되돌린다`() {
        val comparison = RoadmapMapper.toComparison(dto(cohort = "classmates"), requested = RoadmapCohort.Senior)

        assertEquals(RoadmapCohort.Senior, comparison.cohort)
        assertEquals(2, comparison.metrics.size)
    }

    @Test
    fun `이름 없는 지표와 내용 없는 제안은 버린다`() {
        val comparison =
            RoadmapMapper.toComparison(
                RoadmapComparisonDto(
                    cohort = "peer",
                    sampleSize = 1,
                    metrics = listOf(RoadmapMetricDto(name = " ", me = 1.0, peerAvg = 2.0)),
                    suggestions = listOf(RoadmapSuggestionDto(semester = "3-2", action = " ", expectedLift = 0)),
                ),
                requested = RoadmapCohort.Peer,
            )

        assertEquals(emptyList<Any>(), comparison.metrics)
        assertEquals(emptyList<Any>(), comparison.suggestions)
    }

    private fun dto(cohort: String): RoadmapComparisonDto =
        RoadmapComparisonDto(
            cohort = cohort,
            sampleSize = 86,
            metrics =
                listOf(
                    RoadmapMetricDto(name = "프로젝트 수", me = 4.0, peerAvg = 2.0),
                    RoadmapMetricDto(name = "인턴 경험", me = 0.0, peerAvg = 0.8),
                ),
            suggestions = listOf(RoadmapSuggestionDto(semester = "3-2", action = "SQLD + 토익 800+", expectedLift = 12)),
        )
}
