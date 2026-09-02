package com.cambridge.core.domain.repository

import com.cambridge.core.model.paging.CursorPage
import com.cambridge.core.model.posting.Posting
import com.cambridge.core.model.posting.PostingDetail
import com.cambridge.core.model.posting.PostingQuery

/** 공고 계약 — API_SPEC v0.1 §5 `/postings`. */
public interface PostingRepository {
    public suspend fun getPostings(query: PostingQuery): Result<CursorPage<Posting>>

    /** `GET /postings/{id}` — 구조화 결과·적합도·유사 공고 포함. */
    public suspend fun getPostingDetail(id: Long): Result<PostingDetail>

    /** `POST`/`DELETE /postings/{id}/bookmark`. */
    public suspend fun setBookmarked(
        id: Long,
        bookmarked: Boolean,
    ): Result<Unit>

    /** `POST /postings/{id}/read`. */
    public suspend fun markRead(id: Long): Result<Unit>
}
