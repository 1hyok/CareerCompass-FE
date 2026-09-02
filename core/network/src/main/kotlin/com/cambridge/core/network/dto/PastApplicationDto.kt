package com.cambridge.core.network.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** API_SPEC v0.1 §4 과거 지원서. */
@Serializable
data class PastApplicationDto(
    @SerialName("id")
    val id: Long,
    @SerialName("label")
    val label: String,
    @SerialName("filePath")
    val filePath: String? = null,
    @SerialName("items")
    val items: List<PastApplicationItemDto>,
    @SerialName("createdAt")
    val createdAt: String? = null,
)

@Serializable
data class PastApplicationItemDto(
    @SerialName("id")
    val id: Long,
    @SerialName("category")
    val category: String,
    @SerialName("content")
    val content: String,
    @SerialName("confident")
    val confident: Boolean,
)

/** `GET /past-applications` 응답 — 목록 키는 `applications` 로 가정한다. */
@Serializable
data class PastApplicationListDto(
    @SerialName("applications")
    val applications: List<PastApplicationDto>,
)

/** `PATCH /past-applications/{appId}/items/{itemId}`. */
@Serializable
data class UpdateItemCategoryRequestDto(
    @SerialName("category")
    val category: String,
)
