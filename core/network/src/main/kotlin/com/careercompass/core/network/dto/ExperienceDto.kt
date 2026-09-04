package com.careercompass.core.network.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject

/**
 * API_SPEC v0.1 §3 경험 카드. `data` 는 유형별 자유 객체라 [JsonObject] 로 받고 data 계층이 유형에 맞춰 해석한다.
 *
 * `createdAt` 은 명세 예시에 없어 조건부(nullable)로 둔다 — 목록 정렬은 서버 순서를 신뢰한다.
 */
@Serializable
data class ExperienceDto(
    @SerialName("id")
    val id: Long,
    @SerialName("type")
    val type: String,
    @SerialName("title")
    val title: String,
    @SerialName("startDate")
    val startDate: String? = null,
    @SerialName("endDate")
    val endDate: String? = null,
    @SerialName("data")
    val data: JsonObject,
    @SerialName("createdAt")
    val createdAt: String? = null,
)

/** `GET /experiences` 응답 — 명세는 페이징 규약(`nextCursor`)만 정하므로 목록 키는 `experiences` 로 가정한다. */
@Serializable
data class ExperienceListDto(
    @SerialName("experiences")
    val experiences: List<ExperienceDto>,
    @SerialName("nextCursor")
    val nextCursor: String? = null,
)

/** `POST /experiences` · `PATCH /experiences/{id}` 요청. */
@Serializable
data class ExperienceRequestDto(
    @SerialName("type")
    val type: String,
    @SerialName("title")
    val title: String,
    @SerialName("startDate")
    val startDate: String? = null,
    @SerialName("endDate")
    val endDate: String? = null,
    @SerialName("data")
    val data: JsonObject,
)
