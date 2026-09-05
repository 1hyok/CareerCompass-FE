package com.careercompass.feature.foryou.domain.model

/** 비교 대상 집단 — API_SPEC v0.1 §7 `GET /roadmap/compare` 의 `cohort`. */
public enum class RoadmapCohort(
    public val wireValue: String,
) {
    /** 같은 학과·학년 동기. */
    Peer("peer"),

    /** 합격 선배. */
    Senior("senior"),

    /** 내 성장 — 비교 대상이 과거의 나다. */
    MeOnly("me_only"),
    ;

    public companion object {
        public fun fromWireValue(value: String): RoadmapCohort? = entries.firstOrNull { it.wireValue == value }
    }
}

/**
 * 커리어 로드맵 비교 결과 — §7 `GET /roadmap/compare`.
 *
 * [sampleSize] 는 비교에 쓰인 표본 수다. 표본이 적으면 화면이 값을 그대로 믿지 말라고 알려야 한다.
 */
public data class RoadmapComparison(
    public val cohort: RoadmapCohort,
    public val sampleSize: Int,
    public val metrics: List<RoadmapMetric>,
    public val suggestions: List<RoadmapSuggestion>,
)

/**
 * 지표 한 줄 — 내 값과 비교 집단의 평균.
 *
 * 서버 키는 cohort 가 무엇이든 `peerAvg` 지만 뜻은 「고른 집단의 평균」이라 [cohortAverage] 로 읽는다.
 * 값이 실수인 것은 계약이다 — 「인턴 경험 0.8회」처럼 평균은 정수로 떨어지지 않는다.
 */
public data class RoadmapMetric(
    public val name: String,
    public val mine: Double,
    public val cohortAverage: Double,
)

/** 학기별 실행 제안. [expectedLift] 는 적합도 점수의 예상 상승폭(점). */
public data class RoadmapSuggestion(
    public val semester: String,
    public val action: String,
    public val expectedLift: Int,
)
