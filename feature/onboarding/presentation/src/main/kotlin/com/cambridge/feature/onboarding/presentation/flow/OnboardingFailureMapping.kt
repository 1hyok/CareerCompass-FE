package com.cambridge.feature.onboarding.presentation.flow

import com.cambridge.core.domain.error.CoreAuthFailure
import com.cambridge.core.domain.error.CoreDataFailure
import java.io.IOException

/**
 * 도메인 실패를 온보딩 화면 사유로 좁힌다. 사유를 확인하지 못한 실패는 [OnboardingFailureReason.Unknown].
 *
 * **401 은 null 이다** — 화면에 그릴 사유가 아니라 화면을 떠날 신호라서다(#211). 세션이 끝났으면 온보딩이 할 수
 * 있는 일이 없고, 사용자가 할 수 있는 일(다시 로그인)은 이 그래프 밖에 있다. `TokenAuthenticator`·`TokenReissuer`
 * 가 이 시점엔 로컬 세션까지 이미 정리했으므로 화면이 할 일은 그 사실을 앱 셸에 올리는 것뿐이다.
 *
 * 그래서 이 함수를 직접 부르지 않는다 — null 을 [OnboardingFlowState.sessionEnded] 로 옮기는 자리는
 * [OnboardingViewModel] 의 실패 깔때기 하나다.
 */
internal fun Throwable.toOnboardingFailureReason(): OnboardingFailureReason? =
    when (this) {
        is CoreDataFailure.Unauthorized,
        is CoreAuthFailure.SessionExpired,
        -> null

        is CoreDataFailure.NetworkUnavailable,
        is CoreAuthFailure.NetworkUnavailable,
        is IOException,
        -> OnboardingFailureReason.Network

        is CoreDataFailure.LimitExceeded -> OnboardingFailureReason.LimitExceeded

        is CoreDataFailure.InvalidInput -> OnboardingFailureReason.InvalidInput

        is CoreDataFailure.ServiceUnavailable,
        is CoreDataFailure.ServerError,
        -> OnboardingFailureReason.Server

        else -> OnboardingFailureReason.Unknown
    }
