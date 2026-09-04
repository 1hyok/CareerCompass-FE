package com.careercompass.core.network.service

import com.careercompass.core.network.dto.ExperienceDto
import com.careercompass.core.network.dto.ExperienceListDto
import com.careercompass.core.network.dto.ExperienceRequestDto
import com.careercompass.core.network.model.BaseResponse
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

/** API_SPEC v0.1 §3 — `/experiences`. */
public interface ExperienceApiService {
    @GET("experiences")
    public suspend fun getExperiences(
        @Query("type") type: String?,
        @Query("cursor") cursor: String?,
        @Query("limit") limit: Int,
    ): BaseResponse<ExperienceListDto>

    @POST("experiences")
    public suspend fun createExperience(
        @Body body: ExperienceRequestDto,
    ): BaseResponse<ExperienceDto>

    @PATCH("experiences/{id}")
    public suspend fun updateExperience(
        @Path("id") id: Long,
        @Body body: ExperienceRequestDto,
    ): BaseResponse<ExperienceDto>

    @DELETE("experiences/{id}")
    public suspend fun deleteExperience(
        @Path("id") id: Long,
    ): BaseResponse<Unit>
}
