package com.careercompass.feature.onboarding.presentation.navigation

/**
 * 온보딩 로컬 스택이 **스스로 갈 수 없는 곳**과 셸만 내릴 수 있는 판정을 앱 셸에 남긴 것.
 *
 * 그래프 안의 push/pop 은 [OnboardingNavHost] 가 로컬 백스택으로 직접 처리한다. 스택 바닥에서의 back 은 이동이 아니라
 * 경계라 [com.careercompass.core.ui.navigation.FeatureStackBoundary] 가 갖는다.
 */
public interface OnboardingExternalActions {
    /** 인증·온보딩을 끝냈다 — 온보딩을 통째로 비우고 메인(피드)로. 뒤로가기로 인증 화면에 돌아가지 않는다. */
    public fun navigateToMain()

    /** 완료 화면 「게시판 먼저 등록하기」 — 메인으로 가되 게시판 등록 화면을 먼저 연다. */
    public fun navigateToBoardRegister()

    /** 지문 확인 뒤 세션 검증이 만료를 알렸다 — 셸이 로그인 화면에 남길 사유를 받는다(#128). */
    public fun onAuthSessionExpired()

    /** Step 1~4 가 401 을 물었다 — 셸이 시작 목적지를 다시 계산해 로그인 화면으로 보낸다(#211). */
    public fun onSessionEnded()
}
