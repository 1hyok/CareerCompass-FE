package com.careercompass.feature.profile.domain.usecase

import com.careercompass.core.domain.repository.PastApplicationRepository
import com.careercompass.core.model.application.PastApplication
import javax.inject.Inject

/**
 * 올려 둔 과거 지원서 전부를 가져온다 — `GET /past-applications`.
 *
 * 최대 10개(F1-4)라 페이징이 없다. 항목(`items`)까지 함께 오므로 목록 화면이 항목 개수와
 * 「분류가 불확실한 항목」을 이 응답만으로 그린다.
 */
public class GetPastApplicationsUseCase
    @Inject
    constructor(
        private val pastApplicationRepository: PastApplicationRepository,
    ) {
        public suspend operator fun invoke(): Result<List<PastApplication>> = pastApplicationRepository.getPastApplications()
    }
