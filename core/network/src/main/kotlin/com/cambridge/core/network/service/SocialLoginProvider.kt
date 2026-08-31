package com.cambridge.core.network.service

/** Social login providers supported by the API wire contract. */
public enum class SocialLoginProvider(
    private val wireValue: String,
) {
    Kakao("kakao"),
    Google("google"),
    ;

    /** Retrofit converts `@Path` values through [toString]. */
    override fun toString(): String = wireValue
}
