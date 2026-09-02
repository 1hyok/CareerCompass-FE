package com.cambridge.core.model.posting

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant
import java.time.LocalDate

class PostingTest {
    @Test
    fun `점수 구간별 레이블은 기능 스펙 F3-2 를 따른다`() {
        assertEquals(SuitabilityLabel.VerySuitable, SuitabilityLabel.fromScore(80))
        assertEquals(SuitabilityLabel.Suitable, SuitabilityLabel.fromScore(79))
        assertEquals(SuitabilityLabel.Suitable, SuitabilityLabel.fromScore(60))
        assertEquals(SuitabilityLabel.Neutral, SuitabilityLabel.fromScore(59))
        assertEquals(SuitabilityLabel.Neutral, SuitabilityLabel.fromScore(40))
        assertEquals(SuitabilityLabel.Low, SuitabilityLabel.fromScore(39))
        assertEquals(SuitabilityLabel.Low, SuitabilityLabel.fromScore(0))
    }

    @Test
    fun `지원서 초안은 채용과 장학금에서만 지원한다`() {
        assertTrue(PostingType.Recruit.supportsApplicationDraft)
        assertTrue(PostingType.Scholarship.supportsApplicationDraft)
        assertFalse(PostingType.Contest.supportsApplicationDraft)
        assertFalse(PostingType.Activity.supportsApplicationDraft)
        assertFalse(PostingType.Other.supportsApplicationDraft)
    }

    @Test
    fun `점수와 레이블은 함께 있어야 한다`() {
        assertThrows(IllegalArgumentException::class.java) { posting(score = 88, scoreLabel = null) }
        assertThrows(IllegalArgumentException::class.java) { posting(score = null, scoreLabel = SuitabilityLabel.Suitable) }
    }

    @Test
    fun `마감 지난 공고와 남은 일수를 계산한다`() {
        val today = LocalDate.of(2026, 5, 20)
        val posting = posting(dueDate = LocalDate.of(2026, 5, 25))

        assertFalse(posting.isExpired(today))
        assertEquals(5L, posting.daysUntilDue(today))
        assertTrue(posting.copy(dueDate = LocalDate.of(2026, 5, 19)).isExpired(today))
        assertEquals(null, posting.copy(dueDate = null).daysUntilDue(today))
    }

    @Test
    fun `조회 조건은 중복 필터와 범위를 거부한다`() {
        assertThrows(IllegalArgumentException::class.java) { PostingQuery(boardIds = listOf(1, 1)) }
        assertThrows(IllegalArgumentException::class.java) { PostingQuery(minScore = 101) }
        assertThrows(IllegalArgumentException::class.java) { PostingQuery(limit = 0) }
        assertThrows(IllegalArgumentException::class.java) { PostingQuery(cursor = " ") }
    }

    private fun posting(
        score: Int? = 88,
        scoreLabel: SuitabilityLabel? = SuitabilityLabel.VerySuitable,
        dueDate: LocalDate? = LocalDate.of(2026, 5, 25),
    ) = Posting(
        id = 101,
        title = "2026 카카오 SW 인턴십",
        type = PostingType.Recruit,
        board = PostingBoardRef(id = 3, name = "공식 채용"),
        dueDate = dueDate,
        collectedAt = Instant.parse("2026-05-17T22:00:00Z"),
        score = score,
        scoreLabel = scoreLabel,
        isRead = false,
        isBookmarked = false,
    )
}
