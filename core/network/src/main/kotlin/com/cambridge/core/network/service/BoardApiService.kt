package com.cambridge.core.network.service

import com.cambridge.core.network.dto.BoardDto
import com.cambridge.core.network.dto.BoardListDto
import com.cambridge.core.network.dto.BoardRegisterRequestDto
import com.cambridge.core.network.dto.BoardUpdateRequestDto
import com.cambridge.core.network.model.BaseResponse
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Path

/** API_SPEC v0.1 §5 — `/boards`. 구조 감지만 타임아웃이 달라 [BoardDetectApiService] 로 나가 있다. */
public interface BoardApiService {
    @POST("boards")
    public suspend fun register(
        @Body body: BoardRegisterRequestDto,
    ): BaseResponse<BoardDto>

    @GET("boards")
    public suspend fun getBoards(): BaseResponse<BoardListDto>

    @PATCH("boards/{id}")
    public suspend fun update(
        @Path("id") id: Long,
        @Body body: BoardUpdateRequestDto,
    ): BaseResponse<BoardDto>

    @DELETE("boards/{id}")
    public suspend fun delete(
        @Path("id") id: Long,
    ): BaseResponse<Unit>

    @POST("boards/{id}/retry")
    public suspend fun retry(
        @Path("id") id: Long,
    ): BaseResponse<Unit>
}
