package com.careercompass.feature.onboarding.presentation.flow

import com.careercompass.core.domain.error.CoreAuthFailure
import com.careercompass.core.domain.error.CoreDataFailure
import com.careercompass.core.ui.failure.FailureSurface
import com.careercompass.feature.onboarding.presentation.reporting.OnboardingFailureStage
import java.io.IOException

/**
 * 도메인 실패를 온보딩 화면 사유로 좁힌다. 사유를 확인하지 못한 실패는 [OnboardingFailureReason.Unknown].
 *
 * **401 은 null 이다** — 화면에 그릴 사유가 아니라 화면을 떠날 신호라서다(#211). 세션이 끝났으면 온보딩이 할 수
 * 있는 일이 없고, 사용자가 할 수 있는 일(다시 로그인)은 이 그래프 밖에 있다. `TokenAuthenticator`·`TokenReissuer`
 * 가 이 시점엔 로컬 세션까지 이미 정리했으므로 화면이 할 일은 그 사실을 앱 셸에 올리는 것뿐이다.
 *
 * 그래서 이 함수를 직접 부르지 않는다 — null 을 [OnboardingFlowState.sessionEnded] 로 옮기는 자리는
 * [OnboardingViewModel] 의 실패 깔때기 하나다. [stage] 는 상한 초과 문구의 문맥을 정한다.
 */
internal fun Throwable.toOnboardingFailureReason(stage: OnboardingFailureStage): OnboardingFailureReason? =
    when (this) {
        is CoreDataFailure.Unauthorized,
        is CoreAuthFailure.SessionExpired,
        -> {
            null
        }

        // 타임아웃은 연결 없음과 처방이 다르다(#134) — 표도 두 행으로 가른다.
        is CoreDataFailure.NetworkUnavailable -> {
            if (isTimeout) OnboardingFailureReason.Timeout else OnboardingFailureReason.Network
        }

        is CoreAuthFailure.NetworkUnavailable,
        is IOException,
        -> {
            OnboardingFailureReason.Network
        }

        is CoreDataFailure.LimitExceeded -> {
            OnboardingFailureReason.LimitExceeded(stage.limitSurface())
        }

        is CoreDataFailure.InvalidInput -> {
            OnboardingFailureReason.InvalidInput
        }

        // 503 은 서버가 쉬는 것이지 고장이 아니다 — 재시도를 권하는 문구로 접지 않는다(#236).
        is CoreDataFailure.ServiceUnavailable -> {
            OnboardingFailureReason.Maintenance
        }

        is CoreDataFailure.ServerError -> {
            OnboardingFailureReason.Server
        }

        else -> {
            OnboardingFailureReason.Unknown
        }
    }

/**
 * 상한 초과 문구에 실을 문맥 — 단계가 무엇을 담다 막혔는지 안다.
 *
 * 관심 태그·직무처럼 표에 문맥이 없는 상한은 [FailureSurface.Unspecified] 로 개수를 말하지 않는다.
 */
internal fun OnboardingFailureStage.limitSurface(): FailureSurface =
    when (this) {
        OnboardingFailureStage.AddExperience,
        OnboardingFailureStage.UpdateExperience,
        -> FailureSurface.ExperienceCard

        OnboardingFailureStage.UploadPastApplication -> FailureSurface.Application

        else -> FailureSurface.Unspecified
    }
