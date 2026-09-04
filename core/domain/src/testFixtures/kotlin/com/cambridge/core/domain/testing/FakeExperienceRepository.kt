package com.cambridge.core.domain.testing

import com.cambridge.core.domain.repository.ExperienceRepository
import com.cambridge.core.model.experience.Experience
import com.cambridge.core.model.experience.ExperienceDraft
import com.cambridge.core.model.experience.ExperienceType
import com.cambridge.core.model.paging.CursorPage
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.atomic.AtomicLong

/** [ExperienceRepository] fake 정본. 기본은 메모리 목록(최신 등록순)을 유지한다. */
public class FakeExperienceRepository(
    initial: List<Experience> = emptyList(),
    public var onGetExperiences: (suspend (ExperienceType?, String?, Int) -> Result<CursorPage<Experience>>)? = null,
    public var onCreateExperience: (suspend (ExperienceDraft) -> Result<Experience>)? = null,
    public var onUpdateExperience: (suspend (Long, ExperienceDraft) -> Result<Experience>)? = null,
    public var onDeleteExperience: (suspend (Long) -> Result<Unit>)? = null,
) : ExperienceRepository {
    public val experiences: CopyOnWriteArrayList<Experience> = CopyOnWriteArrayList(initial)
    public val createdDrafts: CopyOnWriteArrayList<ExperienceDraft> = CopyOnWriteArrayList()
    private val nextId = AtomicLong((initial.maxOfOrNull { it.id } ?: 0L) + 1)

    override suspend fun getExperiences(
        type: ExperienceType?,
        cursor: String?,
        limit: Int,
    ): Result<CursorPage<Experience>> {
        onGetExperiences?.let { return it(type, cursor, limit) }
        val filtered = experiences.filter { type == null || it.type == type }
        return Result.success(CursorPage(items = filtered.take(limit), nextCursor = null))
    }

    override suspend fun createExperience(draft: ExperienceDraft): Result<Experience> {
        createdDrafts += draft
        onCreateExperience?.let { return it(draft) }
        val created =
            Experience(
                id = nextId.getAndIncrement(),
                title = draft.title,
                startPoint = draft.startPoint,
                endPoint = draft.endPoint,
                details = draft.details,
                createdAt = null,
            )
        experiences.add(0, created)
        return Result.success(created)
    }

    override suspend fun updateExperience(
        id: Long,
        draft: ExperienceDraft,
    ): Result<Experience> {
        onUpdateExperience?.let { return it(id, draft) }
        val index = experiences.indexOfFirst { it.id == id }
        if (index < 0) return Result.failure(NoSuchElementException("experience $id"))
        val updated =
            experiences[index].copy(
                title = draft.title,
                startPoint = draft.startPoint,
                endPoint = draft.endPoint,
                details = draft.details,
            )
        experiences[index] = updated
        return Result.success(updated)
    }

    override suspend fun deleteExperience(id: Long): Result<Unit> {
        onDeleteExperience?.let { return it(id) }
        return if (experiences.removeIf { it.id == id }) Result.success(Unit) else Result.failure(NoSuchElementException("experience $id"))
    }

    public companion object {
        public fun strict(): FakeExperienceRepository =
            FakeExperienceRepository(
                onGetExperiences = { _, _, _ -> unexpectedCall("ExperienceRepository.getExperiences") },
                onCreateExperience = { unexpectedCall("ExperienceRepository.createExperience") },
                onUpdateExperience = { _, _ -> unexpectedCall("ExperienceRepository.updateExperience") },
                onDeleteExperience = { unexpectedCall("ExperienceRepository.deleteExperience") },
            )
    }
}
