package androidx.biometric

import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.ViewModelProvider

/**
 * `BiometricPrompt` 가 호스트 액티비티에 등록해 둔 클라이언트 콜백을 꺼낸다 — 테스트 전용.
 *
 * 이 파일이 `androidx.biometric` 패키지에 있는 이유는 그 콜백을 붙들고 있는 `BiometricViewModel` 의 접근자가
 * 라이브러리 패키지 안으로 닫혀 있기 때문이다. 지문 프롬프트의 재생성 회귀 가드(`BiometricPromptLauncherRecreateTest`)
 * 는 「프롬프트 결과가 늦게 도착했을 때 누가 받는가」를 물어야 하는데, 실제 생체 센서를 띄울 수 없는 단위 테스트에서
 * 그 물음에 답하려면 등록된 콜백을 직접 불러 보는 길밖에 없다.
 *
 * 프로덕션 코드는 이 경로를 쓰지 않는다. 라이브러리를 올려 접근자가 사라지면 이 파일만 고치면 된다.
 */
internal fun biometricClientCallbackOf(activity: FragmentActivity): BiometricPrompt.AuthenticationCallback =
    ViewModelProvider(activity).get(BiometricViewModel::class.java).clientCallback
