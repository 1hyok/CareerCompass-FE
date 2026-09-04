package com.careercompass.feature.onboarding.presentation.biometric

import androidx.compose.runtime.Immutable

/** Immutable rendering state for the fingerprint quick-login screen. */
@Immutable
public data class BiometricLoginUiState(
    public val userName: String,
    public val accountLabel: String?,
    public val isAuthenticating: Boolean = false,
    public val errorMessage: String? = null,
) {
    init {
        require(userName.isNotBlank()) { "userName must not be blank" }
        require(accountLabel == null || accountLabel.isNotBlank()) {
            "accountLabel must be null or non-blank"
        }
        require(errorMessage == null || errorMessage.isNotBlank()) {
            "errorMessage must be null or non-blank"
        }
    }

    /** The fingerprint action accepts a new attempt only while none is in flight. */
    public val isBiometricEnabled: Boolean
        get() = !isAuthenticating
}

/** User intentions emitted by [BiometricLoginScreen]. */
public sealed interface BiometricLoginEvent {
    public data object BiometricClicked : BiometricLoginEvent

    public data object OtherMethodClicked : BiometricLoginEvent

    public data object ErrorDismissed : BiometricLoginEvent
}
