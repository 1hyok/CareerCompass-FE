package com.cambridge.feature.onboarding.domain.usecase

import com.cambridge.feature.onboarding.domain.model.OnboardingStep
import com.cambridge.feature.onboarding.domain.repository.OnboardingProgressRepository
import javax.inject.Inject

/**
 * Step 3 를 마치고(경험을 추가했든 건너뛰었든) 진행 상태를 Step 4 로 옮긴다.
 *
 * 경험은 선택 입력(F1-2 Step 3)이라 서버 호출 없이 진행 기록만 바꾼다.
 */
public class ProceedToPastApplicationUseCase
    @Inject
    constructor(
        private val progressRepository: OnboardingProgressRepository,
    ) {
        public suspend operator fun invoke(): Result<Unit> = progressRepository.save(OnboardingStep.PastApplication)
    }
