package com.careercompass.core.network.service

import com.careercompass.core.network.dto.BiometricRegisterRequestDto
import com.careercompass.core.network.dto.LogoutRequestDto
import com.careercompass.core.network.dto.RefreshDto
import com.careercompass.core.network.dto.RefreshRequestDto
import com.careercompass.core.network.dto.SocialLoginDto
import com.careercompass.core.network.dto.SocialLoginRequestDto
import com.careercompass.core.network.model.BaseResponse
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Path

public interface AuthApiService {
    /** API_SPEC v0.1 §1 — `POST /auth/social/{provider}`. */
    @POST("auth/social/{provider}")
    public suspend fun socialLogin(
        @Path("provider") provider: SocialLoginProvider,
        @Body body: SocialLoginRequestDto,
    ): BaseResponse<SocialLoginDto>

    /** API_SPEC v0.1 §1 — `POST /auth/logout`. 토큰 무효화 + FCM 토큰 해제. */
    @POST("auth/logout")
    public suspend fun logout(
        @Body body: LogoutRequestDto,
    ): BaseResponse<Unit>

    /** API_SPEC v0.1 §1 — `POST /auth/biometric/register`. */
    @POST("auth/biometric/register")
    public suspend fun registerBiometric(
        @Body body: BiometricRegisterRequestDto,
    ): BaseResponse<Unit>
}

/**
 * 토큰 재발급 전용 — 액세스 토큰을 붙이지 않는 `RefreshClient` 로 호출한다.
 * 같은 서비스에 두면 만료 토큰이 헤더에 실려 401 → 재발급 → 401 이 반복된다.
 */
public interface TokenApiService {
    /** API_SPEC v0.1 §1 — `POST /auth/refresh`. */
    @POST("auth/refresh")
    public suspend fun refresh(
        @Body body: RefreshRequestDto,
    ): BaseResponse<RefreshDto>
}
