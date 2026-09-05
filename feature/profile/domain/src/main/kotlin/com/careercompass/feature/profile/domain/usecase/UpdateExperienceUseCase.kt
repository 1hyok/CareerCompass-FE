package com.careercompass.feature.profile.domain.usecase

import com.careercompass.core.domain.repository.ExperienceRepository
import com.careercompass.core.model.experience.Experience
import com.careercompass.core.model.experience.ExperienceDraft
import javax.inject.Inject

/**
 * 경험 카드를 고친다 — `PATCH /experiences/{id}`.
 *
 * 개수가 늘지 않으므로 상한을 보지 않는다([CreateExperienceUseCase] 와 갈리는 점이다). 시점 정밀도를
 * 넓히거나 좁히지 않는 것은 [ExperienceDraft] 불변식이 지킨다 — 연도만 아는 수상 카드를 열었다 저장하는
 * 것만으로 없던 월·일이 생기지 않는다(#166·#171).
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
