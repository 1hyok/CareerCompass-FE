package com.cambridge.feature.onboarding.domain.usecase

import com.careercompass.core.domain.repository.ExperienceRepository
import com.careercompass.core.model.experience.Experience
import com.careercompass.core.model.experience.MAX_EXPERIENCE_CARDS
import javax.inject.Inject

/**
 * Step 3 목록에 보여줄 경험 카드 전부를 가져온다.
 *
 * 카드는 최대 30개(F1-3)라 첫 페이지 하나로 끝난다 — 온보딩 화면은 커서 페이징을 하지 않는다.
 */
public class GetOnboardingExperiencesUseCase
    @Inject
    constructor(
        private val experienceRepository: ExperienceRepository,
    ) {
        public suspend operator fun invoke(): Result<List<Experience>> =
            experienceRepository.getExperiences(type = null, cursor = null, limit = MAX_EXPERIENCE_CARDS).map { it.items }
    }
