package com.careercompass.core.network.service

import com.careercompass.core.network.dto.JobInterestsRequestDto
import com.careercompass.core.network.dto.TagsRequestDto
import com.careercompass.core.network.dto.UpdateProfileRequestDto
import com.careercompass.core.network.dto.UserProfileDto
import com.careercompass.core.network.model.BaseResponse
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.PUT

/** API_SPEC v0.1 §2 — `/users/me`. */
public interface UserApiService {
    @GET("users/me")
    public suspend fun getMe(): BaseResponse<UserProfileDto>

    @PATCH("users/me")
    public suspend fun updateMe(
        @Body body: UpdateProfileRequestDto,
    ): BaseResponse<UserProfileDto>

    @PUT("users/me/job-interests")
    public suspend fun replaceJobInterests(
        @Body body: JobInterestsRequestDto,
    ): BaseResponse<Unit>

    @PUT("users/me/tags")
    public suspend fun replaceTags(
        @Body body: TagsRequestDto,
    ): BaseResponse<Unit>
}
