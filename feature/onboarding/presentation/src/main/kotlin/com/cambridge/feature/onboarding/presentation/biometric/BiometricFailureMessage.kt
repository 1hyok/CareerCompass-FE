package com.cambridge.feature.onboarding.presentation.biometric

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.cambridge.feature.onboarding.presentation.R

/** 지문 로그인 실패 사유를 화면 문구로 바꾼다. */
@Composable
internal fun BiometricFailureReason.toMessage(): String =
    when (this) {
        BiometricFailureReason.Unavailable -> stringResource(R.string.onboarding_biometric_failure_unavailable)
        BiometricFailureReason.Failed -> stringResource(R.string.onboarding_biometric_failure_failed)
        BiometricFailureReason.Lockout -> stringResource(R.string.onboarding_biometric_failure_lockout)
    }
