package com.careercompass.core.data.support

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import com.careercompass.core.datastore.LocalStoreRegistry
import com.careercompass.core.datastore.StoreScope

/** 메모리 저장소로 동작하는 [LocalStoreRegistry]. clearScope 호출 횟수를 센다. */
internal class FakeLocalStoreRegistry : LocalStoreRegistry {
    private val stores = mutableMapOf<String, Pair<StoreScope, DataStore<Preferences>>>()
    var clearedScopes = mutableListOf<StoreScope>()

    override fun store(
        name: String,
        scope: StoreScope,
    ): DataStore<Preferences> = stores.getOrPut(name) { scope to InMemoryPreferencesDataStore() }.second

    override suspend fun clearScope(scope: StoreScope) {
        clearedScopes += scope
        stores.values.filter { it.first == scope }.forEach { (_, store) -> store.edit { it.clear() } }
    }
}
