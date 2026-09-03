package androidx.biometric

import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.ViewModelProvider

/** 테스트 전용 — `BiometricPrompt` 가 액티비티에 등록해 둔 클라이언트 콜백을 꺼낸다. */
internal fun clientCallbackOf(activity: FragmentActivity): BiometricPrompt.AuthenticationCallback =
    ViewModelProvider(activity).get(BiometricViewModel::class.java).clientCallback
