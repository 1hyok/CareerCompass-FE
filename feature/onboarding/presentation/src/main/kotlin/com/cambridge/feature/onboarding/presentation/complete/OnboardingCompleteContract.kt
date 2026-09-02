package com.cambridge.feature.onboarding.presentation.complete

import androidx.compose.runtime.Immutable

/** Immutable rendering state for the onboarding completion screen. */
@Immutable
public data class OnboardingCompleteUiState(
    public val userName: String? = null,
) {
    init {
        require(userName == null || userName.isNotBlank()) {
            "userName must be null or non-blank"
        }
    }
}

/** User intentions emitted by [OnboardingCompleteScreen]. */
public sealed interface OnboardingCompleteEvent {
    public data object ViewFeedClicked : OnboardingCompleteEvent

    public data object RegisterBoardClicked : OnboardingCompleteEvent
}
