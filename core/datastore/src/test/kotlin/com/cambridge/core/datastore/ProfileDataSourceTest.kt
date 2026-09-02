package com.cambridge.core.datastore

import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Test

class ProfileDataSourceTest {
    private val dataSource = ProfileDataSource(InMemoryPreferencesDataStore())

    @Test
    fun `프로필 JSON 을 저장하면 그대로 읽힌다`() =
        runTest {
            assertNull(dataSource.profileJson.first())

            dataSource.saveProfileJson("""{"id":1}""")

            assertEquals("""{"id":1}""", dataSource.profileJson.first())
        }

    @Test
    fun `온보딩 완료 힌트는 기록 전에는 null 이다`() =
        runTest {
            assertNull(dataSource.onboardingDoneHint.first())

            dataSource.setOnboardingDoneHint(false)

            assertEquals(false, dataSource.onboardingDoneHint.first())
        }

    @Test
    fun `빈 프로필 JSON 은 저장하지 않는다`() {
        assertThrows(IllegalArgumentException::class.java) {
            runBlocking { dataSource.saveProfileJson(" ") }
        }
    }
}
