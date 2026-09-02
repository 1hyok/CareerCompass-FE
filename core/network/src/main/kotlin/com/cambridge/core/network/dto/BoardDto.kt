package com.cambridge.core.network.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject

/** API_SPEC v0.1 §5 `POST /boards/detect` 요청. */
@Serializable
data class BoardDetectRequestDto(
    @SerialName("url")
    val url: String,
) {
    init {
        require(url.isNotBlank()) { "url must not be blank" }
    }
}

@Serializable
data class BoardPreviewItemDto(
    @SerialName("title")
    val title: String,
    @SerialName("url")
    val url: String,
    @SerialName("date")
    val date: String? = null,
)

/** `POST /boards/detect` 응답. 실패 상태에서는 `selectors`·`preview` 가 없을 수 있다. */
@Serializable
data class BoardDetectionDto(
    @SerialName("detectStatus")
    val detectStatus: String,
    @SerialName("selectors")
    val selectors: JsonObject? = null,
    @SerialName("preview")
    val preview: List<BoardPreviewItemDto>? = null,
)

/** `POST /boards` 등록 확정 요청. */
@Serializable
data class BoardRegisterRequestDto(
    @SerialName("url")
    val url: String,
    @SerialName("name")
    val name: String,
    @SerialName("type")
    val type: String,
    @SerialName("cycleHours")
    val cycleHours: Int,
)

/** 게시판 — `GET /boards` 항목·`POST /boards`·`PATCH /boards/{id}` 응답. */
@Serializable
data class BoardDto(
    @SerialName("id")
    val id: Long,
    @SerialName("url")
    val url: String,
    @SerialName("name")
    val name: String,
    @SerialName("type")
    val type: String,
    @SerialName("cycleHours")
    val cycleHours: Int,
    @SerialName("isActive")
    val isActive: Boolean,
    @SerialName("status")
    val status: String,
    @SerialName("failCount")
    val failCount: Int,
    @SerialName("lastCollectedAt")
    val lastCollectedAt: String? = null,
)

/** `GET /boards` 응답 — 목록 키는 `boards` 로 가정한다. */
@Serializable
data class BoardListDto(
    @SerialName("boards")
    val boards: List<BoardDto>,
)

/** `PATCH /boards/{id}` — null 인 필드는 직렬화하지 않는다. */
@Serializable
data class BoardUpdateRequestDto(
    @SerialName("name")
    val name: String? = null,
    @SerialName("type")
    val type: String? = null,
    @SerialName("cycleHours")
    val cycleHours: Int? = null,
    @SerialName("isActive")
    val isActive: Boolean? = null,
)
