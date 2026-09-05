package com.careercompass.feature.foryou.data

import com.careercompass.core.common.result.runCatchingCancellable
import com.careercompass.core.data.failure.mapDataFailure
import com.careercompass.core.network.model.requireData
import com.careercompass.core.network.service.ForYouApiService
import com.careercompass.feature.foryou.data.mapper.ForYouMapper
import com.careercompass.feature.foryou.domain.model.ForYouRecommendations
import com.careercompass.feature.foryou.domain.repository.ForYouRepository
import javax.inject.Inject

/**
 * `GET /feed/for-you` 구현 — API_SPEC v0.1 §7.
 *
 * 실패 번역은 §9 표 하나를 쓴다(`mapDataFailure`). 그래서 프로필·경험 카드가 모자라 돌아온 422
 * `PROFILE_INCOMPLETE` 가 `CoreDataFailure.ProfileIncomplete` 로 올라가고, 화면은 그 사유에서
 * 「프로필이 아직 비어 있어요 · 프로필 입력하기」를 얻는다(`docs/spec/error-copy.md`).
 */
internal class ForYouRepositoryImpl
    @Inject
    constructor(
        private val forYouApiService: ForYouApiService,
    ) : ForYouRepository {
        override suspend fun getRecommendations(): Result<ForYouRecommendations> =
            runCatchingCancellable {
                ForYouMapper.toRecommendations(forYouApiService.getForYouFeed().requireData())
            }.mapDataFailure()
    }
