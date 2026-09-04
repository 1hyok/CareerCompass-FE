package com.careercompass.core.network.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** API_SPEC v0.1 §1 `POST /auth/refresh` 요청. 본문 형식은 명세에 없어 `refreshToken` 하나로 가정한다. */
@Serializable
data class RefreshRequestDto(
    @SerialName("refreshToken")
    val refreshToken: String,
) {
    init {
        require(refreshToken.isNotBlank()) { "refreshToken must not be blank" }
    }

    override fun toString(): String = "RefreshRequestDto(refreshToken=<redacted>)"
}

/** `POST /auth/refresh` 응답 — 소셜 로그인과 같은 토큰 묶음. */
@Serializable
data class RefreshDto(
    @SerialName("accessToken")
    val accessToken: String,
    @SerialName("refreshToken")
    val refreshToken: String,
    @SerialName("expiresIn")
    val expiresIn: Long,
) {
    init {
        require(accessToken.isNotBlank()) { "accessToken must not be blank" }
        require(refreshToken.isNotBlank()) { "refreshToken must not be blank" }
        require(expiresIn > 0) { "expiresIn must be greater than zero" }
    }

    override fun toString(): String = "RefreshDto(accessToken=<redacted>, refreshToken=<redacted>, expiresIn=$expiresIn)"
}

/** `POST /auth/logout` 요청 — 토큰 무효화 + FCM 토큰 해제. */
@Serializable
data class LogoutRequestDto(
    @SerialName("refreshToken")
    val refreshToken: String,
) {
    init {
        require(refreshToken.isNotBlank()) { "refreshToken must not be blank" }
    }

    override fun toString(): String = "LogoutRequestDto(refreshToken=<redacted>)"
}

/** `POST /auth/biometric/register` 요청 — 디바이스별 토큰 저장. 본문은 `deviceId` 하나로 가정한다. */
@Serializable
data class BiometricRegisterRequestDto(
    @SerialName("deviceId")
    val deviceId: String,
) {
    init {
        require(deviceId.isNotBlank()) { "deviceId must not be blank" }
    }

    override fun toString(): String = "BiometricRegisterRequestDto(deviceId=<redacted>)"
}
