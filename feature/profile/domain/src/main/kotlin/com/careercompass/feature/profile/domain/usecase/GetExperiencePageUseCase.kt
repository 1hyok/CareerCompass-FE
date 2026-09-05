package com.careercompass.feature.profile.domain.usecase

import com.careercompass.core.domain.repository.ExperienceRepository
import com.careercompass.core.model.experience.Experience
import com.careercompass.core.model.experience.ExperienceType
import com.careercompass.core.model.paging.CursorPage
import javax.inject.Inject

/**
 * 경험 카드 한 페이지를 조회한다 — `GET /experiences?type=&cursor=&limit=` (최신 등록순).
 *
 * 온보딩 Step 3 은 30개를 한 번에 받아 끝내지만(`GetOnboardingExperiencesUseCase`) 마이 탭 목록은
 * 유형 필터와 커서 페이징을 함께 쓴다. 그래서 페이지를 접지 않고 [CursorPage] 그대로 넘긴다 —
 * 다음 커서를 화면이 들고 있어야 이어 받을 수 있다.
 *
 * [type] 이 null 이면 전체다. 필터를 건 목록의 개수는 상한 판정에 쓸 수 없으므로, 화면이 보여 줄
 * 「현재 개수/상한」은 [CountExperiencesUseCase] 로 따로 센다.
 */
public class GetExperiencePageUseCase
    @Inject
    constructor(
        private val experienceRepository: ExperienceRepository,
    ) {
        public suspend operator fun invoke(
            type: ExperienceType? = null,
            cursor: String? = null,
            limit: Int = ExperienceRepository.DEFAULT_PAGE_SIZE,
        ): Result<CursorPage<Experience>> =
            experienceRepository.getExperiences(
                type = type,
                cursor = cursor,
                limit = limit,
            )
    }
