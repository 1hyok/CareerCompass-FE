package com.careercompass.core.datastore

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import com.careercompass.core.datastore.di.TokenDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 액세스·리프레시 토큰의 로컬 저장(SESSION 스코프). 읽기 스트림은 [IOException] 시 [emptyPreferences] 로 복구한다.
 */
@Singleton
public class TokenDataSource
    @Inject
    constructor(
        @param:TokenDataStore private val dataStore: DataStore<Preferences>,
    ) {
        private object Keys {
            val ACCESS_TOKEN = stringPreferencesKey("access_token")
            val REFRESH_TOKEN = stringPreferencesKey("refresh_token")
        }

        private val preferencesFlow: Flow<Preferences> =
            dataStore.data.catch { exception ->
                if (exception is IOException) emit(emptyPreferences()) else throw exception
            }

        public val isLoggedIn: Flow<Boolean> = preferencesFlow.map { prefs -> prefs[Keys.ACCESS_TOKEN] != null }

        public suspend fun saveTokens(
            accessToken: String,
            refreshToken: String,
        ) {
            require(accessToken.isNotBlank()) { "accessToken must not be blank" }
            require(refreshToken.isNotBlank()) { "refreshToken must not be blank" }
            dataStore.edit { prefs ->
                prefs[Keys.ACCESS_TOKEN] = accessToken
                prefs[Keys.REFRESH_TOKEN] = refreshToken
            }
        }

        public suspend fun getAccessToken(): String? = preferencesFlow.first()[Keys.ACCESS_TOKEN]

        public suspend fun getRefreshToken(): String? = preferencesFlow.first()[Keys.REFRESH_TOKEN]

        public suspend fun clear() {
            dataStore.edit { prefs ->
                prefs.remove(Keys.ACCESS_TOKEN)
                prefs.remove(Keys.REFRESH_TOKEN)
            }
        }
    }
