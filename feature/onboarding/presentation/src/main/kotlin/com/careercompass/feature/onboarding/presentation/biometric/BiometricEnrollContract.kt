package com.careercompass.feature.onboarding.presentation.biometric

import androidx.compose.runtime.Immutable
import com.careercompass.core.ui.mvi.MviIntent
import com.careercompass.core.ui.mvi.ReducerEvent
import com.careercompass.core.ui.mvi.UiState

/** 지문 등록 제안의 실패 사유. 문구는 [BiometricEnrollSheet] 가 리소스로 만든다. */
public enum class BiometricEnrollFailureReason {
    /** 지문 확인이 실패·잠금·불가로 끝났다. */
    Authentication,

    /** 지문은 확인했지만 서버 등록이 실패했다. */
    Registration,
}

/**
 * 지문 등록 제안 상태. [failure]·[canProceed] 는 단발 신호다.
 *
 * @property isOffered 제안 시트가 떠 있는지.
 * @property isRegistering 프롬프트가 떠 있는 동안과 서버 등록을 기다리는 동안 true — 둘 다 「버튼을 다시 누르면 안
 *   되는」 같은 상태라 화면은 구분하지 않는다.
 * @property canProceed 제안이 끝나 원래 이동을 이어서 해도 되는 시점.
 */
@Immutable
public data class BiometricEnrollUiState(
    val isOffered: Boolean = false,
    val isRegistering: Boolean = false,
    val failure: BiometricEnrollFailureReason? = null,
    val canProceed: Boolean = false,
) : UiState {
    /**
     * 등록이 진행 중이면 두 버튼을 함께 잠근다.
     *
     * 「나중에」까지 잠그는 이유 — 이 상태에서는 생체 프롬프트가 화면을 덮고 있거나 서버 응답을 기다리는 중이다.
     * 그때 시트를 닫으면 등록 결과를 받을 화면이 사라지고, 서버에는 등록됐는데 로컬 귀속만 없는 상태가 남는다.
     */
    public val isActionEnabled: Boolean
        get() = !isRegistering
}

/** 관문·시트가 [BiometricEnrollViewModel] 에 보내는 것. 프롬프트 결과도 여기로 들어온다. */
public sealed interface BiometricEnrollIntent : MviIntent {
    /**
     * 제안할지 정한다. 제안하지 않기로 하면 곧바로 통과 신호를 낸다.
     *
     * @param deviceCanEnroll 이 기기·호스트에서 강한 생체 인증을 지금 쓸 수 있는가.
     */
    public data class RequestOffer(
        val deviceCanEnroll: Boolean,
    ) : BiometricEnrollIntent

    /** 프롬프트가 떴다 — 결과가 올 때까지 시트의 버튼을 잠근다. */
    public data object AuthenticationStarted : BiometricEnrollIntent

    /** 지문이 맞았다 — 이제서야 서버에 기기를 등록한다. */
    public data object AuthenticationSucceeded : BiometricEnrollIntent

    /** 사용자가 프롬프트를 닫았다 — 아직 답을 고르는 중이므로 시트를 그대로 둔다. */
    public data object AuthenticationCancelled : BiometricEnrollIntent

    /**
     * 지문 확인이 오류로 끝났다. 프롬프트가 준 [BiometricFailureReason] 은 받지 않는다 — 잠금이든 미지원이든
     * 이 시트가 할 말은 「확인하지 못했다」 하나뿐이라, 사유는 [cause] 로 리포팅에만 남는다.
     */
    public data class AuthenticationFailed(
        val cause: Throwable,
    ) : BiometricEnrollIntent

    /** 「나중에」 — 취소가 아니라 다시 묻지 말라는 답이다. 시트를 스와이프로 닫는 것도 같다. */
    public data object Decline : BiometricEnrollIntent

    public data object ConsumeProceed : BiometricEnrollIntent

    public data object ConsumeFailure : BiometricEnrollIntent
}

/** 상태가 겪은 것. [BiometricEnrollViewModel] 만 만든다. */
public sealed interface BiometricEnrollReducerEvent : ReducerEvent {
    public data object Offered : BiometricEnrollReducerEvent

    /** 제안이 끝났다 — 시트를 닫고 원래 이동을 이어서 하라고 알린다. */
    public data object Proceeded : BiometricEnrollReducerEvent

    public data object RegistrationStarted : BiometricEnrollReducerEvent

    /** 결과 없이 끝났다(프롬프트 취소) — 잠금만 푼다. */
    public data object RegistrationEnded : BiometricEnrollReducerEvent

    public data class RegistrationFailed(
        val reason: BiometricEnrollFailureReason,
    ) : BiometricEnrollReducerEvent

    public data object ProceedConsumed : BiometricEnrollReducerEvent

    public data object FailureConsumed : BiometricEnrollReducerEvent
}
