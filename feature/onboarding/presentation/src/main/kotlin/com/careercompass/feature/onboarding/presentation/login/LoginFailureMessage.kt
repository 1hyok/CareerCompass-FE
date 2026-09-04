package com.careercompass.feature.onboarding.presentation.login

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.careercompass.feature.onboarding.presentation.R

/** 로그인 실패 사유를 화면 문구로 바꾼다. */
@Composable
internal fun LoginFailureReason.toMessage(): String =
    when (this) {
        LoginFailureReason.Network -> stringResource(R.string.onboarding_login_failure_network)
        LoginFailureReason.Rejected -> stringResource(R.string.onboarding_login_failure_rejected)
        LoginFailureReason.Unknown -> stringResource(R.string.onboarding_login_failure_unknown)
    }
