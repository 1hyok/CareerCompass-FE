package com.cambridge.core.datastore

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import com.cambridge.core.datastore.di.DeviceDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.io.IOException
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 기기 수명(DEVICE 스코프) 값 — 소셜 로그인 요청의 `deviceId` 와 지문 로그인 사용 여부.
 *
 * `deviceId` 는 최초 요청 때 UUID v4 를 만들어 저장하고 이후 같은 값을 돌려준다. 로그아웃해도 유지된다 —
 * 서버가 기기 단위 토큰(`POST /auth/biometric/register`)을 이 값에 묶기 때문이다.
 */
@Singleton
public class DeviceDataSource
    @Inject
    constructor(
        @param:DeviceDataStore private val dataStore: DataStore<Preferences>,
    ) {
        private object Keys {
            val DEVICE_ID = stringPreferencesKey("device_id")
            val BIOMETRIC_ENABLED = booleanPreferencesKey("biometric_enabled")
        }

        private val preferencesFlow: Flow<Preferences> =
            dataStore.data.catch { exception ->
                if (exception is IOException) emit(emptyPreferences()) else throw exception
            }

        public val isBiometricEnabled: Flow<Boolean> = preferencesFlow.map { prefs -> prefs[Keys.BIOMETRIC_ENABLED] == true }

        /** 저장된 기기 식별자를 돌려주고, 없으면 새로 만들어 저장한다. */
        public suspend fun getOrCreateDeviceId(): String {
            preferencesFlow.first()[Keys.DEVICE_ID]?.let { return it }
            var created: String? = null
            dataStore.edit { prefs ->
                val existing = prefs[Keys.DEVICE_ID]
                if (existing == null) {
                    val generated = UUID.randomUUID().toString()
                    prefs[Keys.DEVICE_ID] = generated
                    created = generated
                } else {
                    created = existing
                }
            }
            return checkNotNull(created)
        }

        public suspend fun setBiometricEnabled(enabled: Boolean) {
            dataStore.edit { prefs -> prefs[Keys.BIOMETRIC_ENABLED] = enabled }
        }
    }
