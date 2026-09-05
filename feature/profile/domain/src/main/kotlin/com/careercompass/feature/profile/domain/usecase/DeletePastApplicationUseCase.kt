package com.careercompass.feature.profile.domain.usecase

import com.careercompass.core.domain.repository.PastApplicationRepository
import javax.inject.Inject

/**
 * 과거 지원서를 지운다 — `DELETE /past-applications/{id}`.
 *
 * 서버의 원본 파일까지 함께 지워진다. 화면은 그 사실을 알리고 확인을 받은 뒤 호출한다(F1-4).
 */
public class DeletePastApplicationUseCase
    @Inject
    constructor(
        private val pastApplicationRepository: PastApplicationRepository,
    ) {
        public suspend operator fun invoke(id: Long): Result<Unit> = pastApplicationRepository.delete(id)
    }
