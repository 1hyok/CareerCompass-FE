package com.careercompass.core.domain.testing

import com.careercompass.core.domain.repository.PastApplicationRepository
import com.careercompass.core.model.application.PastApplication
import com.careercompass.core.model.application.PastApplicationCategory
import com.careercompass.core.model.application.PastApplicationItem
import com.careercompass.core.model.application.UploadFile
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.atomic.AtomicLong

/** [PastApplicationRepository] fake 정본. 기본 업로드는 분류 항목 없는 지원서를 만든다. */
public class FakePastApplicationRepository(
    initial: List<PastApplication> = emptyList(),
    public var onUpload: (suspend (UploadFile, String) -> Result<PastApplication>)? = null,
    public var onGetPastApplications: (suspend () -> Result<List<PastApplication>>)? = null,
    public var onUpdateItemCategory: (suspend (Long, Long, PastApplicationCategory) -> Result<PastApplicationItem>)? = null,
    public var onDelete: (suspend (Long) -> Result<Unit>)? = null,
) : PastApplicationRepository {
    public val applications: CopyOnWriteArrayList<PastApplication> = CopyOnWriteArrayList(initial)
    public val uploads: CopyOnWriteArrayList<Pair<UploadFile, String>> = CopyOnWriteArrayList()
    private val nextId = AtomicLong((initial.maxOfOrNull { it.id } ?: 0L) + 1)

    override suspend fun upload(
        file: UploadFile,
        label: String,
    ): Result<PastApplication> {
        uploads += file to label
        onUpload?.let { return it(file, label) }
        val created = PastApplication(id = nextId.getAndIncrement(), label = label, items = emptyList(), createdAt = null)
        applications += created
        return Result.success(created)
    }

    override suspend fun getPastApplications(): Result<List<PastApplication>> {
        onGetPastApplications?.let { return it() }
        return Result.success(applications.toList())
    }

    override suspend fun updateItemCategory(
        applicationId: Long,
        itemId: Long,
        category: PastApplicationCategory,
    ): Result<PastApplicationItem> {
        onUpdateItemCategory?.let { return it(applicationId, itemId, category) }
        val index = applications.indexOfFirst { it.id == applicationId }
        if (index < 0) return Result.failure(NoSuchElementException("application $applicationId"))
        val application = applications[index]
        val item = application.items.firstOrNull { it.id == itemId } ?: return Result.failure(NoSuchElementException("item $itemId"))
        val updatedItem = item.copy(category = category, confident = true)
        applications[index] = application.copy(items = application.items.map { if (it.id == itemId) updatedItem else it })
        return Result.success(updatedItem)
    }

    override suspend fun delete(id: Long): Result<Unit> {
        onDelete?.let { return it(id) }
        return if (applications.removeIf { it.id == id }) {
            Result.success(
                Unit,
            )
        } else {
            Result.failure(NoSuchElementException("application $id"))
        }
    }

    public companion object {
        public fun strict(): FakePastApplicationRepository =
            FakePastApplicationRepository(
                onUpload = { _, _ -> unexpectedCall("PastApplicationRepository.upload") },
                onGetPastApplications = { unexpectedCall("PastApplicationRepository.getPastApplications") },
                onUpdateItemCategory = { _, _, _ -> unexpectedCall("PastApplicationRepository.updateItemCategory") },
                onDelete = { unexpectedCall("PastApplicationRepository.delete") },
            )
    }
}
