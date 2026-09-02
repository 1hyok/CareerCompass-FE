package com.cambridge.core.data.mapper

import com.cambridge.core.model.auth.Session
import com.cambridge.core.model.auth.SocialProvider
import com.cambridge.core.model.auth.TokenBundle
import com.cambridge.core.network.dto.RefreshDto
import com.cambridge.core.network.dto.SocialLoginDto
import com.cambridge.core.network.service.SocialLoginProvider

internal object AuthMapper {
    fun toSession(dto: SocialLoginDto): Session =
        Session(
            accessToken = dto.accessToken,
            refreshToken = dto.refreshToken,
            isNewUser = dto.isNewUser,
            expiresInSeconds = dto.expiresIn,
        )

    fun toTokenBundle(dto: RefreshDto): TokenBundle =
        TokenBundle(accessToken = dto.accessToken, refreshToken = dto.refreshToken, expiresInSeconds = dto.expiresIn)

    fun toWireProvider(provider: SocialProvider): SocialLoginProvider =
        when (provider) {
            SocialProvider.Kakao -> SocialLoginProvider.Kakao
            SocialProvider.Google -> SocialLoginProvider.Google
        }
}
