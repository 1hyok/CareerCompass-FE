package com.careercompass.feature.foryou.data.mapper

import com.careercompass.core.network.dto.ForYouFeedDto
import com.careercompass.core.network.dto.ForYouPickDto
import com.careercompass.core.network.dto.ForYouTopPickDto
import com.careercompass.feature.foryou.domain.model.ForYouRecommendation
import com.careercompass.feature.foryou.domain.model.ForYouRecommendations

/**
 * For You wire → 도메인 변환.
 *
 * **여기가 `reason` 두 스키마를 하나로 모으는 자리다.** 톱 픽은 배열, 나머지 둘은 문자열 하나로 오는데
 * (API_SPEC §7), 화면이 그 차이를 알 이유가 없다. 문자열 하나는 원소 하나짜리 목록이 된다.
 *
 * 값 확장에는 관대하다 — 빈 이유 문자열은 버리고, 이유가 하나도 없는 추천은 그대로 남긴다(공고를 감출
 * 이유가 없다). 키 누락은 DTO 파싱 단계에서 이미 실패한다.
 */
internal object ForYouMapper {
    fun toRecommendations(dto: ForYouFeedDto): ForYouRecommendations =
        ForYouRecommendations(
            topPick = dto.topPick?.let(::toRecommendation),
            byStrength = dto.byStrength.map(::toRecommendation),
            byGap = dto.byGap.map(::toRecommendation),
        )

    /** 배열 이유 — 빈 문자열만 걸러 순서대로 싣는다. */
    private fun toRecommendation(dto: ForYouTopPickDto): ForYouRecommendation =
        ForYouRecommendation(
            postingId = dto.postingId,
            reasons = dto.reason.map(String::trim).filter(String::isNotEmpty),
        )

    /** 문자열 이유 — 한 줄짜리 목록으로 접는다. */
    private fun toRecommendation(dto: ForYouPickDto): ForYouRecommendation =
        ForYouRecommendation(
            postingId = dto.postingId,
            reasons = listOfNotNull(dto.reason.trim().takeIf(String::isNotEmpty)),
        )
}
