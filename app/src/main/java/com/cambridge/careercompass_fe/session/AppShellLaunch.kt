package com.cambridge.careercompass_fe.session

/**
 * 앱 셸이 NavHost 를 만들 때 쓰는 값.
 *
 * [revision] 이 바뀔 때마다 NavHost 를 새로 만든다 — 목적지가 같아도(로그인으로 시작 → 세션 만료 → 다시 로그인)
 * 이전 백스택은 버려야 하는데, 목적지 값만 키로 쓰면 같은 값이라 아무 일도 일어나지 않는다. 초기값은 프로세스마다
 * 다르다 — 프로세스 재생성 때 저장된 NavController 상태가 새 시작 분기(로그인·지문)를 우회해 복원되지 않는다.
 *
 * @property sessionExpiryNotice 이 셸 세대의 로그인 화면에서 「로그인이 만료됐다」를 알릴지. 목적지가 아니라 안내라
 *   [revision] 과 따로 꺼진다 — 닫기를 누르거나 다시 로그인을 시도하면 [MainViewModel.consumeSessionExpiryNotice]
 *   가 NavHost 를 그대로 둔 채 끈다. 프로세스가 새로 뜨면 사유를 모르는 첫 계산이라 저절로 꺼져 있다.
 */
public data class AppShellLaunch(
    val revision: Long,
    val destination: AppStartDestination,
    val sessionExpiryNotice: Boolean,
)
