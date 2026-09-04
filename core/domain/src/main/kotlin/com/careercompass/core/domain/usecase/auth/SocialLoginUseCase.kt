package com.careercompass.core.domain.usecase.auth

import com.careercompass.core.domain.repository.AuthRepository
import com.careercompass.core.model.auth.SocialProvider
import javax.inject.Inject

/** 소셜 로그인 결과 — 온보딩 진입 여부를 가른다. */
public data class SocialLoginOutcome(
    val isNewUser: Boolean,
)

/**
 * 소셜 SDK 토큰으로 서버 로그인 후 세션을 저장한다.
 *
 * FCM 토큰은 사용자 동의 흐름이 구현될 때까지 보내지 않는다(#41 — 자동 등록 기본 차단).
 */
public class SocialLoginUseCase
    @Inject
    constructor(
        private val authRepository: AuthRepository,
    ) {
        public suspend operator fun invoke(
            provider: SocialProvider,
            providerToken: String,
        ): Result<SocialLoginOutcome> {
            require(providerToken.isNotBlank()) { "providerToken must not be blank" }
            val session =
                authRepository
                    .socialLogin(provider = provider, providerToken = providerToken, fcmToken = null)
                    .getOrElse { return Result.failure(it) }
            return authRepository.saveSession(session).map { SocialLoginOutcome(isNewUser = session.isNewUser) }
        }
    }
