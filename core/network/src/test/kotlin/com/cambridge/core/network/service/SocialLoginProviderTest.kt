package com.cambridge.core.network.service

import org.junit.Assert.assertEquals
import org.junit.Test

class SocialLoginProviderTest {
    @Test
    fun `supported providers expose stable wire values`() {
        assertEquals("kakao", SocialLoginProvider.Kakao.toString())
        assertEquals("google", SocialLoginProvider.Google.toString())
    }

    @Test
    fun `supported provider set is closed to Kakao and Google`() {
        assertEquals(
            listOf(SocialLoginProvider.Kakao, SocialLoginProvider.Google),
            SocialLoginProvider.entries,
        )
    }
}
