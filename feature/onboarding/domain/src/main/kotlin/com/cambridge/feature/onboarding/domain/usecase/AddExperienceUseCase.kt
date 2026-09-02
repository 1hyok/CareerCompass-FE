package com.cambridge.feature.onboarding.domain.usecase

import com.cambridge.core.domain.repository.ExperienceRepository
import com.cambridge.core.model.experience.Experience
import com.cambridge.core.model.experience.ExperienceDraft
import javax.inject.Inject

/** Step 3 「경험 추가」 — 유형별 필수 필드 규칙은 [ExperienceDraft] 불변식이 지킨다. */
public class AddExperienceUseCase
    @Inject
    constructor(
        private val experienceRepository: ExperienceRepository,
    ) {
        public suspend operator fun invoke(draft: ExperienceDraft): Result<Experience> = experienceRepository.createExperience(draft)
    }
