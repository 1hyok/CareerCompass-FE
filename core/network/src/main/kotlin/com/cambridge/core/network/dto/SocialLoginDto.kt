package com.cambridge.core.network.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class SocialLoginRequestDto(
    @SerialName("accessToken")
    val accessToken: String,
    @SerialName("deviceId")
    val deviceId: String,
    @SerialName("fcmToken")
    val fcmToken: String? = null,
)

@Serializable
data class SocialLoginDto(
    @SerialName("accessToken")
    val accessToken: String,
    @SerialName("refreshToken")
    val refreshToken: String,
    @SerialName("isNewUser")
    val isNewUser: Boolean,
    @SerialName("expiresIn")
    val expiresIn: Long,
)
