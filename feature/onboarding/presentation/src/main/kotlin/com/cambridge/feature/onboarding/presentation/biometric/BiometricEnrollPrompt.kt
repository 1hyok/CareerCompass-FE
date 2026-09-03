package com.cambridge.feature.onboarding.presentation.biometric

import androidx.activity.compose.LocalActivity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.res.stringResource
import androidx.fragment.app.FragmentActivity
import com.cambridge.feature.onboarding.presentation.R

/** 지문 등록 프롬프트의 결과. 실패 사유는 리포팅용 [Failed.cause] 로만 남는다 — 호출부가 할 말은 한 줄뿐이라서다. */
public sealed interface BiometricEnrollPromptResult {
    /** 지문을 확인했다 — 호출부가 이어서 서버 등록(`AuthRepository.registerBiometric`)을 한다. */
    public data object Succeeded : BiometricEnrollPromptResult

    /** 사용자가 프롬프트를 닫았다. 답을 고르지 않은 것이라 표시도 기록도 하지 않는다. */
    public data object Cancelled : BiometricEnrollPromptResult

    /** 지문 확인이 실패·잠금·불가로 끝났다. */
    public data class Failed(
        public val cause: Throwable,
    ) : BiometricEnrollPromptResult
}

/**
 * 지문 **등록**용 생체 프롬프트를 온보딩 밖에서도 열 수 있게 하는 진입점 — 마이 탭의 지문 로그인 스위치(#113)가 쓴다.
 *
 * #98 의 등록 흐름(프롬프트 → `registerBiometric()`) 중 프롬프트 절반이다. 프롬프트 문구·요구 인증 수단
 * ([BIOMETRIC_ENROLL_AUTHENTICATORS])·[FragmentActivity] 요구는 온보딩이 계속 소유한다 — 기준이 갈리면 같은
 * 「지문 등록」이 화면마다 달라진다. 서버 등록까지 여기서 하지 않는 이유는 실패 처리가 화면마다 다르기 때문이다:
 * 제안 시트는 문구를 띄우고([BiometricEnrollViewModel]), 스위치는 원래 자리로 돌아간다.
 *
 * @param onResult 프롬프트가 끝났다. 결과를 받아 호출부가 자기 화면 규칙대로 처리한다.
 * @return 프롬프트를 띄우는 함수. 이 기기·호스트에서 강한 생체 인증을 지금 쓸 수 없으면 null 이고, 호출부는 켜는
 *   길을 막는다(프리뷰·테스트 호스트처럼 [FragmentActivity] 가 아닐 때도 같다).
 */
@Composable
public fun rememberBiometricEnrollPrompt(onResult: (BiometricEnrollPromptResult) -> Unit): (() -> Unit)? {
    val currentOnResult by rememberUpdatedState(onResult)
    val launch =
        rememberBiometricPromptLauncher(
            title = stringResource(R.string.onboarding_biometric_enroll_prompt_title),
            negativeButtonText = stringResource(R.string.onboarding_biometric_prompt_negative),
            allowedAuthenticators = BIOMETRIC_ENROLL_AUTHENTICATORS,
            listener = remember { BiometricEnrollPromptResultListener { currentOnResult(it) } },
        )

    // 판정을 remember 하지 않는 이유 — 화면을 열어 둔 사이 지문이 지워지거나 잠길 수 있다. 판정이 늦어도 프롬프트를
    // 띄우기 직전에 launcher 가 한 번 더 확인하므로, 그사이 못 쓰게 된 기기는 시스템 오류 다이얼로그가 아니라
    // [BiometricEnrollPromptResult.Failed] 로 끝난다.
    val activity = LocalActivity.current as? FragmentActivity
    return launch.takeIf { activity.canEnrollBiometric() }
}

private class BiometricEnrollPromptResultListener(
    private val onResult: (BiometricEnrollPromptResult) -> Unit,
) : BiometricPromptListener {
    /** 프롬프트가 떴다는 것만으로 호출부가 할 일은 없다 — 켜는 쪽은 프롬프트를 **요청한** 시점에 이미 잠겨 있다. */
    override fun onStarted() = Unit

    override fun onSucceeded(): Unit = onResult(BiometricEnrollPromptResult.Succeeded)

    override fun onCancelled(): Unit = onResult(BiometricEnrollPromptResult.Cancelled)

    override fun onFailed(
        reason: BiometricFailureReason,
        cause: Throwable,
    ): Unit = onResult(BiometricEnrollPromptResult.Failed(cause))
}
