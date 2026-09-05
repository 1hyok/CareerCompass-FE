package com.careercompass.feature.profile.domain.usecase

import com.careercompass.core.domain.repository.ExperienceRepository
import com.careercompass.core.model.experience.Experience
import com.careercompass.core.model.experience.ExperienceDraft
import com.careercompass.core.model.experience.MAX_EXPERIENCE_CARDS
import com.careercompass.feature.profile.domain.error.ProfileFailure
import javax.inject.Inject

/**
 * 경험 카드를 등록한다 — `POST /experiences`.
 *
 * 보내기 전에 개수를 세어 상한([MAX_EXPERIENCE_CARDS], F1-3)에 닿았으면 요청 없이
 * [ProfileFailure.ExperienceLimitReached] 로 실패한다. 목록 화면이 이미 진입점을 막지만, 다른 기기에서
 * 채운 상한은 이 화면이 모른다 — 그 경우 서버의 422 `LIMIT_EXCEEDED` 를 기다리면 사용자는 유형별 상세
 * 입력을 다 마친 뒤에야 거절을 본다. 개수 조회 자체가 실패하면 그 실패를 그대로 돌려준다(게시판 등록의
 * `RegisterBoardUseCase` 와 같은 판정 — 같은 원인으로 등록도 실패할 가능성이 크다).
 *
 * 유형별 필수 필드와 시점 정밀도 규칙은 [ExperienceDraft] 불변식이 지킨다.
 */
public class CreateExperienceUseCase
    @Inject
    constructor(
        private val experienceRepository: ExperienceRepository,
    ) {
        public suspend operator fun invoke(draft: ExperienceDraft): Result<Experience> {
            val page =
                experienceRepository
                    .getExperiences(type = null, cursor = null, limit = MAX_EXPERIENCE_CARDS)
                    .getOrElse { return Result.failure(it) }
            if (page.items.size >= MAX_EXPERIENCE_CARDS) {
                return Result.failure(ProfileFailure.ExperienceLimitReached(MAX_EXPERIENCE_CARDS))
            }
            return experienceRepository.createExperience(draft)
        }
    }
