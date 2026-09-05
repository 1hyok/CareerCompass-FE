package com.careercompass.feature.profile.domain.usecase

import com.careercompass.core.domain.repository.PastApplicationRepository
import com.careercompass.core.model.application.PastApplicationCategory
import com.careercompass.core.model.application.PastApplicationItem
import javax.inject.Inject

/**
 * 지원서 항목의 분류를 손으로 고친다 — `PATCH /past-applications/{appId}/items/{itemId}`.
 *
 * 서버가 고쳐진 항목을 돌려주므로 호출부는 목록의 그 항목만 갈아 끼운다. `confident` 도 서버 판정을
 * 그대로 따른다 — 사용자가 손댔으니 확실해졌다고 우리가 정하지 않는다.
 */
public class UpdatePastApplicationItemCategoryUseCase
    @Inject
    constructor(
        private val pastApplicationRepository: PastApplicationRepository,
    ) {
        public suspend operator fun invoke(
            applicationId: Long,
            itemId: Long,
            category: PastApplicationCategory,
        ): Result<PastApplicationItem> =
            pastApplicationRepository.updateItemCategory(
                applicationId = applicationId,
                itemId = itemId,
                category = category,
            )
    }
