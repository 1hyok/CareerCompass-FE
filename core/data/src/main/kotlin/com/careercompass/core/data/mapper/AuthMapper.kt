package com.careercompass.core.data.mapper

import com.careercompass.core.model.auth.Session
import com.careercompass.core.model.auth.SocialProvider
import com.careercompass.core.model.auth.TokenBundle
import com.careercompass.core.network.dto.RefreshDto
import com.careercompass.core.network.dto.SocialLoginDto
import com.careercompass.core.network.service.SocialLoginProvider

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
