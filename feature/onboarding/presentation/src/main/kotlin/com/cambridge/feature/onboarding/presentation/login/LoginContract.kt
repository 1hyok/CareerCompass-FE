package com.cambridge.feature.onboarding.presentation.login

import androidx.compose.runtime.Immutable

/** Immutable rendering state for the social login screen. */
@Immutable
public data class LoginUiState(
    public val isLoading: Boolean = false,
    public val errorMessage: String? = null,
) {
    init {
        require(errorMessage == null || errorMessage.isNotBlank()) {
            "errorMessage must be null or non-blank"
        }
    }

    /** Social login buttons accept taps only while no sign-in attempt is in flight. */
    public val isActionEnabled: Boolean
        get() = !isLoading
}

/** User intentions emitted by [LoginScreen]. */
public sealed interface LoginEvent {
    public data object KakaoLoginClicked : LoginEvent

    public data object GoogleLoginClicked : LoginEvent

    public data object ErrorDismissed : LoginEvent
}
