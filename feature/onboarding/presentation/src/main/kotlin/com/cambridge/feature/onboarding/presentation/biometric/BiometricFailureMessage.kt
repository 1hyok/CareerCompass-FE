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

/**
 * 지문 등록 제안 실패 사유를 시트 문구로 바꾼다.
 *
 * 로그인 쪽과 달리 「다른 방법으로 로그인」 같은 대안을 권하지 않는다 — 등록에 실패해도 사용자는 이미 로그인한
 * 상태이고, 이 시트는 닫으면 그대로 가던 길로 이어진다.
 */
@Composable
internal fun BiometricEnrollFailureReason.toMessage(): String =
    when (this) {
        BiometricEnrollFailureReason.Authentication -> stringResource(R.string.onboarding_biometric_enroll_failure_authentication)
        BiometricEnrollFailureReason.Registration -> stringResource(R.string.onboarding_biometric_enroll_failure_registration)
    }
