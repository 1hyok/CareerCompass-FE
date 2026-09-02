package com.cambridge.core.datastore

import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.util.UUID

class DeviceDataSourceTest {
    private val dataSource = DeviceDataSource(InMemoryPreferencesDataStore())

    @Test
    fun `기기 식별자는 최초 한 번 만들어 재사용한다`() =
        runTest {
            val first = dataSource.getOrCreateDeviceId()
            val second = dataSource.getOrCreateDeviceId()

            assertEquals(first, second)
            UUID.fromString(first)
        }

    @Test
    fun `지문 로그인은 등록한 사용자 id 로 남긴다`() =
        runTest {
            assertNull(dataSource.biometricUserId.first())

            dataSource.enableBiometric(userId = 7L)

            assertEquals(7L, dataSource.biometricUserId.first())
        }

    @Test
    fun `다른 사용자가 등록하면 이전 등록을 덮어쓴다`() =
        runTest {
            dataSource.enableBiometric(userId = 7L)

            dataSource.enableBiometric(userId = 8L)

            assertEquals(8L, dataSource.biometricUserId.first())
        }

    @Test
    fun `지문 로그인을 끄면 등록 사용자 id 가 지워진다`() =
        runTest {
            dataSource.enableBiometric(userId = 7L)

            dataSource.disableBiometric()

            assertNull(dataSource.biometricUserId.first())
        }
}
