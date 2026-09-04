package com.careercompass.core.model.auth

/** 앱이 지원하는 소셜 로그인 제공자. wire 값은 network 계층이 갖는다. */
public enum class SocialProvider {
    Kakao,
    Google,
}

/**
 * 소셜 로그인 성공 결과.
 *
 * @property isNewUser 이번 로그인으로 가입된 신규 사용자인지 — 온보딩 진입 여부를 가른다.
 * @property expiresInSeconds 액세스 토큰 잔여 수명(초). 서버가 생략하면 null.
 */
public data class Session(
    val accessToken: String,
    val refreshToken: String,
    val isNewUser: Boolean,
    val expiresInSeconds: Long?,
) {
    init {
        require(accessToken.isNotBlank()) { "accessToken must not be blank" }
        require(refreshToken.isNotBlank()) { "refreshToken must not be blank" }
        require(expiresInSeconds == null || expiresInSeconds > 0) { "expiresInSeconds must be null or positive" }
    }

    override fun toString(): String =
        "Session(accessToken=<redacted>, refreshToken=<redacted>, isNewUser=$isNewUser, expiresInSeconds=$expiresInSeconds)"
}

/** 토큰 재발급 성공 결과. */
public data class TokenBundle(
    val accessToken: String,
    val refreshToken: String,
    val expiresInSeconds: Long?,
) {
    init {
        require(accessToken.isNotBlank()) { "accessToken must not be blank" }
        require(refreshToken.isNotBlank()) { "refreshToken must not be blank" }
        require(expiresInSeconds == null || expiresInSeconds > 0) { "expiresInSeconds must be null or positive" }
    }

    override fun toString(): String = "TokenBundle(accessToken=<redacted>, refreshToken=<redacted>, expiresInSeconds=$expiresInSeconds)"
}
