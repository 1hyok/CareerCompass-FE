package com.careercompass.core.common.url

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ExternalUrlTest {
    @Test
    fun `http 와 https 는 통과한다`() {
        assertEquals("https://www.konkuk.ac.kr/notice/1", ExternalUrl.openableOrNull("https://www.konkuk.ac.kr/notice/1"))
        assertEquals("http://board.example.com/list?page=2", ExternalUrl.openableOrNull("http://board.example.com/list?page=2"))
    }

    @Test
    fun `대문자 스킴도 같은 스킴이다`() {
        assertEquals("HTTPS://example.com/a", ExternalUrl.openableOrNull("HTTPS://example.com/a"))
    }

    @Test
    fun `앞뒤 공백은 털어 낸다`() {
        assertEquals("https://example.com", ExternalUrl.openableOrNull("  https://example.com  "))
    }

    @Test
    fun `웹이 아닌 스킴은 거절한다`() {
        assertNull(ExternalUrl.openableOrNull("intent://scan/#Intent;scheme=zxing;end"))
        assertNull(ExternalUrl.openableOrNull("market://details?id=com.example"))
        assertNull(ExternalUrl.openableOrNull("file:///data/data/com.cambridge.careercompass_fe/files/token"))
        assertNull(ExternalUrl.openableOrNull("javascript:alert(1)"))
        assertNull(ExternalUrl.openableOrNull("careercompass://postings/1"))
    }

    @Test
    fun `스킴이 없으면 보완하지 않고 거절한다`() {
        assertNull(ExternalUrl.openableOrNull("www.example.com/notice"))
        assertNull(ExternalUrl.openableOrNull("//example.com/notice"))
    }

    @Test
    fun `호스트가 없으면 거절한다`() {
        assertNull(ExternalUrl.openableOrNull("https:///notice"))
        assertNull(ExternalUrl.openableOrNull("https://@/notice"))
    }

    @Test
    fun `공백이 섞인 주소는 거절한다`() {
        assertNull(ExternalUrl.openableOrNull("https://example.com/a b"))
        assertNull(ExternalUrl.openableOrNull(""))
        assertNull(ExternalUrl.openableOrNull("   "))
    }

    @Test
    fun `리포팅용 스킴만 뽑는다`() {
        assertEquals("intent", ExternalUrl.schemeOrNull("intent://scan/#Intent;scheme=zxing;end"))
        assertEquals("https", ExternalUrl.schemeOrNull(" https://example.com/a?token=secret "))
        assertNull(ExternalUrl.schemeOrNull("example.com"))
    }
}
