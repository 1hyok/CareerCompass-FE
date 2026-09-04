package com.cambridge.feature.onboarding.domain.usecase

import com.careercompass.core.domain.repository.ExperienceRepository
import com.careercompass.core.model.experience.Experience
import com.careercompass.core.model.experience.ExperienceDraft
import javax.inject.Inject

/**
 * Step 3 카드 수정 — 기능 스펙 F1-3 「등록된 카드는 수정 및 삭제 가능」.
 *
 * 유형별 필수 필드 규칙은 [ExperienceDraft] 불변식이 지킨다([AddExperienceUseCase] 와 같다).
 */
public class UpdateExperienceUseCase
    @Inject
    constructor(
        private val experienceRepository: ExperienceRepository,
    ) {
        public suspend operator fun invoke(
            id: Long,
            draft: ExperienceDraft,
        ): Result<Experience> = experienceRepository.updateExperience(id = id, draft = draft)
    }
