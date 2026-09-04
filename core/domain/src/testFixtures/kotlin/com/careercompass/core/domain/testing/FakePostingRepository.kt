package com.careercompass.core.domain.testing

import com.careercompass.core.domain.repository.PostingRepository
import com.careercompass.core.model.paging.CursorPage
import com.careercompass.core.model.posting.Posting
import com.careercompass.core.model.posting.PostingDetail
import com.careercompass.core.model.posting.PostingQuery
import com.careercompass.core.model.posting.PostingSort
import java.util.concurrent.CopyOnWriteArrayList

/**
 * [PostingRepository] fake 정본.
 *
 * 기본 조회는 메모리 목록에 [PostingQuery] 의 유형·게시판·최소 점수·읽지 않음 필터와 정렬을 적용한다.
 * 페이징은 `cursor` 를 시작 인덱스로 해석해 [PostingQuery.limit] 개씩 돌려준다.
 */
public class FakePostingRepository(
    initial: List<Posting> = emptyList(),
    details: List<PostingDetail> = emptyList(),
    public var onGetPostings: (suspend (PostingQuery) -> Result<CursorPage<Posting>>)? = null,
    public var onGetPostingDetail: (suspend (Long) -> Result<PostingDetail>)? = null,
    public var onSetBookmarked: (suspend (Long, Boolean) -> Result<Unit>)? = null,
    public var onMarkRead: (suspend (Long) -> Result<Unit>)? = null,
) : PostingRepository {
    public val postings: CopyOnWriteArrayList<Posting> = CopyOnWriteArrayList(initial)
    public val details: CopyOnWriteArrayList<PostingDetail> = CopyOnWriteArrayList(details)
    public val queries: CopyOnWriteArrayList<PostingQuery> = CopyOnWriteArrayList()
    public val bookmarkCalls: CopyOnWriteArrayList<Pair<Long, Boolean>> = CopyOnWriteArrayList()
    public val readCalls: CopyOnWriteArrayList<Long> = CopyOnWriteArrayList()

    override suspend fun getPostings(query: PostingQuery): Result<CursorPage<Posting>> {
        queries += query
        onGetPostings?.let { return it(query) }
        val minScore = query.minScore
        val filtered =
            postings
                .filter { query.types.isEmpty() || it.type in query.types }
                .filter { query.boardIds.isEmpty() || it.board.id in query.boardIds }
                .filter { minScore == null || (it.score ?: -1) >= minScore }
                .filter { !query.unreadOnly || !it.isRead }
        val sorted =
            when (query.sort) {
                PostingSort.CollectedDesc -> filtered.sortedByDescending { it.collectedAt }
                PostingSort.DueAsc -> filtered.sortedWith(compareBy(nullsLast()) { it.dueDate })
                PostingSort.ScoreDesc -> filtered.sortedByDescending { it.score ?: -1 }
            }
        val start = query.cursor?.toIntOrNull() ?: 0
        val page = sorted.drop(start).take(query.limit)
        val next = (start + query.limit).takeIf { it < sorted.size }?.toString()
        return Result.success(CursorPage(items = page, nextCursor = next))
    }

    override suspend fun getPostingDetail(id: Long): Result<PostingDetail> {
        onGetPostingDetail?.let { return it(id) }
        return details.firstOrNull { it.id == id }?.let { Result.success(it) }
            ?: Result.failure(NoSuchElementException("posting $id"))
    }

    override suspend fun setBookmarked(
        id: Long,
        bookmarked: Boolean,
    ): Result<Unit> {
        bookmarkCalls += id to bookmarked
        onSetBookmarked?.let { return it(id, bookmarked) }
        replacePosting(id) { it.copy(isBookmarked = bookmarked) }
        replaceDetail(id) { it.copy(isBookmarked = bookmarked) }
        return Result.success(Unit)
    }

    override suspend fun markRead(id: Long): Result<Unit> {
        readCalls += id
        onMarkRead?.let { return it(id) }
        replacePosting(id) { it.copy(isRead = true) }
        replaceDetail(id) { it.copy(isRead = true) }
        return Result.success(Unit)
    }

    private fun replacePosting(
        id: Long,
        transform: (Posting) -> Posting,
    ) {
        val index = postings.indexOfFirst { it.id == id }
        if (index >= 0) postings[index] = transform(postings[index])
    }

    private fun replaceDetail(
        id: Long,
        transform: (PostingDetail) -> PostingDetail,
    ) {
        val index = details.indexOfFirst { it.id == id }
        if (index >= 0) details[index] = transform(details[index])
    }

    public companion object {
        public fun strict(): FakePostingRepository =
            FakePostingRepository(
                onGetPostings = { unexpectedCall("PostingRepository.getPostings") },
                onGetPostingDetail = { unexpectedCall("PostingRepository.getPostingDetail") },
                onSetBookmarked = { _, _ -> unexpectedCall("PostingRepository.setBookmarked") },
                onMarkRead = { unexpectedCall("PostingRepository.markRead") },
            )
    }
}
