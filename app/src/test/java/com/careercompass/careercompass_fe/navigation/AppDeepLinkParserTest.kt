package com.careercompass.careercompass_fe.navigation

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/** `careercompass://postings/{id}` 계약을 고정한다 — notification 모듈이 만드는 URI 가 이 규칙을 따른다. */
class AppDeepLinkParserTest {
    private fun parse(
        scheme: String?,
        host: String?,
        vararg pathSegments: String,
    ): AppDeepLink? = AppDeepLinkParser.parse(scheme = scheme, host = host, pathSegments = pathSegments.toList())

    @Test
    fun `careercompass 스킴의 postings 호스트와 양의 정수 id 는 공고 상세다`() {
        assertEquals(AppDeepLink.PostingDetail(101), parse("careercompass", "postings", "101"))
        assertEquals(AppDeepLink.PostingDetail(1), parse("careercompass", "postings", "1"))
        assertEquals(AppDeepLink.PostingDetail(Long.MAX_VALUE), parse("careercompass", "postings", Long.MAX_VALUE.toString()))
    }

    @Test
    fun `스킴과 호스트는 대소문자를 가리지 않는다`() {
        assertEquals(AppDeepLink.PostingDetail(101), parse("CareerCompass", "Postings", "101"))
    }

    @Test
    fun `다른 스킴이나 호스트는 무시한다`() {
        assertNull(parse("https", "postings", "101"))
        assertNull(parse("careercompass", "boards", "101"))
        assertNull(parse(null, "postings", "101"))
        assertNull(parse("careercompass", null, "101"))
    }

    @Test
    fun `id 가 양의 정수가 아니면 무시한다`() {
        assertNull(parse("careercompass", "postings", "0"))
        assertNull(parse("careercompass", "postings", "-1"))
        assertNull(parse("careercompass", "postings", "+5"))
        assertNull(parse("careercompass", "postings", "abc"))
        assertNull(parse("careercompass", "postings", "1a"))
        assertNull(parse("careercompass", "postings", ""))
        assertNull(parse("careercompass", "postings", "99999999999999999999"))
    }

    @Test
    fun `경로 세그먼트가 정확히 하나가 아니면 무시한다`() {
        assertNull(parse("careercompass", "postings"))
        assertNull(parse("careercompass", "postings", "101", "raw"))
    }

    @Test
    fun `공개 상수는 URI 계약과 같다`() {
        assertEquals("careercompass", DEEP_LINK_SCHEME)
        assertEquals("postings", DEEP_LINK_HOST_POSTINGS)
    }
}
