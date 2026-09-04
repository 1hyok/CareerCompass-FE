package com.careercompass.core.datastore

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import com.careercompass.core.datastore.di.DeviceDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.io.IOException
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 기기 수명(DEVICE 스코프) 값 — 소셜 로그인 요청의 `deviceId` 와 지문 로그인을 등록한 사용자.
 *
 * `deviceId` 는 최초 요청 때 UUID v4 를 만들어 저장하고 이후 같은 값을 돌려준다. 로그아웃해도 유지된다 —
 * 서버가 기기 단위 토큰(`POST /auth/biometric/register`)을 이 값에 묶기 때문이다.
 *
 * 지문 로그인은 「켜짐」 Boolean 이 아니라 **등록한 사용자 id** 로 남긴다. Boolean 이면 로그아웃해도 살아남아
 * 다른 계정의 세션이 지문 화면을 거쳐 열렸다(#81). 어느 계정이 켰는지를 기록해 두고, 켜짐 여부는 data 계층이
 * 현재 세션 사용자와 대조해 계산한다.
 *
 * 등록 제안을 「나중에」로 넘긴 기록도 같은 이유로 사용자 id 로 남긴다 — 다만 이쪽은 **집합**이다. 등록은 기기당
 * 하나지만 거절은 여러 계정이 각자 할 수 있고, 한 칸이면 다음 계정의 거절이 앞 계정의 거절을 지워 이미 넘긴
 * 사용자에게 다시 묻게 된다.
 *
 * 화면 테마도 여기 있다. 계정이 아니라 **이 기기에서 보는 방식**이라 로그아웃해도 남아야 하고, 로그인 전
 * (스플래시·로그인 화면)에도 이미 적용돼 있어야 하기 때문이다. 값의 뜻은 `core:model` 의 `ThemeMode` 가 갖고
 * 여기서는 문자열로만 다룬다 — datastore 는 모델을 모른다.
 */
@Singleton
public class DeviceDataSource
    @Inject
    constructor(
        @param:DeviceDataStore private val dataStore: DataStore<Preferences>,
    ) {
        private object Keys {
            val DEVICE_ID = stringPreferencesKey("device_id")
            val BIOMETRIC_USER_ID = longPreferencesKey("biometric_user_id")

            // Preferences 에 Long 집합 타입이 없어 문자열로 담는다. 읽을 때 파싱 실패한 항목은 버린다.
            val BIOMETRIC_ENROLL_DECLINED_USER_IDS = stringSetPreferencesKey("biometric_enroll_declined_user_ids")

            val THEME_MODE = stringPreferencesKey("theme_mode")
        }

        private val preferencesFlow: Flow<Preferences> =
            dataStore.data.catch { exception ->
                if (exception is IOException) emit(emptyPreferences()) else throw exception
            }

        /** 이 기기에서 지문 로그인을 등록한 사용자 id. 등록한 적 없거나 껐으면 null. */
        public val biometricUserId: Flow<Long?> = preferencesFlow.map { prefs -> prefs[Keys.BIOMETRIC_USER_ID] }

        /** 이 기기에서 고른 화면 테마의 저장 문자열. 고른 적 없으면 null — 뜻(기본값)은 읽는 쪽이 정한다. */
        public val themeMode: Flow<String?> = preferencesFlow.map { prefs -> prefs[Keys.THEME_MODE] }

        /** 이 기기에서 지문 등록 제안을 「나중에」로 넘긴 사용자 id 들. */
        public val biometricEnrollDeclinedUserIds: Flow<Set<Long>> =
            preferencesFlow.map { prefs ->
                prefs[Keys.BIOMETRIC_ENROLL_DECLINED_USER_IDS].orEmpty().mapNotNullTo(mutableSetOf(), String::toLongOrNull)
            }

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

        /** 지문 로그인을 [userId] 계정에 귀속해 켠다. 다른 계정이 등록해 둔 값은 덮어쓴다. */
        public suspend fun enableBiometric(userId: Long) {
            dataStore.edit { prefs -> prefs[Keys.BIOMETRIC_USER_ID] = userId }
        }

        /** 지문 로그인을 끈다 — 등록 사용자 id 를 지운다. */
        public suspend fun disableBiometric() {
            dataStore.edit { prefs -> prefs.remove(Keys.BIOMETRIC_USER_ID) }
        }

        /** 화면 테마를 저장한다. */
        public suspend fun setThemeMode(value: String) {
            dataStore.edit { prefs -> prefs[Keys.THEME_MODE] = value }
        }

        /** [userId] 에게는 지문 등록을 다시 제안하지 않는다고 남긴다. 이미 남아 있으면 그대로다. */
        public suspend fun declineBiometricEnroll(userId: Long) {
            dataStore.edit { prefs ->
                prefs[Keys.BIOMETRIC_ENROLL_DECLINED_USER_IDS] =
                    prefs[Keys.BIOMETRIC_ENROLL_DECLINED_USER_IDS].orEmpty() + userId.toString()
            }
        }
    }
