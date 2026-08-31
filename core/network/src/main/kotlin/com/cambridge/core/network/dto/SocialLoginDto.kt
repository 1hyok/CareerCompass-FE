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
) {
    init {
        require(accessToken.isNotBlank()) { "accessToken must not be blank" }
        require(deviceId.isNotBlank()) { "deviceId must not be blank" }
        require(fcmToken == null || fcmToken.isNotBlank()) { "fcmToken must be null or non-blank" }
    }

    override fun toString(): String =
        "SocialLoginRequestDto(" +
            "accessToken=<redacted>, " +
            "deviceId=<redacted>, " +
            "fcmToken=${if (fcmToken == null) "null" else "<redacted>"}" +
            ")"
}

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
) {
    init {
        require(accessToken.isNotBlank()) { "accessToken must not be blank" }
        require(refreshToken.isNotBlank()) { "refreshToken must not be blank" }
        require(expiresIn > 0) { "expiresIn must be greater than zero" }
    }

    override fun toString(): String =
        "SocialLoginDto(" +
            "accessToken=<redacted>, " +
            "refreshToken=<redacted>, " +
            "isNewUser=$isNewUser, " +
            "expiresIn=$expiresIn" +
            ")"
}
