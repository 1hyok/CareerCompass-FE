package com.careercompass.core.datastore

import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class TokenDataSourceTest {
    private val dataSource = TokenDataSource(InMemoryPreferencesDataStore())

    @Test
    fun `토큰을 저장하면 로그인 상태가 되고 읽을 수 있다`() =
        runTest {
            assertFalse(dataSource.isLoggedIn.first())

            dataSource.saveTokens(accessToken = "access", refreshToken = "refresh")

            assertTrue(dataSource.isLoggedIn.first())
            assertEquals("access", dataSource.getAccessToken())
            assertEquals("refresh", dataSource.getRefreshToken())
        }

    @Test
    fun `clear 는 두 토큰을 모두 지운다`() =
        runTest {
            dataSource.saveTokens(accessToken = "access", refreshToken = "refresh")

            dataSource.clear()

            assertNull(dataSource.getAccessToken())
            assertNull(dataSource.getRefreshToken())
            assertFalse(dataSource.isLoggedIn.first())
        }

    @Test
    fun `빈 토큰은 저장하지 않는다`() =
        runTest {
            assertThrows(IllegalArgumentException::class.java) {
                kotlinx.coroutines.runBlocking { dataSource.saveTokens(accessToken = " ", refreshToken = "refresh") }
            }
        }
}
