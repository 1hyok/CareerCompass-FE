package com.cambridge.feature.feed.domain.model

import com.cambridge.feature.feed.domain.error.FeedFailure
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class BoardUrlTest {
    @Test
    fun `공백을 지우고 스킴이 없으면 https 를 보완한다`() {
        assertEquals("https://konkuk.ac.kr/board/notice", BoardUrl.normalize("  konkuk.ac.kr/board/notice \n").getOrThrow())
    }

    @Test
    fun `스킴과 호스트는 소문자로, 경로·쿼리는 그대로 둔다`() {
        assertEquals(
            "http://konkuk.ac.kr/Board/Notice?Page=1#Top",
            BoardUrl.normalize("HTTP://Konkuk.AC.KR/Board/Notice?Page=1#Top").getOrThrow(),
        )
    }

    @Test
    fun `인코딩되지 않은 한글 경로와 포트·사용자 정보를 허용한다`() {
        assertEquals("https://www.konkuk.ac.kr/bbs/공지사항", BoardUrl.normalize("https://www.konkuk.ac.kr/bbs/공지사항").getOrThrow())
        assertEquals("http://localhost:8080/board", BoardUrl.normalize("http://localhost:8080/board").getOrThrow())
        assertEquals("https://user:pw@intra.example.com/", BoardUrl.normalize("https://user:pw@Intra.Example.com/").getOrThrow())
    }

    @Test
    fun `이미 정규화된 URL 은 그대로다`() {
        val url = "https://konkuk.ac.kr/board/notice"

        assertEquals(url, BoardUrl.normalize(url).getOrThrow())
    }

    @Test
    fun `빈 값·다른 스킴·호스트 없음·내부 공백은 InvalidBoardUrl 로 거절한다`() {
        val rejected =
            listOf(
                "",
                "   ",
                "ftp://konkuk.ac.kr/files",
                "mailto:admin@konkuk.ac.kr",
                "javascript:alert(1)",
                "https://",
                "https:///board",
                "https:konkuk.ac.kr",
                "https://konkuk ac.kr/board",
                "https://:8080/board",
            )

        rejected.forEach { input ->
            val failure = BoardUrl.normalize(input).exceptionOrNull()
            assertTrue("'$input' 은 거절돼야 한다", failure is FeedFailure.InvalidBoardUrl)
            assertEquals(input, (failure as FeedFailure.InvalidBoardUrl).input)
        }
    }
}
