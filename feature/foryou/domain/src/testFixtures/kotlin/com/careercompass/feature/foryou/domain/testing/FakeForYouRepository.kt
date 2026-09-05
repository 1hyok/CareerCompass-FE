package com.careercompass.feature.foryou.domain.testing

import com.careercompass.feature.foryou.domain.model.ForYouRecommendations
import com.careercompass.feature.foryou.domain.repository.ForYouRepository

/**
 * [ForYouRepository] fake 정본 — 정해 둔 결과 하나를 돌려주고 호출 횟수를 센다.
 *
 * [onGetRecommendations] 를 채우면 그 결과가 이긴다(실패 시나리오).
 */
public class FakeForYouRepository(
    private var result: Result<ForYouRecommendations> =
        Result.success(ForYouRecommendations(topPick = null, byStrength = emptyList(), byGap = emptyList())),
    public var onGetRecommendations: (suspend () -> Result<ForYouRecommendations>)? = null,
) : ForYouRepository {
    public var callCount: Int = 0
        private set

    public fun returns(value: Result<ForYouRecommendations>) {
        result = value
    }

    override suspend fun getRecommendations(): Result<ForYouRecommendations> {
        callCount += 1
        onGetRecommendations?.let { return it() }
        return result
    }
}
