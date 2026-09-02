package com.cambridge.core.datastore

import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertThrows
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class LocalStoreRegistryImplTest {
    @get:Rule
    val folder = TemporaryFolder()

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val key = stringPreferencesKey("value")

    @After
    fun tearDown() {
        scope.cancel()
    }

    private fun registry(): LocalStoreRegistryImpl =
        LocalStoreRegistryImpl(
            produceFile = { name -> File(folder.root, "$name.preferences_pb") },
            registryScope = scope,
        )

    @Test
    fun `같은 이름은 같은 인스턴스를 돌려주고 다른 scope 로 재요청하면 실패한다`() {
        val registry = registry()

        val first = registry.store("Token", StoreScope.SESSION)
        val second = registry.store("Token", StoreScope.SESSION)

        assertSame(first, second)
        assertThrows(IllegalStateException::class.java) { registry.store("Token", StoreScope.DEVICE) }
    }

    @Test
    fun `clearScope 는 해당 scope 저장소만 비운다`() =
        runBlocking {
            val registry = registry()
            val session = registry.store("Token", StoreScope.SESSION)
            val device = registry.store("Device", StoreScope.DEVICE)
            session.edit { it[key] = "access" }
            device.edit { it[key] = "device-id" }

            registry.clearScope(StoreScope.SESSION)

            assertNull(session.data.first()[key])
            assertEquals("device-id", device.data.first()[key])
        }

    @Test
    fun `이전 프로세스에서 등록된 저장소도 매니페스트로 찾아 비운다`() =
        runBlocking {
            val previous = registry()
            previous.store("Token", StoreScope.SESSION).edit { it[key] = "access" }
            previous.awaitPendingRegistrations()
            scope.cancel()

            val restartedScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
            val restarted =
                LocalStoreRegistryImpl(
                    produceFile = { name -> File(folder.root, "$name.preferences_pb") },
                    registryScope = restartedScope,
                )
            try {
                restarted.clearScope(StoreScope.SESSION)

                assertNull(restarted.store("Token", StoreScope.SESSION).data.first()[key])
            } finally {
                restartedScope.cancel()
            }
        }
}
