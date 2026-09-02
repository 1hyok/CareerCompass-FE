package com.cambridge.core.network.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** API_SPEC v0.1 §2 `GET /users/me` 응답. */
@Serializable
data class UserProfileDto(
    @SerialName("id")
    val id: Long,
    @SerialName("name")
    val name: String? = null,
    @SerialName("school")
    val school: String? = null,
    @SerialName("department")
    val department: String? = null,
    @SerialName("gpa")
    val gpa: Double? = null,
    @SerialName("gradYear")
    val gradYear: Int? = null,
    @SerialName("jobInterests")
    val jobInterests: List<JobInterestDto>,
    @SerialName("tags")
    val tags: List<String>,
    @SerialName("onboardingDone")
    val onboardingDone: Boolean,
    @SerialName("completion")
    val completion: Int,
)

@Serializable
data class JobInterestDto(
    @SerialName("code")
    val code: String,
    @SerialName("priority")
    val priority: Int,
)

/** `PATCH /users/me` — null 인 필드는 직렬화하지 않는다(`encodeDefaults = false`). */
@Serializable
data class UpdateProfileRequestDto(
    @SerialName("name")
    val name: String? = null,
    @SerialName("school")
    val school: String? = null,
    @SerialName("department")
    val department: String? = null,
    @SerialName("gpa")
    val gpa: Double? = null,
    @SerialName("gradYear")
    val gradYear: Int? = null,
)

/** `PUT /users/me/job-interests`. */
@Serializable
data class JobInterestsRequestDto(
    @SerialName("interests")
    val interests: List<JobInterestDto>,
)

/** `PUT /users/me/tags`. */
@Serializable
data class TagsRequestDto(
    @SerialName("tags")
    val tags: List<String>,
)
