package com.careercompass.core.network.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * API_SPEC v0.1 §7 `GET /feed/for-you` 응답.
 *
 * 목록 둘에는 기본값을 두지 않는다 — 추천이 없으면 서버가 빈 배열을 보내고, 키 자체가 빠졌다면 계약이
 * 바뀐 것이라 파싱이 실패해야 드러난다. 단수인 [topPick] 만 「없음」이 정상값이라 nullable 이다.
 */
@Serializable
data class ForYouFeedDto(
    @SerialName("topPick")
    val topPick: ForYouTopPickDto? = null,
    @SerialName("byStrength")
    val byStrength: List<ForYouPickDto>,
    @SerialName("byGap")
    val byGap: List<ForYouPickDto>,
)

/**
 * 톱 픽 한 건. **`reason` 이 배열이다** — 같은 §7 안에서 [ForYouPickDto.reason] 은 문자열이라 두 스키마가
 * 어긋나 있다.
 *
 * 앱에서 한쪽으로 맞추지 않고 온 모양 그대로 받는다. 서버가 실제로 보내는 것이 계약이고, 여기서 배열을
 * 문자열로 접으면 파싱이 조용히 실패한다. 하나로 모으는 일은 도메인 변환이 한다.
 * 서버 쪽 정리는 [CareerCompass-BE #55](https://github.com/Team-CareerCompass/CareerCompass-BE/issues/55).
 */
@Serializable
data class ForYouTopPickDto(
    @SerialName("postingId")
    val postingId: Long,
    @SerialName("reason")
    val reason: List<String>,
)

/** 강점 기반·취약점 보완 추천 한 건. `reason` 이 문자열 하나다 — [ForYouTopPickDto] 참고. */
@Serializable
data class ForYouPickDto(
    @SerialName("postingId")
    val postingId: Long,
    @SerialName("reason")
    val reason: String,
)
