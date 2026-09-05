package com.careercompass.feature.profile.domain.error

import com.careercompass.core.model.application.MAX_PAST_APPLICATIONS
import com.careercompass.core.model.experience.MAX_EXPERIENCE_CARDS

/**
 * 마이 탭 도메인이 **요청 전에** 확정하는 실패. 서버가 돌려준 실패는
 * [CoreDataFailure][com.careercompass.core.domain.error.CoreDataFailure] 로 따로 흐른다.
 *
 * 상한 두 개를 여기 두는 이유는 서버의 422 `LIMIT_EXCEEDED` 가 늦기 때문이다. 카드 30개가 찬 상태에서
 * 등록을 보내면 사용자는 입력을 다 마친 뒤에야 거절을 본다. 상한 자체는 이미 `core:model` 이 상수로
 * 갖고 있으므로([MAX_EXPERIENCE_CARDS]·[MAX_PAST_APPLICATIONS]) 여기서는 「닿았다」는 사건만 이름 짓는다.
 */
public sealed class ProfileFailure(
    message: String,
) : Exception(message) {
    /** 경험 카드가 이미 상한([limit], 기능 스펙 F1-3 최대 30개)에 닿아 더 등록할 수 없다. */
    public class ExperienceLimitReached(
        public val limit: Int = MAX_EXPERIENCE_CARDS,
    ) : ProfileFailure("experience card limit reached ($limit)")

    /** 과거 지원서가 이미 상한([limit], 기능 스펙 F1-4 최대 10개)에 닿아 더 올릴 수 없다. */
    public class PastApplicationLimitReached(
        public val limit: Int = MAX_PAST_APPLICATIONS,
    ) : ProfileFailure("past application limit reached ($limit)")
}
