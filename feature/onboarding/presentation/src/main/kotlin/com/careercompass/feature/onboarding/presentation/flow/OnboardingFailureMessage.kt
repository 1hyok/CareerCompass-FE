package com.careercompass.feature.onboarding.presentation.flow

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.careercompass.feature.onboarding.presentation.R

/** 흐름 실패 사유의 배너 문구. */
@Composable
internal fun OnboardingFailureReason.toMessage(): String =
    when (this) {
        OnboardingFailureReason.Network -> stringResource(R.string.onboarding_failure_network)
        OnboardingFailureReason.LimitExceeded -> stringResource(R.string.onboarding_failure_limit_exceeded)
        OnboardingFailureReason.InvalidInput -> stringResource(R.string.onboarding_failure_invalid_input)
        OnboardingFailureReason.Server -> stringResource(R.string.onboarding_failure_server)
        OnboardingFailureReason.UnsupportedFile -> stringResource(R.string.onboarding_failure_unsupported_file)
        OnboardingFailureReason.FileTooLarge -> stringResource(R.string.onboarding_failure_file_too_large)
        OnboardingFailureReason.Unknown -> stringResource(R.string.onboarding_failure_unknown)
    }

/** 문서 카드 상태 줄에 들어가는 짧은 문구 — 「%s · 재시도」 형식에 맞춘다. */
@Composable
internal fun OnboardingFailureReason.toShortMessage(): String =
    when (this) {
        OnboardingFailureReason.Network -> stringResource(R.string.onboarding_upload_failed_network)

        OnboardingFailureReason.LimitExceeded -> stringResource(R.string.onboarding_upload_failed_limit)

        OnboardingFailureReason.InvalidInput,
        OnboardingFailureReason.UnsupportedFile,
        OnboardingFailureReason.FileTooLarge,
        -> stringResource(R.string.onboarding_upload_failed_invalid)

        OnboardingFailureReason.Server -> stringResource(R.string.onboarding_upload_failed_server)

        OnboardingFailureReason.Unknown -> stringResource(R.string.onboarding_upload_failed_unknown)
    }
