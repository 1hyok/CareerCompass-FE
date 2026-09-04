package com.cambridge.feature.onboarding.domain.usecase

import com.careercompass.core.domain.repository.PastApplicationRepository
import com.careercompass.core.model.application.PastApplication
import javax.inject.Inject

/** Step 4 목록 초기값 — 재진입 시 이미 올린 지원서를 다시 보여준다. */
public class GetOnboardingPastApplicationsUseCase
    @Inject
    constructor(
        private val pastApplicationRepository: PastApplicationRepository,
    ) {
        public suspend operator fun invoke(): Result<List<PastApplication>> = pastApplicationRepository.getPastApplications()
    }
