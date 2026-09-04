package com.careercompass.feature.onboarding.presentation.shared.util

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.careercompass.feature.onboarding.presentation.R
import com.careercompass.feature.onboarding.presentation.shared.model.OnboardingFieldError

/** 필드 공통 검증 문구. 필드 고유 문구가 필요한 화면은 이 함수 대신 자기 리소스를 고른다. */
@Composable
internal fun OnboardingFieldError.toMessage(): String =
    when (this) {
        OnboardingFieldError.Required -> stringResource(R.string.onboarding_field_required)
        is OnboardingFieldError.TooLong -> stringResource(R.string.onboarding_field_too_long, maxLength)
        OnboardingFieldError.InvalidFormat -> stringResource(R.string.onboarding_field_invalid_format)
        OnboardingFieldError.OutOfRange -> stringResource(R.string.onboarding_field_out_of_range)
    }
