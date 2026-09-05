package com.careercompass.feature.onboarding.presentation.login

import androidx.compose.runtime.Immutable
import com.careercompass.core.model.auth.SocialProvider
import com.careercompass.core.ui.mvi.MviIntent
import com.careercompass.core.ui.mvi.ReducerEvent
import com.careercompass.core.ui.mvi.UiState

/** 로그인 실패 사유. 문구는 [LoginContent] 가 리소스로 만든다. */
public enum class LoginFailureReason {
    /** 서버에 닿지 못했다. */
    Network,

    /** 서버가 소셜 토큰을 거절했다. */
    Rejected,

    /** 사유를 확인하지 못한 실패. */
    Unknown,
}

/** 로그인 성공 뒤 갈 곳 — 신규 가입이면 온보딩, 아니면 피드(F1-1). */
public enum class LoginDestination {
    Onboarding,
    Feed,
}

/**
 * 소셜 로그인 화면 상태. [failure]·[pendingNavigation] 은 단발 신호다 — 화면이 소비한 뒤
 * [LoginIntent.ConsumeFailure]·[LoginIntent.ConsumeNavigation] 으로 비운다.
 *
 * @property isLoading 소셜 SDK 가 떠 있거나 서버 로그인을 기다리는 동안 true.
 */
@Immutable
public data class LoginUiState(
    val isLoading: Boolean = false,
    val failure: LoginFailureReason? = null,
    val pendingNavigation: LoginDestination? = null,
) : UiState {
    /**
     * 화면이 「진행 중」으로 그려야 하는 시점. 이동이 대기 중인 동안도 포함한다 — 관문이 프로필을 받아 오는 사이 버튼이
     * 살아 있으면 이미 로그인한 사용자가 SDK 를 한 번 더 열 수 있다.
     */
    public val isBusy: Boolean
        get() = isLoading || pendingNavigation != null

    /** 소셜 로그인 버튼은 진행 중인 시도가 없을 때만 눌린다. */
    public val isActionEnabled: Boolean
        get() = !isBusy
}

/** 화면이 [LoginViewModel] 에 보내는 것 — 사용자가 하려는 것. */
public sealed interface LoginIntent : MviIntent {
    /**
     * 소셜 로그인 시도 하나.
     *
     * @param requestToken SDK 에서 토큰을 받아 오는 일. Activity 가 필요한 유일한 조각이라 화면이 만들어 넘긴다.
     */
    public data class RequestSocialLogin(
        val provider: SocialProvider,
        val requestToken: suspend () -> Result<String>,
    ) : LoginIntent

    /** SDK 를 띄운 화면이 사라졌다(설정 변경에 따른 재생성·화면 이탈) — 토큰 단계만 끊는다. */
    public data object DetachLoginHost : LoginIntent

    public data object ConsumeFailure : LoginIntent

    public data object ConsumeNavigation : LoginIntent
}

/** 상태가 겪은 것. [LoginViewModel] 만 만든다. */
public sealed interface LoginReducerEvent : ReducerEvent {
    /** 시도가 시작됐다 — 앞선 실패 표시는 이 시도의 판정이 아니다. */
    public data object AttemptStarted : LoginReducerEvent

    /** 토큰 단계가 결과 없이 끝났다(사용자 취소·호스트 이탈) — 표시 없이 잠금만 푼다. */
    public data object AttemptAbandoned : LoginReducerEvent

    public data class LoggedIn(
        val destination: LoginDestination,
    ) : LoginReducerEvent

    public data class LoginFailed(
        val reason: LoginFailureReason,
    ) : LoginReducerEvent

    public data object FailureConsumed : LoginReducerEvent

    public data object NavigationConsumed : LoginReducerEvent
}
