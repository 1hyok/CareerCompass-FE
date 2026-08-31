package com.cambridge.core.network.service

import com.cambridge.core.network.dto.SocialLoginDto
import com.cambridge.core.network.dto.SocialLoginRequestDto
import com.cambridge.core.network.model.BaseResponse
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
}
