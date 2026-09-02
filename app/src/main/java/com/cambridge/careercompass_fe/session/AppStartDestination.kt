package com.cambridge.careercompass_fe.session

/**
 * 앱이 스플래시 뒤 처음 보여 줄 목적지.
 *
 * [MainViewModel] 이 세션·프로필로 확정하고, `AppNavigation` 이 이 값으로 NavHost 의 시작 그래프를 고른다.
 * 확정 전에는 시스템 스플래시를 유지한다(값이 null).
 */
public sealed interface AppStartDestination {
    /** 세션 없음 — 소셜 로그인 화면. */
    public data object Login : AppStartDestination

    /** 세션 있음 + 이 기기에서 지문 로그인을 켬 — 지문 확인 화면. */
    public data object BiometricLogin : AppStartDestination

    /** 세션 있음 + 온보딩 미완료 — 온보딩(중단 단계 재개는 온보딩 그래프가 정한다). */
    public data object Onboarding : AppStartDestination

    /** 세션 있음 + 온보딩 완료 — 메인(피드 탭). */
    public data object Main : AppStartDestination
}
