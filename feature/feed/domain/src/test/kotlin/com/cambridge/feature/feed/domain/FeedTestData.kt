package com.cambridge.feature.feed.domain

import com.cambridge.core.model.board.Board
import com.cambridge.core.model.board.BoardStatus
import com.cambridge.core.model.board.BoardType
import com.cambridge.core.model.posting.Posting
import com.cambridge.core.model.posting.PostingBoardRef
import com.cambridge.core.model.posting.PostingDetail
import com.cambridge.core.model.posting.PostingType
import com.cambridge.core.model.posting.SuitabilityLabel
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

internal val SEOUL: ZoneId = ZoneId.of("Asia/Seoul")
internal val TODAY: LocalDate = LocalDate.of(2026, 9, 2)
internal val NOON_TODAY: Instant = TODAY.atTime(12, 0).atZone(SEOUL).toInstant()

/** 2026-09-02 12:00 KST 에 고정된 시계. */
internal val FIXED_CLOCK: Clock = Clock.fixed(NOON_TODAY, SEOUL)

internal fun posting(
    id: Long,
    title: String = "공고 $id",
    type: PostingType = PostingType.Recruit,
    boardId: Long = 1L,
    dueDate: LocalDate? = null,
    collectedAt: Instant = NOON_TODAY,
    score: Int? = null,
    isRead: Boolean = false,
    isBookmarked: Boolean = false,
): Posting =
    Posting(
        id = id,
        title = title,
        type = type,
        board = PostingBoardRef(id = boardId, name = "게시판 $boardId"),
        dueDate = dueDate,
        collectedAt = collectedAt,
        score = score,
        scoreLabel = score?.let(SuitabilityLabel::fromScore),
        isRead = isRead,
        isBookmarked = isBookmarked,
    )

internal fun postingDetail(
    id: Long,
    isRead: Boolean = false,
): PostingDetail =
    PostingDetail(
        id = id,
        title = "공고 $id",
        type = PostingType.Recruit,
        board = PostingBoardRef(id = 1L, name = "게시판 1"),
        url = "https://example.com/postings/$id",
        rawContent = "본문",
        dueDate = null,
        collectedAt = NOON_TODAY,
        isRead = isRead,
        isBookmarked = false,
        parsed = null,
        suitability = null,
        similar = emptyList(),
    )

internal fun board(
    id: Long,
    isActive: Boolean = true,
): Board =
    Board(
        id = id,
        url = "https://example.com/boards/$id",
        name = "게시판 $id",
        type = BoardType.Scholarship,
        cycleHours = 24,
        isActive = isActive,
        status = BoardStatus.Active,
        failCount = 0,
        lastCollectedAt = null,
    )
