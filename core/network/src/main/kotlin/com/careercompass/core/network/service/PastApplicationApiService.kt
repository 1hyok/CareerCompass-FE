package com.careercompass.core.network.service

import com.careercompass.core.network.dto.PastApplicationDto
import com.careercompass.core.network.dto.PastApplicationItemDto
import com.careercompass.core.network.dto.PastApplicationListDto
import com.careercompass.core.network.dto.UpdateItemCategoryRequestDto
import com.careercompass.core.network.model.BaseResponse
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.Multipart
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Part
import retrofit2.http.Path

/** API_SPEC v0.1 §4 — `/past-applications`. */
public interface PastApplicationApiService {
    /** `POST /past-applications/upload` (multipart/form-data) — `file` 바이너리와 `label` 텍스트 파트. */
    @Multipart
    @POST("past-applications/upload")
    public suspend fun upload(
        @Part file: MultipartBody.Part,
        @Part("label") label: RequestBody,
    ): BaseResponse<PastApplicationDto>

    @GET("past-applications")
    public suspend fun getPastApplications(): BaseResponse<PastApplicationListDto>

    @PATCH("past-applications/{appId}/items/{itemId}")
    public suspend fun updateItemCategory(
        @Path("appId") applicationId: Long,
        @Path("itemId") itemId: Long,
        @Body body: UpdateItemCategoryRequestDto,
    ): BaseResponse<PastApplicationItemDto>

    @DELETE("past-applications/{id}")
    public suspend fun delete(
        @Path("id") id: Long,
    ): BaseResponse<Unit>
}
