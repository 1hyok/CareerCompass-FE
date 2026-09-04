package com.cambridge.feature.feed.presentation.shared.model

import com.cambridge.core.model.user.UserProfile

/**
 * 적합도 표시가 어느 모양이어야 하는지 — 문구·점수 모델은 각 화면이 붙인다.
 *
 * **「점수를 못 보여 준다」는 사실은 셋이 아니라 둘로만 갈린다**(이슈 #200). 기능 스펙 F2-3 은 산출 불가를
 * 「프로필 미입력」과 「파싱 실패」로 나누지만, 파싱 실패는 서버 계약에 나타낼 자리가 없다 —
 * `GET /postings` 의 `score` 도 `GET /postings/{id}` 의 `parsed`·`suitability` 도 그냥 nullable 이라
 * 「아직 안 끝났다」와 「끝났는데 실패했다」가 **같은 모양(null)으로 온다.**
 * 그래서 파싱 실패는 [Analyzing] 안에 접혀 있다. 필요한 계약 변경과 그때까지의 처분은
 * `docs/spec/suitability-score-boundary.md` 에 적었다.
 */
public enum class SuitabilityJudgement {
    /** 프로필이 비어 산출 자체가 불가 — 사용자가 **할 일이 있다**(프로필 채우기). */
    ProfileIncomplete,

    /** 서버가 아직 점수를 주지 않았다. 영구 실패한 공고도 여기로 접힌다 — 계약이 둘을 가르지 못한다. */
    Analyzing,

    /** 점수가 있다. */
    Ready,
}

/**
 * 적합도 표시 판정(기능 스펙 F2-3 「적합도 점수 표시 조건」·F3-1 「처리 시점」).
 *
 * 1. 서버가 점수를 줬으면([hasScore]) 그대로 보인다 — 프로필이 그 뒤 비었더라도 산출된 값이다.
 * 2. 프로필을 알고 있고 희망 직무·관심 태그가 모두 비어 있으면 「프로필 입력」 안내.
 * 3. 그 밖(파싱 전·파싱 실패·프로필 미확인)은 「분석 중」.
 *
 * 순서가 곧 판정이다 — 점수가 **먼저**다. 프로필이 빈 사용자에게도 서버가 점수를 줬다면 그것은 이미
 * 산출된 사실이므로 「프로필을 채우라」고 말할 근거가 없다.
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
