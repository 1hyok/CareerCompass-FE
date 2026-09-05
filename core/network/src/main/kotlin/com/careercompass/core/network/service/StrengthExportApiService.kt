package com.careercompass.core.network.service

import com.careercompass.core.network.dto.StrengthExportDto
import com.careercompass.core.network.dto.StrengthExportRequestDto
import com.careercompass.core.network.model.BaseResponse
import retrofit2.http.Body
import retrofit2.http.POST

/** API_SPEC v0.1 §7 — `/export`. */
public interface StrengthExportApiService {
    /** `POST /export` — 고른 구획을 `format` 으로 묶어 돌려준다. */
    @POST("export")
    public suspend fun exportStrengths(
        @Body body: StrengthExportRequestDto,
    ): BaseResponse<StrengthExportDto>
}
