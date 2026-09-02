package com.cambridge.feature.onboarding.presentation.flow

import com.cambridge.core.domain.error.CoreAuthFailure
import com.cambridge.core.domain.error.CoreDataFailure
import java.io.IOException

/** 도메인 실패를 온보딩 화면 사유로 좁힌다. 사유를 확인하지 못한 실패는 [OnboardingFailureReason.Unknown]. */
internal fun Throwable.toOnboardingFailureReason(): OnboardingFailureReason =
    when (this) {
        is CoreDataFailure.NetworkUnavailable,
        is CoreAuthFailure.NetworkUnavailable,
        is IOException,
        -> OnboardingFailureReason.Network

        is CoreDataFailure.Unauthorized,
        is CoreAuthFailure.SessionExpired,
        -> OnboardingFailureReason.SessionExpired

        is CoreDataFailure.LimitExceeded -> OnboardingFailureReason.LimitExceeded

        is CoreDataFailure.InvalidInput -> OnboardingFailureReason.InvalidInput

        is CoreDataFailure.ServiceUnavailable,
        is CoreDataFailure.ServerError,
        -> OnboardingFailureReason.Server

        else -> OnboardingFailureReason.Unknown
    }
