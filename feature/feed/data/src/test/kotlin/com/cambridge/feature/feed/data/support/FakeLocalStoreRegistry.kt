package com.cambridge.feature.feed.data.support

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import com.cambridge.core.datastore.LocalStoreRegistry
import com.cambridge.core.datastore.StoreScope

/** 메모리 저장소로 동작하는 [LocalStoreRegistry]. 어떤 이름이 어느 scope 로 등록됐는지 [registrations] 로 검증한다. */
internal class FakeLocalStoreRegistry : LocalStoreRegistry {
    private val stores = mutableMapOf<String, Pair<StoreScope, DataStore<Preferences>>>()
    val registrations = mutableListOf<Pair<String, StoreScope>>()
    val clearedScopes = mutableListOf<StoreScope>()

    override fun store(
        name: String,
        scope: StoreScope,
    ): DataStore<Preferences> {
        registrations += name to scope
        return stores.getOrPut(name) { scope to InMemoryPreferencesDataStore() }.second
    }

    override suspend fun clearScope(scope: StoreScope) {
        clearedScopes += scope
        stores.values.filter { it.first == scope }.forEach { (_, store) -> store.edit { it.clear() } }
    }
}
