package com.careercompass.core.network.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * API_SPEC v0.1 §7 `POST /export` 요청.
 *
 * 빈 `sections` 는 빈 문서를 부르는 요청이라 아예 보내지 않는다 — 도메인이 먼저 막고, 여기서도 다시 막아
 * wire 계약이 스스로 지켜지게 둔다([BoardDetectRequestDto] 와 같은 자리다).
 */
@Serializable
data class StrengthExportRequestDto(
    @SerialName("format")
    val format: String,
    @SerialName("sections")
    val sections: List<String>,
) {
    init {
        require(format.isNotBlank()) { "format must not be blank" }
        require(sections.isNotEmpty()) { "sections must not be empty" }
    }
}

/** `POST /export` 응답. `format` 은 서버가 실제로 만든 형식이다. */
@Serializable
data class StrengthExportDto(
    @SerialName("format")
    val format: String,
    @SerialName("content")
    val content: String,
)
