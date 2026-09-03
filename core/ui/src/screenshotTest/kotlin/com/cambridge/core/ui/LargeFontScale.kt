package com.cambridge.core.ui

/**
 * 큰 글꼴 골든이 쓰는 배율.
 *
 * 2.0 을 고른 근거는 플랫폼 상한이다. Android 14(API 34) 가 글꼴 크기 설정의 상한을 200% 로
 * 올렸고(그 전 AOSP 의 `config_fontScaleValues` 상한은 1.30), 그 위는 없다. 상한 하나를 지키면
 * 1.15·1.30 같은 중간 단계는 같은 레이아웃의 덜 심한 경우라 따로 골든을 두지 않는다.
 *
 * Android 14 의 실제 배율은 비선형이라 큰 글자는 2.0 보다 덜 커지는데, Compose preview 의
 * `fontScale` 은 선형으로 곱한다 — 즉 이 골든이 단말보다 엄격하다. 여기서 안 깨지면 단말에서도
 * 안 깨진다.
 *
 * 새 화면에 큰 글꼴 골든을 붙이는 기준과 뺀 화면의 근거는 `docs/testing/screenshot.md` 에 있다.
 */
internal const val LARGE_FONT_SCALE: Float = 2.0f
