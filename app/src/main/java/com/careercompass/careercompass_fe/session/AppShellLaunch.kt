package com.careercompass.careercompass_fe.session

/**
 * 앱 셸이 루트 백스택을 세울 때 쓰는 값.
 *
 * [revision] 이 바뀔 때마다 루트 백스택을 새로 세운다 — 목적지가 같아도(로그인으로 시작 → 세션 만료 → 다시 로그인)
 * 이전 백스택은 버려야 하는데, 목적지 값만 키로 쓰면 같은 값이라 아무 일도 일어나지 않는다.
 *
 * 프로세스 재생성에서는 **인증이 다시 필요할 때만** 올린다([AppStartDestination.requiresAuthentication]) — 그때만
 * 복원된 백스택이 로그인·지문 게이트를 우회한다. 세션이 그대로면 값을 잇는다: 셸 세대의 저장 상태에는 루트·로컬
 * 백스택뿐 아니라 entry 에 매달린 `SavedStateHandle` 이 함께 들어 있어, 값을 바꾸면 온보딩 입력 초안까지 버려진다(#133).
 *
 * @property sessionExpiryNotice 이 셸 세대의 로그인 화면에서 「로그인이 만료됐다」를 알릴지. 목적지가 아니라 안내라
 *   [revision] 과 따로 꺼진다 — 닫기를 누르거나 다시 로그인을 시도하면 [MainViewModel.consumeSessionExpiryNotice]
 *   가 루트 백스택을 그대로 둔 채 끈다. 프로세스가 새로 뜨면 사유를 모르는 첫 계산이라 저절로 꺼져 있다.
 */
public data class AppShellLaunch(
    val revision: Long,
    val destination: AppStartDestination,
    val sessionExpiryNotice: Boolean,
)
