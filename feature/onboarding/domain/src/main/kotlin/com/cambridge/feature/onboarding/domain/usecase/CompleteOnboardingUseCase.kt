package com.cambridge.feature.onboarding.domain.usecase

import com.cambridge.core.domain.repository.UserProfileRepository
import com.cambridge.feature.onboarding.domain.repository.OnboardingProgressRepository
import javax.inject.Inject

/**
 * Step 4 완료(건너뛰기 포함) — 로컬 진행 상태를 완료로 바꾸고 프로필을 다시 읽는다.
 *
 * 프로필 갱신은 best-effort 다: 서버 `onboardingDone` 을 가능한 한 빨리 캐시에 반영하려는 것이지 완료의
 * 조건이 아니다. 실패해도 완료는 성립한다 — 다음 진입 판정([ResolveOnboardingEntryUseCase])이 로컬 완료 기록을
 * 존중한다.
 */
public class CompleteOnboardingUseCase
    @Inject
    constructor(
        private val progressRepository: OnboardingProgressRepository,
        private val userProfileRepository: UserProfileRepository,
    ) {
        public suspend operator fun invoke(): Result<Unit> {
            progressRepository.markCompleted().getOrElse { return Result.failure(it) }
            userProfileRepository.refreshProfile()
            return Result.success(Unit)
        }
    }
