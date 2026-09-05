package com.careercompass.core.network.service

import com.careercompass.core.network.dto.RoadmapComparisonDto
import com.careercompass.core.network.model.BaseResponse
import retrofit2.http.GET
import retrofit2.http.Query

/** API_SPEC v0.1 §7 — `/roadmap/compare`. */
public interface RoadmapApiService {
    /**
     * `GET /roadmap/compare` — `cohort` 는 `peer`·`senior`·`me_only`.
     *
     * 추천과 같은 근거(프로필·경험 카드)를 쓰므로 모자라면 422 `PROFILE_INCOMPLETE` 로 답한다.
     */
    @GET("roadmap/compare")
    public suspend fun compareRoadmap(
        @Query("cohort") cohort: String,
    ): BaseResponse<RoadmapComparisonDto>
}
