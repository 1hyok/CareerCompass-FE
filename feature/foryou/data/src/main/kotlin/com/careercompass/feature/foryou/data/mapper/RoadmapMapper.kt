package com.careercompass.feature.foryou.data.mapper

import com.careercompass.core.network.dto.RoadmapComparisonDto
import com.careercompass.core.network.dto.RoadmapMetricDto
import com.careercompass.core.network.dto.RoadmapSuggestionDto
import com.careercompass.feature.foryou.domain.model.RoadmapCohort
import com.careercompass.feature.foryou.domain.model.RoadmapComparison
import com.careercompass.feature.foryou.domain.model.RoadmapMetric
import com.careercompass.feature.foryou.domain.model.RoadmapSuggestion

/**
 * 로드맵 wire → 도메인 변환.
 *
 * 응답의 `cohort` 를 못 읽으면 [requested] 로 되돌린다 — 무엇을 물었는지는 우리가 알고, 모르는 값
 * 하나 때문에 비교표 전체를 버리면 사용자만 손해다.
 */
internal object RoadmapMapper {
    fun toComparison(
        dto: RoadmapComparisonDto,
        requested: RoadmapCohort,
    ): RoadmapComparison =
        RoadmapComparison(
            cohort = RoadmapCohort.fromWireValue(dto.cohort) ?: requested,
            sampleSize = dto.sampleSize,
            metrics = dto.metrics.filter { it.name.isNotBlank() }.map(::toMetric),
            suggestions = dto.suggestions.filter { it.action.isNotBlank() }.map(::toSuggestion),
        )

    private fun toMetric(dto: RoadmapMetricDto): RoadmapMetric =
        RoadmapMetric(
            name = dto.name,
            mine = dto.me,
            cohortAverage = dto.peerAvg,
        )

    private fun toSuggestion(dto: RoadmapSuggestionDto): RoadmapSuggestion =
        RoadmapSuggestion(
            semester = dto.semester,
            action = dto.action,
            expectedLift = dto.expectedLift,
        )
}
