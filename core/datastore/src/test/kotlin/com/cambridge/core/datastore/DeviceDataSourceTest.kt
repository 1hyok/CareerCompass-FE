package com.cambridge.core.datastore

import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
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
    fun `지문 로그인 사용 여부를 저장한다`() =
        runTest {
            assertFalse(dataSource.isBiometricEnabled.first())

            dataSource.setBiometricEnabled(true)

            assertTrue(dataSource.isBiometricEnabled.first())
        }
}
