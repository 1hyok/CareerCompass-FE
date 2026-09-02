package com.cambridge.feature.feed.domain.model

import com.cambridge.core.model.posting.PostingQuery
import com.cambridge.core.model.posting.PostingSort
import com.cambridge.core.model.posting.PostingType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class FeedQueryTest {
    @Test
    fun `minScore 는 null 또는 60·70·80 만 허용한다`() {
        listOf(null, 60, 70, 80).forEach { FeedQuery(minScore = it) }
        listOf(0, 59, 65, 81, 100).forEach { score ->
            assertThrows(IllegalArgumentException::class.java) { FeedQuery(minScore = score) }
        }
    }

    @Test
    fun `검색어는 앞뒤 공백을 지워 저장하고 같은 값으로 취급한다`() {
        val query = FeedQuery(searchQuery = "  카카오 인턴 ")

        assertEquals("카카오 인턴", query.searchQuery)
        assertTrue(query.hasSearchQuery)
        assertEquals(FeedQuery(searchQuery = "카카오 인턴"), query)
        assertEquals(FeedQuery(searchQuery = "카카오 인턴").hashCode(), query.hashCode())
        assertEquals("", query.copy(searchQuery = "   ").searchQuery)
        assertFalse(FeedQuery().hasSearchQuery)
    }

    @Test
    fun `빈 필터는 빈 서버 쿼리로 옮긴다`() {
        val postingQuery = FeedQuery().toPostingQuery(cursor = null)

        assertEquals(PostingQuery(), postingQuery)
        assertEquals(emptyList<Long>(), postingQuery.boardIds)
        assertEquals(emptyList<PostingType>(), postingQuery.types)
        assertEquals(null, postingQuery.minScore)
        assertFalse(postingQuery.unreadOnly)
        assertEquals(PostingSort.CollectedDesc, postingQuery.sort)
        assertEquals(PostingQuery.DEFAULT_LIMIT, postingQuery.limit)
    }

    @Test
    fun `서버 파라미터가 있는 조건은 정렬된 목록으로 옮기고 검색어·마감일은 싣지 않는다`() {
        val query =
            FeedQuery(
                types = setOf(PostingType.Contest, PostingType.Recruit),
                boardIds = setOf(7L, 3L),
                deadline = FeedDeadlineFilter.WithinWeek,
                minScore = 70,
                unreadOnly = true,
                sort = PostingSort.ScoreDesc,
                searchQuery = "카카오",
            )

        val postingQuery = query.toPostingQuery(cursor = "abc", limit = 50)

        assertEquals(
            PostingQuery(
                boardIds = listOf(3L, 7L),
                types = listOf(PostingType.Recruit, PostingType.Contest),
                minScore = 70,
                unreadOnly = true,
                sort = PostingSort.ScoreDesc,
                cursor = "abc",
                limit = 50,
            ),
            postingQuery,
        )
    }

    @Test
    fun `isDefault 는 아무 조건도 걸리지 않은 조회만 참이다`() {
        assertTrue(FeedQuery().isDefault)
        assertTrue(FeedQuery(searchQuery = "   ").isDefault)

        assertFalse(FeedQuery(types = setOf(PostingType.Recruit)).isDefault)
        assertFalse(FeedQuery(boardIds = setOf(1L)).isDefault)
        assertFalse(FeedQuery(deadline = FeedDeadlineFilter.WithinWeek).isDefault)
        assertFalse(FeedQuery(minScore = 60).isDefault)
        assertFalse(FeedQuery(unreadOnly = true).isDefault)
        assertFalse(FeedQuery(sort = PostingSort.DueAsc).isDefault)
        assertFalse(FeedQuery(searchQuery = "카카오").isDefault)
        assertTrue(FeedQuery(searchQuery = "카카오").copy(searchQuery = "").isDefault)
    }

    @Test
    fun `copy 는 바꾼 필드만 반영한다`() {
        val base = FeedQuery(types = setOf(PostingType.Scholarship), minScore = 60)

        val copied = base.copy(unreadOnly = true)

        assertEquals(setOf(PostingType.Scholarship), copied.types)
        assertEquals(60, copied.minScore)
        assertTrue(copied.unreadOnly)
        assertFalse(copied == base)
    }
}
