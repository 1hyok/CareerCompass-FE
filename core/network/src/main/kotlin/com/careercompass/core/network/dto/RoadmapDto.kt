package com.careercompass.core.network.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** API_SPEC v0.1 §7 `GET /roadmap/compare` 응답. */
@Serializable
data class RoadmapComparisonDto(
    @SerialName("cohort")
    val cohort: String,
    @SerialName("sampleSize")
    val sampleSize: Int,
    @SerialName("metrics")
    val metrics: List<RoadmapMetricDto>,
    @SerialName("suggestions")
    val suggestions: List<RoadmapSuggestionDto>,
)

/**
 * 지표 한 줄. 명세 예시가 `"me": 4` 와 `"peerAvg": 0.8` 을 한 응답에 함께 쓰므로 정수로 받을 수 없다.
 *
 * 비교 대상 값의 키는 `cohort` 가 무엇이든 `peerAvg` 다 — 선배·내 성장 비교에서도 이름은 그대로다.
 */
@Serializable
data class RoadmapMetricDto(
    @SerialName("name")
    val name: String,
    @SerialName("me")
    val me: Double,
    @SerialName("peerAvg")
    val peerAvg: Double,
)

/** 학기별 실행 제안. `expectedLift` 는 적합도 점수의 예상 상승폭(점). */
@Serializable
data class RoadmapSuggestionDto(
    @SerialName("semester")
    val semester: String,
    @SerialName("action")
    val action: String,
    @SerialName("expectedLift")
    val expectedLift: Int,
)
