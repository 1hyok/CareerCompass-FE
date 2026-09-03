package com.cambridge.feature.feed.presentation.shared.model

import com.cambridge.core.model.user.UserProfile

/** 적합도 표시가 어느 모양이어야 하는지 — 문구·점수 모델은 각 화면이 붙인다. */
public enum class SuitabilityJudgement {
    ProfileIncomplete,
    Analyzing,
    Ready,
}

/**
 * 적합도 표시 판정(기능 스펙 F2-3 「적합도 점수 표시 조건」·F3-1 「처리 시점」).
 *
 * 1. 서버가 점수를 줬으면([hasScore]) 그대로 보인다 — 프로필이 그 뒤 비었더라도 산출된 값이다.
 * 2. 프로필을 알고 있고 희망 직무·관심 태그가 모두 비어 있으면 「프로필 입력」 안내.
 * 3. 그 밖(파싱 전·프로필 미확인)은 「분석 중」.
 *
 * 목록 카드와 상세 카드가 이 한 규칙을 공유한다 — 같은 공고를 목록에서는 「분석 중」, 상세에서는
 * 「프로필 입력」으로 다르게 말하지 않게 한다.
 */
public fun judgeSuitability(
    hasScore: Boolean,
    profile: UserProfile?,
): SuitabilityJudgement =
    when {
        hasScore -> SuitabilityJudgement.Ready
        profile.lacksSuitabilityInputs() -> SuitabilityJudgement.ProfileIncomplete
        else -> SuitabilityJudgement.Analyzing
    }

/**
 * 프로필을 알고 있는데 적합도 산출 근거(희망 직무·관심 태그)가 하나도 없다.
 *
 * 아직 프로필을 못 받은 상태(`null`)는 미입력이 아니다 — 모르는 것을 단정하면 서버가 곧 줄 점수를
 * 두고 「프로필을 입력하라」고 조를 수 있다.
 */
public fun UserProfile?.lacksSuitabilityInputs(): Boolean = this != null && jobInterests.isEmpty() && tags.isEmpty()
