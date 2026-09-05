package com.careercompass.core.network.service

import com.careercompass.core.network.dto.ForYouFeedDto
import com.careercompass.core.network.model.BaseResponse
import retrofit2.http.GET

/** API_SPEC v0.1 §7 — `/feed/for-you`. */
public interface ForYouApiService {
    /**
     * `GET /feed/for-you` — 톱 픽 + 강점 기반 + 취약점 보완.
     *
     * 프로필과 경험 카드가 모자라 추천을 산출할 수 없으면 422 `PROFILE_INCOMPLETE` 로 답한다.
     */
    @GET("feed/for-you")
    public suspend fun getForYouFeed(): BaseResponse<ForYouFeedDto>
}
