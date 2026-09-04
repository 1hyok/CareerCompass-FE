package com.careercompass.feature.onboarding.domain.usecase

import com.careercompass.core.domain.repository.ExperienceRepository
import javax.inject.Inject

/** Step 3 카드 삭제 — 되돌릴 수 없어 화면이 확인 다이얼로그를 거친 뒤 호출한다(F1-3). */
public class DeleteExperienceUseCase
    @Inject
    constructor(
        private val experienceRepository: ExperienceRepository,
    ) {
        public suspend operator fun invoke(id: Long): Result<Unit> = experienceRepository.deleteExperience(id)
    }
