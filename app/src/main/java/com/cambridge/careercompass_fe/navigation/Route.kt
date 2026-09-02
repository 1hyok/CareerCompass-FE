package com.cambridge.careercompass_fe.navigation

import kotlinx.serialization.Serializable

/**
 * 앱 셸이 소유하는 최상위 목적지. 각 feature 그래프의 진입 라우트는 그 모듈의 `navigation/` 이 갖고,
 * 여기에는 셸 자체가 그리는 탭 자리표시자만 둔다.
 */
@Serializable
public sealed interface Route {
    /** 메인(피드 탭) 루트 — 피드 그래프가 붙으면 그 홈 라우트로 대체된다. */
    @Serializable
    public data object MainTab : Route

    /** 온보딩 그래프가 붙기 전 인증 계열 시작 목적지의 자리표시자. */
    @Serializable
    public data object AuthPlaceholder : Route

    /** 하단 탭 「분석」(For You·커리어 로드맵·강점 Export) — foryou 모듈이 진입점을 제공할 때까지 자리표시자. */
    @Serializable
    public data object AnalysisTab : Route

    /** 하단 탭 「지원서」(AI 초안·에디터·이력) — editor 모듈이 진입점을 제공할 때까지 자리표시자. */
    @Serializable
    public data object ApplicationsTab : Route

    /** 하단 탭 「마이」(프로필·경험 카드·과거 자소서·알림 설정) — profile 모듈이 진입점을 제공할 때까지 자리표시자. */
    @Serializable
    public data object MyTab : Route
}
