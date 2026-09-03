package com.cambridge.feature.onboarding.presentation.biometric

import androidx.compose.runtime.Immutable

/** Immutable rendering state for the fingerprint enrollment offer sheet. */
@Immutable
public data class BiometricEnrollUiState(
    public val isRegistering: Boolean = false,
    public val errorMessage: String? = null,
) {
    init {
        require(errorMessage == null || errorMessage.isNotBlank()) {
            "errorMessage must be null or non-blank"
        }
    }

    /**
     * 등록이 진행 중이면 두 버튼을 함께 잠근다.
     *
     * 「나중에」까지 잠그는 이유 — 이 상태에서는 생체 프롬프트가 화면을 덮고 있거나 서버 응답을 기다리는 중이다.
     * 그때 시트를 닫으면 등록 결과를 받을 화면이 사라지고, 서버에는 등록됐는데 로컬 귀속만 없는 상태가 남는다.
     */
    public val isActionEnabled: Boolean
        get() = !isRegistering
}

/** User intentions emitted by [BiometricEnrollSheet]. */
public sealed interface BiometricEnrollEvent {
    public data object EnrollClicked : BiometricEnrollEvent

    public data object LaterClicked : BiometricEnrollEvent

    public data object ErrorDismissed : BiometricEnrollEvent
}
