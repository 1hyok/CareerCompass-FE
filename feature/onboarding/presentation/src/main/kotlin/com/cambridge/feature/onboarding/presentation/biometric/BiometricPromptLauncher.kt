package com.cambridge.feature.onboarding.presentation.biometric

import androidx.activity.compose.LocalActivity
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity

/** 지문 로그인이 받아 주는 인증 수단. 등록해 둔 지문이 약한 센서로 잡힌 기기도 로그인은 막지 않는다. */
internal const val BIOMETRIC_LOGIN_AUTHENTICATORS: Int =
    BiometricManager.Authenticators.BIOMETRIC_STRONG or BiometricManager.Authenticators.BIOMETRIC_WEAK

/**
 * 지문 **등록**에 요구하는 인증 수단 — 강한 생체만 받는다.
 *
 * 등록은 이 기기를 계정에 묶는 행위라 로그인보다 기준이 높다. 약한 센서만 있는 기기에는 제안 자체를 하지 않는다.
 */
internal const val BIOMETRIC_ENROLL_AUTHENTICATORS: Int = BiometricManager.Authenticators.BIOMETRIC_STRONG

/**
 * 지문 **등록**을 지금 이 호스트·기기에서 할 수 있는가 — [FragmentActivity] 이고 강한 생체를 쓸 수 있어야 한다.
 *
 * 등록을 제안하는 관문([BiometricEnrollGate])과 앱 셸의 지문 로그인 스위치가 같은 답을 써야 한다. 한쪽만 기준이
 * 달라지면 제안은 하지 않으면서 스위치는 켜지는(또는 그 반대) 기기가 생긴다.
 */
internal fun FragmentActivity?.canEnrollBiometric(): Boolean =
    this != null &&
        BiometricManager.from(this).canAuthenticate(BIOMETRIC_ENROLL_AUTHENTICATORS) == BiometricManager.BIOMETRIC_SUCCESS

/** 생체 프롬프트 결과를 받는 쪽. 지문 로그인과 지문 등록 제안이 각자의 ViewModel 로 구현한다. */
internal interface BiometricPromptListener {
    fun onStarted()

    fun onSucceeded()

    /** 사용자가 프롬프트를 닫았다. 두 화면 모두 표시도 기록도 하지 않는다. */
    fun onCancelled()

    fun onFailed(
        reason: BiometricFailureReason,
        cause: Throwable,
    )
}

/**
 * 생체 프롬프트를 띄우는 함수를 만든다 — 지문 로그인·지문 등록 제안이 같은 호출을 쓴다.
 *
 * `BiometricPrompt` 는 [FragmentActivity] 를 요구한다. 호스트가 그렇지 않으면(프리뷰·테스트 호스트) 프롬프트를
 * 띄우지 않고 [BiometricFailureReason.Unavailable] 로 끝낸다 — 호출부는 그 실패를 자기 화면 규칙대로 처리한다.
 *
 * 띄우기 직전에 [BiometricManager.canAuthenticate] 를 다시 확인한다. 화면을 그린 뒤 지문이 지워졌거나 시도 초과로
 * 잠겼을 수 있고, 그때 프롬프트를 띄우면 시스템 오류 다이얼로그가 대신 뜬다.
 *
 * 상태가 저장된 뒤(`FragmentManager.isStateSaved`)에도 확인한다. 그 창에서 `BiometricPrompt.authenticate()` 는
 * 로그만 남기고 **아무 콜백 없이** 돌아가는데, 진행 표시를 [BiometricPromptListener.onStarted] 로 이미 켠 뒤라면
 * 그 표시를 되돌릴 사람이 없다 — 재생성을 건너온 ViewModel 에 잠금만 남아 버튼이 영영 안 눌린다(#157). 그래서
 * 시작을 알리기 전에 먼저 보고, 못 띄우는 창이면 [BiometricPromptListener.onCancelled] 로 조용히 되돌린다:
 * 사용자가 실패한 것이 아니라 화면이 재생성되는 것뿐이라 안내도 기록도 남기지 않는다(#147 이 소셜 로그인에서 정한 규칙).
 *
 * 반대로 **프롬프트가 이미 떠 있는 동안의 재생성은 결함이 아니다.** 콜백은 액티비티 수명의 `BiometricViewModel` 이
 * 붙들고 있어 컴포지션이 죽어도 살아 있고, [rememberUpdatedState] 로 리스너를 매번 다시 읽으므로 늦게 온 결과도
 * 살아남은 ViewModel 로 간다. 새 컴포지션이 붙으면 새 `BiometricPrompt` 가 콜백을 갈아 끼운다.
 */
@Composable
internal fun rememberBiometricPromptLauncher(
    title: String,
    negativeButtonText: String,
    allowedAuthenticators: Int,
    listener: BiometricPromptListener,
): () -> Unit {
    val activity = LocalActivity.current as? FragmentActivity
    val currentListener by rememberUpdatedState(listener)
    val prompt =
        remember(activity) {
            activity?.let { host ->
                BiometricPrompt(host, ContextCompat.getMainExecutor(host), BiometricResultCallback { currentListener })
            }
        }
    val promptInfo =
        remember(title, negativeButtonText, allowedAuthenticators) {
            BiometricPrompt.PromptInfo
                .Builder()
                .setTitle(title)
                .setNegativeButtonText(negativeButtonText)
                .setAllowedAuthenticators(allowedAuthenticators)
                .build()
        }

    return {
        if (activity == null || prompt == null) {
            currentListener.onFailed(
                BiometricFailureReason.Unavailable,
                IllegalStateException("BiometricPrompt requires a FragmentActivity host"),
            )
        } else if (activity.supportFragmentManager.isStateSaved) {
            // 이 창에서는 프롬프트가 뜨지도, 결과가 오지도 않는다 — 시작을 알리지 않고 잠금만 되돌린다.
            currentListener.onCancelled()
        } else {
            val availability = BiometricManager.from(activity).canAuthenticate(allowedAuthenticators)
            if (availability != BiometricManager.BIOMETRIC_SUCCESS) {
                currentListener.onFailed(
                    BiometricFailureReason.Unavailable,
                    BiometricAuthenticationException(availability, "biometric unavailable: $availability"),
                )
            } else {
                currentListener.onStarted()
                prompt.authenticate(promptInfo)
            }
        }
    }
}

/** 프롬프트 오류 코드를 리포팅에 남기기 위한 타입 — 문구는 버려지고 타입·코드만 남는다. */
internal class BiometricAuthenticationException(
    val errorCode: Int,
    message: String,
) : Exception(message)

/**
 * 프롬프트 오류 코드를 화면이 아는 사유로 옮긴다.
 *
 * [listener] 를 매번 다시 읽는 이유 — 콜백은 `BiometricPrompt` 가 붙잡고 있어 컴포지션보다 오래 산다. 생성 시점의
 * 리스너를 붙들면 재구성 뒤의 상태를 갱신하지 못한다.
 */
private class BiometricResultCallback(
    private val listener: () -> BiometricPromptListener,
) : BiometricPrompt.AuthenticationCallback() {
    override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
        listener().onSucceeded()
    }

    override fun onAuthenticationError(
        errorCode: Int,
        errString: CharSequence,
    ) {
        when (errorCode) {
            BiometricPrompt.ERROR_USER_CANCELED,
            BiometricPrompt.ERROR_NEGATIVE_BUTTON,
            BiometricPrompt.ERROR_CANCELED,
            -> listener().onCancelled()

            BiometricPrompt.ERROR_LOCKOUT,
            BiometricPrompt.ERROR_LOCKOUT_PERMANENT,
            -> listener().onFailed(BiometricFailureReason.Lockout, exception(errorCode))

            BiometricPrompt.ERROR_HW_UNAVAILABLE,
            BiometricPrompt.ERROR_HW_NOT_PRESENT,
            BiometricPrompt.ERROR_NO_BIOMETRICS,
            BiometricPrompt.ERROR_NO_DEVICE_CREDENTIAL,
            BiometricPrompt.ERROR_SECURITY_UPDATE_REQUIRED,
            -> listener().onFailed(BiometricFailureReason.Unavailable, exception(errorCode))

            else -> listener().onFailed(BiometricFailureReason.Failed, exception(errorCode))
        }
    }

    /** 단일 시도 실패 — 프롬프트가 계속 떠 있으므로 화면 상태는 바꾸지 않는다. */
    override fun onAuthenticationFailed() = Unit

    private fun exception(errorCode: Int) = BiometricAuthenticationException(errorCode, "biometric prompt error: $errorCode")
}
