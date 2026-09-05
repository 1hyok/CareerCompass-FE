package com.careercompass.feature.foryou.domain.error

/**
 * For You 도메인이 **요청 전에** 확정하는 실패. 서버가 돌려준 실패는
 * [CoreDataFailure][com.careercompass.core.domain.error.CoreDataFailure] 로 따로 흐른다.
 *
 * 추천·로드맵이 프로필 부족으로 막히는 것은 서버가 판정한다(422 `PROFILE_INCOMPLETE`) — 앱이 미리
 * 흉내 내지 않는다. 어떤 조건이면 산출할 수 있는지의 정본은 서버 한 곳이어야 한다.
 */
public sealed class ForYouFailure(
    message: String,
) : Exception(message) {
    /** 내보낼 구획을 하나도 고르지 않았다 — 빈 문서를 받으려고 서버를 부르지 않는다. */
    public class NoExportSection : ForYouFailure("no export section selected")
}
