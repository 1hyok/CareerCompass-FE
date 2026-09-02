package com.cambridge.careercompass_fe.session

/**
 * 앱 셸이 NavHost 를 만들 때 쓰는 값.
 *
 * [revision] 이 바뀔 때마다 NavHost 를 새로 만든다 — 목적지가 같아도(로그인으로 시작 → 세션 만료 → 다시 로그인)
 * 이전 백스택은 버려야 하는데, 목적지 값만 키로 쓰면 같은 값이라 아무 일도 일어나지 않는다. 초기값은 프로세스마다
 * 다르다 — 프로세스 재생성 때 저장된 NavController 상태가 새 시작 분기(로그인·지문)를 우회해 복원되지 않는다.
 */
public data class AppShellLaunch(
    val revision: Long,
    val destination: AppStartDestination,
)
