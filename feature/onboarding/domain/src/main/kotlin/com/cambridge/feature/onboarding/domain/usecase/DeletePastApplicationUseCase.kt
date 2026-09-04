package com.cambridge.feature.onboarding.domain.usecase

import com.careercompass.core.domain.repository.PastApplicationRepository
import javax.inject.Inject

/** Step 4 문서 메뉴의 삭제 — 서버 파일까지 함께 지워진다(API_SPEC §4 `DELETE /past-applications/{id}`). */
public class DeletePastApplicationUseCase
    @Inject
    constructor(
        private val pastApplicationRepository: PastApplicationRepository,
    ) {
        public suspend operator fun invoke(id: Long): Result<Unit> = pastApplicationRepository.delete(id)
    }
