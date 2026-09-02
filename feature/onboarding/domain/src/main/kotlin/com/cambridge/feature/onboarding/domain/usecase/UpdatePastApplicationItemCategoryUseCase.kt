package com.cambridge.feature.onboarding.domain.usecase

import com.cambridge.core.domain.repository.PastApplicationRepository
import com.cambridge.core.model.application.PastApplicationCategory
import com.cambridge.core.model.application.PastApplicationItem
import javax.inject.Inject

/**
 * Step 4 항목 분류의 수동 조정 — 기능 스펙 F1-4 「분류가 불확실하면 사용자가 조정할 수 있다」.
 *
 * 서버가 조정된 항목을 돌려주므로(API_SPEC §4 `PATCH /past-applications/{appId}/items/{itemId}`) 호출부는
 * 응답으로 목록의 그 항목만 갈아 끼운다 — `confident` 도 서버 판정을 그대로 따른다.
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
