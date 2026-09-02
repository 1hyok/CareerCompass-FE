package com.cambridge.core.domain.repository

import com.cambridge.core.model.auth.Session
import com.cambridge.core.model.auth.SocialProvider
import com.cambridge.core.model.auth.TokenBundle
import kotlinx.coroutines.flow.Flow

/**
 * 인증 세션 계약 — API_SPEC v0.1 §1.
 *
 * 실패는 [Result] 로 돌려주고, 사유가 확인된 것은 [com.cambridge.core.domain.error.CoreAuthFailure] 로 번역돼 있다.
 */
public interface AuthRepository {
    /** 액세스 토큰 보유 여부. 세션 정리 시 false 로 바뀐다. */
    public val isLoggedIn: Flow<Boolean>

    /** 이 기기에서 지문 로그인을 켰는지(DEVICE 스코프). */
    public val isBiometricEnabled: Flow<Boolean>

    /** `POST /auth/social/{provider}` — 성공해도 세션은 저장하지 않는다. 저장은 use case 가 [saveSession] 으로 한다. */
    public suspend fun socialLogin(
        provider: SocialProvider,
        providerToken: String,
        fcmToken: String?,
    ): Result<Session>

    public suspend fun saveSession(session: Session): Result<Unit>

    public suspend fun getAccessToken(): Result<String?>

    public suspend fun getRefreshToken(): Result<String?>

    /** `POST /auth/refresh` — 저장된 refresh 토큰으로 회전하고 새 토큰을 저장한다. */
    public suspend fun rotateToken(): Result<TokenBundle>

    /** `POST /auth/logout` best-effort 후 SESSION 스코프 저장소를 비운다. */
    public suspend fun logout(): Result<Unit>

    /** 서버 호출 없이 로컬 세션만 정리한다 — refresh 거절 등 되돌릴 수 없는 실패용. */
    public suspend fun clearSession(): Result<Unit>

    /** `POST /auth/biometric/register` 후 기기 플래그를 켠다. */
    public suspend fun registerBiometric(): Result<Unit>

    public suspend fun setBiometricEnabled(enabled: Boolean): Result<Unit>
}
