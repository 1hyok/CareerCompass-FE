package com.careercompass.feature.onboarding.presentation.biometric

import androidx.compose.runtime.Immutable
import com.careercompass.core.ui.mvi.MviIntent
import com.careercompass.core.ui.mvi.ReducerEvent
import com.careercompass.core.ui.mvi.UiState

/** 지문 로그인 실패 사유. 문구는 [BiometricLoginContent] 가 리소스로 만든다. */
public enum class BiometricFailureReason {
    /** 이 기기·호스트에서는 지문 인증을 쓸 수 없다(미등록·하드웨어 없음·FragmentActivity 아님). */
    Unavailable,

    /** 인증이 오류로 끝났다. */
    Failed,

    /** 시도 횟수 초과로 잠겼다. */
    Lockout,
}

/** 지문 확인 뒤 갈 곳 — 세션 검증 결과에 따라 피드·온보딩, 세션이 끝났거나 다른 방법을 골랐으면 로그인. */
public enum class BiometricDestination {
    Feed,
    Onboarding,

    /** 사용자가 「다른 방법으로 로그인」을 골랐다 — 저장된 세션은 그대로다. */
    Login,

    /**
     * 지문은 맞았지만 저장된 세션이 401 로 끝나 있었다.
     *
     * 가는 화면은 [Login] 과 같지만 사용자가 고른 것이 아니라 설명이 필요하다 — 앱 셸이 이 사실을 받아 로그인
     * 화면에 이유를 남긴다(#128). 이 구분을 화면 쪽에서 다시 판정하지 않도록 목적지에 실어 보낸다.
     */
    SessionExpired,
}

/**
 * 지문 빠른 로그인 화면 상태. [failure]·[pendingNavigation] 은 단발 신호다 — 화면이 소비한 뒤
 * [BiometricLoginIntent.ConsumeFailure]·[BiometricLoginIntent.ConsumeNavigation] 으로 비운다.
 *
 * @property userName 프로필의 이름. 없으면 화면이 기본 호칭으로 대신한다.
 * @property isBiometricEnabled 이 계정이 이 기기에 지문을 등록해 뒀는가 — 없으면 프롬프트를 띄워도 열 세션이 없다.
 * @property isAuthenticating 프롬프트가 떠 있는 동안과, 인증 성공 뒤 세션을 검증하는 동안 true — 둘 다 「지문 버튼을
 *   다시 누르면 안 되는」 같은 상태라 화면은 구분하지 않는다.
 */
@Immutable
public data class BiometricLoginUiState(
    val userName: String? = null,
    val isBiometricEnabled: Boolean = false,
    val isAuthenticating: Boolean = false,
    val failure: BiometricFailureReason? = null,
    val pendingNavigation: BiometricDestination? = null,
) : UiState {
    /** 지문 버튼은 진행 중인 시도가 없을 때만 새 시도를 받는다. */
    public val isActionEnabled: Boolean
        get() = !isAuthenticating
}

/** 화면이 [BiometricLoginViewModel] 에 보내는 것. 프롬프트 결과도 여기로 들어온다. */
public sealed interface BiometricLoginIntent : MviIntent {
    /** 프롬프트가 떴다. */
    public data object AuthenticationStarted : BiometricLoginIntent

    /** 지문이 맞았다 — 세션을 검증해 목적지를 정한다. */
    public data object AuthenticationSucceeded : BiometricLoginIntent

    /** 사용자가 프롬프트를 닫았다 — 표시도 기록도 하지 않는다. */
    public data object AuthenticationCancelled : BiometricLoginIntent

    public data class AuthenticationFailed(
        val reason: BiometricFailureReason,
        val cause: Throwable,
    ) : BiometricLoginIntent

    /** 「다른 방법으로 로그인」. */
    public data object ChooseOtherMethod : BiometricLoginIntent

    public data object ConsumeNavigation : BiometricLoginIntent

    public data object ConsumeFailure : BiometricLoginIntent
}

/** 상태가 겪은 것. [BiometricLoginViewModel] 만 만든다. */
public sealed interface BiometricLoginReducerEvent : ReducerEvent {
    /** 저장소가 알려 준 계정 사실 — 지문 등록 여부와 이름. */
    public data class AccountChanged(
        val isBiometricEnabled: Boolean,
        val userName: String?,
    ) : BiometricLoginReducerEvent

    public data object AuthenticationStarted : BiometricLoginReducerEvent

    /** 세션 검증이 끝나 목적지가 정해졌다. */
    public data class SessionResolved(
        val destination: BiometricDestination,
    ) : BiometricLoginReducerEvent

    /** 결과 없이 끝났다(사용자 취소) — 잠금만 푼다. */
    public data object AuthenticationEnded : BiometricLoginReducerEvent

    public data class AuthenticationFailed(
        val reason: BiometricFailureReason,
    ) : BiometricLoginReducerEvent

    public data object OtherMethodChosen : BiometricLoginReducerEvent

    public data object NavigationConsumed : BiometricLoginReducerEvent

    public data object FailureConsumed : BiometricLoginReducerEvent
}
