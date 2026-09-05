package com.careercompass.feature.profile.domain.usecase

import com.careercompass.core.domain.repository.ExperienceRepository
import javax.inject.Inject

/** 경험 카드를 지운다 — `DELETE /experiences/{id}`. 되돌릴 수 없어 화면이 확인을 받은 뒤 호출한다(F1-3). */
public class DeleteExperienceUseCase
    @Inject
    constructor(
        private val experienceRepository: ExperienceRepository,
    ) {
        public suspend operator fun invoke(id: Long): Result<Unit> = experienceRepository.deleteExperience(id)
    }
