package com.cambridge.core.domain.repository

import com.cambridge.core.model.experience.Experience
import com.cambridge.core.model.experience.ExperienceDraft
import com.cambridge.core.model.experience.ExperienceType
import com.cambridge.core.model.paging.CursorPage

/** 경험 카드 계약 — API_SPEC v0.1 §3 `/experiences`. */
public interface ExperienceRepository {
    /** `GET /experiences?type=&cursor=&limit=` — 최신 등록순. */
    public suspend fun getExperiences(
        type: ExperienceType? = null,
        cursor: String? = null,
        limit: Int = DEFAULT_PAGE_SIZE,
    ): Result<CursorPage<Experience>>

    public suspend fun createExperience(draft: ExperienceDraft): Result<Experience>

    public suspend fun updateExperience(
        id: Long,
        draft: ExperienceDraft,
    ): Result<Experience>

    public suspend fun deleteExperience(id: Long): Result<Unit>

    public companion object {
        public const val DEFAULT_PAGE_SIZE: Int = 20
    }
}
