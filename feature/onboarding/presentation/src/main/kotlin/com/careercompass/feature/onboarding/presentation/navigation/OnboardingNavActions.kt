package com.careercompass.feature.onboarding.presentation.navigation

import com.careercompass.feature.onboarding.domain.model.OnboardingStep
import com.careercompass.feature.onboarding.presentation.flow.OnboardingDestination

/**
 * 온보딩 그래프가 앱 셸에 요청하는 이동. 그래프 밖 목적지(피드·게시판 등록)는 이 모듈이 모르므로 셸이 채운다.
 *
 * @property replaceLoginWithOnboarding 신규 가입 — 로그인 화면을 걷어내고 Step 1 로.
 * @property replaceAuthWithFeed 기존 사용자 로그인·지문 인증 성공 — 인증 화면을 걷어내고 피드로.
 * @property replaceAuthWithOnboarding 지문 인증 뒤 세션 검증에서 온보딩 미완료로 확인 — 지문 화면을 걷어내고 Step 1 로.
 * @property navigateToLoginFromBiometric 지문 화면의 「다른 방법으로 로그인」 — 사용자가 고른 것이라 설명이 없다.
 * @property navigateToLoginAfterSessionExpiry 지문 확인 뒤 세션 검증이 만료를 알렸다 — 같은 로그인 화면으로 가되
 *   셸이 사유를 알아야 「왜 로그아웃됐는지」를 그 화면에서 말할 수 있다(#128).
 * @property onSessionEnded Step 1~4 에서 401 을 물었다 — 세션이 끝났다는 **사실만** 올린다(#211). 지문 경로와
 *   달리 그래프가 스스로 옮기지 않는다: 어디로 갈지는 시작 목적지를 다시 계산해야 알 수 있고, 그 계산은 셸의
 *   일이다. 피드·게시판의 `FeedNavActions.onSessionEnded` 와 같은 길이다.
 * @property proceedToStep 단계 저장 성공 또는 재개 지점으로 전진.
 * @property proceedToComplete Step 4 완료·건너뛰기 뒤 완료 화면으로.
 * @property popBack 단계 화면의 뒤로 가기. Step 1 에서 부르면 그래프가 비므로 셸이 종료를 결정한다.
 * @property replaceOnboardingWithFeed 완료 화면 「공고 보러 가기」 및 이미 완료된 사용자의 재진입.
 * @property replaceOnboardingWithBoardRegister 완료 화면 「게시판 먼저 등록하기」.
 */
public class OnboardingNavActions(
    public val replaceLoginWithOnboarding: () -> Unit,
    public val replaceAuthWithFeed: () -> Unit,
    public val replaceAuthWithOnboarding: () -> Unit,
    public val navigateToLoginFromBiometric: () -> Unit,
    public val navigateToLoginAfterSessionExpiry: () -> Unit,
    public val onSessionEnded: () -> Unit,
    public val proceedToStep: (OnboardingStep) -> Unit,
    public val proceedToComplete: () -> Unit,
    public val popBack: () -> Unit,
    public val replaceOnboardingWithFeed: () -> Unit,
    public val replaceOnboardingWithBoardRegister: () -> Unit,
) {
    /** ViewModel 의 이동 신호를 셸 액션으로 옮긴다. */
    public fun navigate(destination: OnboardingDestination) {
        when (destination) {
            is OnboardingDestination.Step -> proceedToStep(destination.step)
            OnboardingDestination.Complete -> proceedToComplete()
            OnboardingDestination.Feed -> replaceOnboardingWithFeed()
            OnboardingDestination.BoardRegister -> replaceOnboardingWithBoardRegister()
        }
    }
}
