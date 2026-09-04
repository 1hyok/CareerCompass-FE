package com.cambridge.feature.feed.domain.model

import com.cambridge.feature.feed.domain.NOON_TODAY
import com.cambridge.feature.feed.domain.posting
import com.careercompass.core.model.posting.PostingQuery
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class FeedSnapshotTest {
    @Test
    fun `빈 목록은 스냅샷이 될 수 없다`() {
        assertThrows(IllegalArgumentException::class.java) {
            FeedSnapshot(postings = emptyList(), savedAt = NOON_TODAY)
        }
    }

    @Test
    fun `공고 id 는 유일해야 한다`() {
        assertThrows(IllegalArgumentException::class.java) {
            FeedSnapshot(postings = listOf(posting(id = 1), posting(id = 1)), savedAt = NOON_TODAY)
        }
    }

    @Test
    fun `유효한 목록은 그대로 보존한다`() {
        val postings = listOf(posting(id = 1), posting(id = 2))

        val snapshot = FeedSnapshot(postings = postings, savedAt = NOON_TODAY)

        assertEquals(postings, snapshot.postings)
        assertEquals(NOON_TODAY, snapshot.savedAt)
    }

    @Test
    fun `저장 상한은 첫 페이지 크기다`() {
        assertEquals(PostingQuery.DEFAULT_LIMIT, FeedSnapshot.MAX_POSTINGS)
    }
}
