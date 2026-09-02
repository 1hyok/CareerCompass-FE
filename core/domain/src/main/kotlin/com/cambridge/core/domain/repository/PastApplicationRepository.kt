package com.cambridge.core.domain.repository

import com.cambridge.core.model.application.PastApplication
import com.cambridge.core.model.application.PastApplicationCategory
import com.cambridge.core.model.application.PastApplicationItem
import com.cambridge.core.model.application.UploadFile

/** 과거 지원서 계약 — API_SPEC v0.1 §4 `/past-applications`. */
public interface PastApplicationRepository {
    /** `POST /past-applications/upload` (multipart) — 업로드·텍스트 추출·AI 분류까지 끝난 결과를 돌려준다. */
    public suspend fun upload(
        file: UploadFile,
        label: String,
    ): Result<PastApplication>

    public suspend fun getPastApplications(): Result<List<PastApplication>>

    /** `PATCH /past-applications/{appId}/items/{itemId}` — 항목 분류 수동 조정. */
    public suspend fun updateItemCategory(
        applicationId: Long,
        itemId: Long,
        category: PastApplicationCategory,
    ): Result<PastApplicationItem>

    public suspend fun delete(id: Long): Result<Unit>
}
