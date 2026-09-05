package com.careercompass.feature.foryou.domain.model

/**
 * 추천을 카테고리별로 몇 건까지 보여 주는가 — API_SPEC v0.1 §7 「카테고리별 최대 5건」.
 *
 * 서버가 더 보내도 이 값이 이긴다. 상한을 화면마다 다시 정하면 같은 규칙이 여러 자리에서 갈린다.
 */
public const val MAX_FOR_YOU_PER_CATEGORY: Int = 5

/**
 * For You 추천 묶음 — API_SPEC v0.1 §7 `GET /feed/for-you`.
 *
 * 세 갈래는 뜻이 다르다. [topPick] 은 단 하나이고 없을 수 있다. [byStrength] 는 내 강점에 맞는 공고,
 * [byGap] 은 모자란 축을 메우는 공고다. 두 목록은 각각 [MAX_FOR_YOU_PER_CATEGORY] 건까지다.
 */
public data class ForYouRecommendations(
    public val topPick: ForYouRecommendation?,
    public val byStrength: List<ForYouRecommendation>,
    public val byGap: List<ForYouRecommendation>,
) {
    /** 세 갈래를 통틀어 추천이 하나도 없는가 — 화면이 「추천할 공고가 없어요」를 그릴 자리다. */
    public val isEmpty: Boolean
        get() = topPick == null && byStrength.isEmpty() && byGap.isEmpty()
}

/**
 * 추천 한 건 — 공고 식별자와 추천 이유.
 *
 * 서버는 이유를 톱 픽에서는 배열로, 나머지 둘에서는 문자열 하나로 준다(§7). 화면이 그 차이를 알 이유가
 * 없으므로 도메인에서는 [reasons] 하나로 모은다 — 문자열 하나는 원소 하나짜리 목록이다.
 *
 * 공고 제목·마감일은 여기 없다. §7 응답이 `postingId` 만 주므로 §5 `GET /postings/{id}` 로 따로 읽는다.
 */
public data class ForYouRecommendation(
    public val postingId: Long,
    public val reasons: List<String>,
)
