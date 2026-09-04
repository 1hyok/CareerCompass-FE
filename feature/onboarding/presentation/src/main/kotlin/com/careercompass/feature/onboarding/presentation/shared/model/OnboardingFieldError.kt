package com.careercompass.feature.onboarding.presentation.shared.model

import androidx.compose.runtime.Immutable

/**
 * 입력 필드 검증 실패 사유. ViewModel 은 이 값만 상태에 두고, 문구는 화면(Entry·시트)이 리소스로 만든다.
 *
 * 필드마다 다른 문구가 필요하면(학점 범위·졸업 연월 형식) 화면이 사유와 필드를 함께 보고 고른다.
 */
@Immutable
public sealed interface OnboardingFieldError {
    /** 필수 값이 비어 있다. */
    public data object Required : OnboardingFieldError

    /** [maxLength] 자를 넘었다. */
    @Immutable
    public data class TooLong(
        val maxLength: Int,
    ) : OnboardingFieldError {
        init {
            require(maxLength > 0) { "maxLength must be positive" }
        }
    }

    /** 형식이 맞지 않는다(숫자·YYYY.MM 등). */
    public data object InvalidFormat : OnboardingFieldError

    /** 형식은 맞지만 허용 범위를 벗어났다. */
    public data object OutOfRange : OnboardingFieldError
}
