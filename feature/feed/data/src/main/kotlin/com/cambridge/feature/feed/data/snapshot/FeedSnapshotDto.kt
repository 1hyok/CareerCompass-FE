package com.cambridge.feature.feed.data.snapshot

import com.cambridge.core.model.posting.Posting
import com.cambridge.core.model.posting.PostingBoardRef
import com.cambridge.core.model.posting.PostingType
import com.cambridge.core.model.posting.SuitabilityLabel
import com.cambridge.feature.feed.domain.model.FeedSnapshot
import kotlinx.serialization.Serializable
import java.time.Instant
import java.time.LocalDate

/**
 * [FeedSnapshot] 의 로컬 저장 형식 — 서버 응답 DTO 와는 별개다(응답 형식이 바뀌어도 저장 파일이 깨지지 않게).
 *
 * 열거값은 wire 값 문자열로, 시각·날짜는 ISO-8601 문자열로 둔다. 읽을 때 모르는 값을 만나면
 * [IllegalArgumentException] 으로 끝나고 저장소는 그 기록을 「스냅샷 없음」으로 본다.
 */
@Serializable
internal data class FeedSnapshotDto(
    val savedAt: String,
    val postings: List<SnapshotPostingDto>,
)

@Serializable
internal data class SnapshotPostingDto(
    val id: Long,
    val title: String,
    val type: String,
    val boardId: Long,
    val boardName: String,
    val dueDate: String?,
    val collectedAt: String,
    val score: Int?,
    val scoreLabel: String?,
    val isRead: Boolean,
    val isBookmarked: Boolean,
)

internal fun FeedSnapshot.toDto(): FeedSnapshotDto =
    FeedSnapshotDto(
        savedAt = savedAt.toString(),
        postings = postings.map(Posting::toDto),
    )

internal fun Posting.toDto(): SnapshotPostingDto =
    SnapshotPostingDto(
        id = id,
        title = title,
        type = type.wireValue,
        boardId = board.id,
        boardName = board.name,
        dueDate = dueDate?.toString(),
        collectedAt = collectedAt.toString(),
        score = score,
        scoreLabel = scoreLabel?.wireValue,
        isRead = isRead,
        isBookmarked = isBookmarked,
    )

/** @throws IllegalArgumentException 모르는 열거값, 형식이 어긋난 시각, 도메인 불변식 위반. */
internal fun FeedSnapshotDto.toDomain(): FeedSnapshot =
    FeedSnapshot(
        postings = postings.map(SnapshotPostingDto::toDomain),
        savedAt = parseInstant(savedAt),
    )

internal fun SnapshotPostingDto.toDomain(): Posting =
    Posting(
        id = id,
        title = title,
        type = requireNotNull(PostingType.fromWireValue(type)) { "unknown posting type: $type" },
        board = PostingBoardRef(id = boardId, name = boardName),
        dueDate = dueDate?.let(::parseLocalDate),
        collectedAt = parseInstant(collectedAt),
        score = score,
        scoreLabel = scoreLabel?.let { requireNotNull(SuitabilityLabel.fromWireValue(it)) { "unknown score label: $it" } },
        isRead = isRead,
        isBookmarked = isBookmarked,
    )

private fun parseInstant(value: String): Instant =
    runCatching { Instant.parse(value) }.getOrElse { throw IllegalArgumentException("invalid instant: $value", it) }

private fun parseLocalDate(value: String): LocalDate =
    runCatching { LocalDate.parse(value) }.getOrElse { throw IllegalArgumentException("invalid date: $value", it) }
