package com.careercompass.feature.foryou.domain.repository

import com.careercompass.feature.foryou.domain.model.ForYouRecommendations

/** For You 추천 계약 — API_SPEC v0.1 §7 `GET /feed/for-you`. */
public interface ForYouRepository {
    /**
     * 추천 묶음을 읽는다.
     *
     * 프로필·경험 카드가 모자라 산출할 수 없으면 서버가 422 `PROFILE_INCOMPLETE` 로 답하고,
     * 그 실패는 `CoreDataFailure.ProfileIncomplete` 로 접혀 올라온다.
     */
    public suspend fun getRecommendations(): Result<ForYouRecommendations>
}
