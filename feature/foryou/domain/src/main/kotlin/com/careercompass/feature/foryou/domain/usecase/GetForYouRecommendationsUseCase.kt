package com.careercompass.feature.foryou.domain.usecase

import com.careercompass.feature.foryou.domain.model.ForYouRecommendations
import com.careercompass.feature.foryou.domain.model.MAX_FOR_YOU_PER_CATEGORY
import com.careercompass.feature.foryou.domain.repository.ForYouRepository
import javax.inject.Inject

/**
 * For You 추천을 읽는다 — `GET /feed/for-you`.
 *
 * 서버 응답 뒤에 클라이언트 규칙 하나를 얹는다 — 목록 둘을 각각 [MAX_FOR_YOU_PER_CATEGORY] 건으로
 * 자른다(§7 「카테고리별 최대 5건」). 서버가 더 보내도 화면은 다섯 장만 그린다. 상한을 화면에서 자르면
 * 같은 규칙이 화면 수만큼 흩어지고, 「몇 건까지인가」를 물을 자리가 없어진다.
 *
 * 프로필·경험 카드가 모자란 경우는 서버가 판정한다 — 422 `PROFILE_INCOMPLETE` 가
 * `CoreDataFailure.ProfileIncomplete` 로 올라오고, 화면은 그 사유에서 「프로필 입력하기」를 얻는다.
 */
public class GetForYouRecommendationsUseCase
    @Inject
    constructor(
        private val forYouRepository: ForYouRepository,
    ) {
        public suspend operator fun invoke(): Result<ForYouRecommendations> =
            forYouRepository.getRecommendations().map { recommendations ->
                recommendations.copy(
                    byStrength = recommendations.byStrength.take(MAX_FOR_YOU_PER_CATEGORY),
                    byGap = recommendations.byGap.take(MAX_FOR_YOU_PER_CATEGORY),
                )
            }
    }
