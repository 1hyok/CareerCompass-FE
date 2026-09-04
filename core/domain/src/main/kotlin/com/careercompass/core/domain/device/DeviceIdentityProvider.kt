package com.careercompass.core.domain.device

/** 소셜 로그인·지문 등록 요청에 실을 기기 식별자. 앱 설치 단위로 한 번 만들어 유지된다. */
public interface DeviceIdentityProvider {
    public suspend fun deviceId(): String
}
