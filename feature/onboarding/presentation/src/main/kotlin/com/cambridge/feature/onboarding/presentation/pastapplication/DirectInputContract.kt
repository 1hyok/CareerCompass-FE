package com.cambridge.feature.onboarding.presentation.pastapplication

import androidx.compose.runtime.Immutable
import com.cambridge.feature.onboarding.presentation.shared.model.OnboardingFieldError

/** Step 4 「직접 입력하기」 시트 상태 — 라벨과 본문을 받아 TXT 지원서로 업로드한다(F1-4 「직접 입력」). */
@Immutable
public data class DirectInputState(
    public val label: String = "",
    public val content: String = "",
    public val labelError: OnboardingFieldError? = null,
    public val contentError: OnboardingFieldError? = null,
    public val isSubmitting: Boolean = false,
) {
    public val isInputEnabled: Boolean
        get() = !isSubmitting

    public val isSubmitEnabled: Boolean
        get() = !isSubmitting && label.isNotBlank() && content.isNotBlank()

    public companion object {
        public const val MAX_LABEL_LENGTH: Int = 50
    }
}

/** User intentions emitted by [DirectInputSheet]. */
public sealed interface DirectInputEvent {
    public data class LabelChanged(
        public val value: String,
    ) : DirectInputEvent

    public data class ContentChanged(
        public val value: String,
    ) : DirectInputEvent

    public data object Submitted : DirectInputEvent

    public data object Dismissed : DirectInputEvent
}
