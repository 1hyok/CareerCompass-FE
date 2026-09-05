package com.careercompass.feature.profile.domain.usecase

import com.careercompass.core.domain.repository.ExperienceRepository
import com.careercompass.core.model.experience.MAX_EXPERIENCE_CARDS
import javax.inject.Inject

/**
 * 등록된 경험 카드가 몇 장인지 센다 — 목록 화면의 「현재 개수/상한」과 등록 진입점 차단의 근거다(F1-3).
 *
 * 유형 필터와 무관하게 **전체**를 센다. 필터를 건 목록의 길이로 세면 프로젝트만 보고 있는 사용자에게
 * 「3/30」이 보이고, 상한에 닿았는데도 등록 버튼이 열린다.
 *
 * 카드는 최대 [MAX_EXPERIENCE_CARDS] 장이라 한 페이지로 끝난다 — 커서를 따라가지 않는다. 서버가
 * 개수만 주는 엔드포인트를 갖게 되면 이 한 곳만 갈아 끼운다.
 */
public class CountExperiencesUseCase
    @Inject
    constructor(
        private val experienceRepository: ExperienceRepository,
    ) {
        public suspend operator fun invoke(): Result<Int> =
            experienceRepository
                .getExperiences(type = null, cursor = null, limit = MAX_EXPERIENCE_CARDS)
                .map { it.items.size }
    }
