package com.careercompass.feature.onboarding.domain.usecase

import com.careercompass.core.domain.repository.ExperienceRepository
import com.careercompass.core.model.experience.Experience
import com.careercompass.core.model.experience.ExperienceDraft
import javax.inject.Inject

/** Step 3 「경험 추가」 — 유형별 필수 필드 규칙은 [ExperienceDraft] 불변식이 지킨다. */
public class AddExperienceUseCase
    @Inject
    constructor(
        private val experienceRepository: ExperienceRepository,
    ) {
        public suspend operator fun invoke(draft: ExperienceDraft): Result<Experience> = experienceRepository.createExperience(draft)
    }
