package com.cambridge.core.data.repoimpl.experience

import com.cambridge.core.common.result.runCatchingCancellable
import com.cambridge.core.data.failure.mapDataFailure
import com.cambridge.core.data.mapper.ExperienceMapper
import com.cambridge.core.domain.repository.ExperienceRepository
import com.cambridge.core.model.experience.Experience
import com.cambridge.core.model.experience.ExperienceDraft
import com.cambridge.core.model.experience.ExperienceType
import com.cambridge.core.model.paging.CursorPage
import com.cambridge.core.network.model.requireData
import com.cambridge.core.network.model.requireOk
import com.cambridge.core.network.service.ExperienceApiService
import javax.inject.Inject

internal class ExperienceRepositoryImpl
    @Inject
    constructor(
        private val experienceApiService: ExperienceApiService,
    ) : ExperienceRepository {
        override suspend fun getExperiences(
            type: ExperienceType?,
            cursor: String?,
            limit: Int,
        ): Result<CursorPage<Experience>> =
            runCatchingCancellable {
                val page = experienceApiService.getExperiences(type = type?.wireValue, cursor = cursor, limit = limit).requireData()
                CursorPage(
                    items = page.experiences.map(ExperienceMapper::toExperience),
                    nextCursor = page.nextCursor?.takeIf(String::isNotBlank),
                )
            }.mapDataFailure()

        override suspend fun createExperience(draft: ExperienceDraft): Result<Experience> =
            runCatchingCancellable {
                ExperienceMapper.toExperience(experienceApiService.createExperience(ExperienceMapper.toRequest(draft)).requireData())
            }.mapDataFailure()

        override suspend fun updateExperience(
            id: Long,
            draft: ExperienceDraft,
        ): Result<Experience> =
            runCatchingCancellable {
                ExperienceMapper.toExperience(experienceApiService.updateExperience(id, ExperienceMapper.toRequest(draft)).requireData())
            }.mapDataFailure()

        override suspend fun deleteExperience(id: Long): Result<Unit> =
            runCatchingCancellable { experienceApiService.deleteExperience(id).requireOk() }.mapDataFailure()
    }
