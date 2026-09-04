package com.careercompass.core.network.service

import com.careercompass.core.network.dto.BoardDetectRequestDto
import com.careercompass.core.network.dto.BoardDetectionDto
import com.careercompass.core.network.model.BaseResponse
import retrofit2.http.Body
import retrofit2.http.POST

/**
 * API_SPEC v0.1 §5 — `POST /boards/detect`.
 *
 * [BoardApiService] 에서 이 호출만 떼어 낸 것은 타임아웃 때문이다. 감지는 서버가 **사용자가 준 외부 사이트**를
 * 크롤링·분석하는 동안 기다리는 호출이라
 * [LongRunningOperation.BoardDetect][com.careercompass.core.network.di.LongRunningOperation.BoardDetect] 로 도는
 * 전용 Retrofit 을 쓴다. 서비스를 나누지 않으면 그 값이 목록·등록까지 함께 늘어난다.
 */
public interface BoardDetectApiService {
    @POST("boards/detect")
    public suspend fun detect(
        @Body body: BoardDetectRequestDto,
    ): BaseResponse<BoardDetectionDto>
}
