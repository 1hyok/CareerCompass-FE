package com.cambridge.core.datastore

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.cambridge.core.datastore.di.ProfileDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 내 프로필의 로컬 캐시(SESSION 스코프) — 로그아웃·세션 정리 때 레지스트리가 함께 비우고, 프로세스 종료를 견딘다.
 *
 * 프로필은 wire JSON 문자열로 보관한다 — 스키마 해석(DTO·매핑)은 data 계층 몫이고 여기는 저장만 안다.
 * 사용자 id 만은 별도 키로 함께 남긴다 — JSON 을 해석하지 않고도 「현재 세션이 누구 것인지」 를 읽어야
 * DEVICE 스코프의 지문 등록 사용자와 대조할 수 있다(#81).
 * 프로세스 메모리 캐시였을 때는 로그아웃해도 남아 다음 로그인 사용자에게 이전 프로필이 새어 나갔고, 프로세스가
 * 죽으면 오프라인 시작 판정 근거가 사라졌다.
 */
@Singleton
public class ProfileDataSource
    @Inject
    constructor(
        @param:ProfileDataStore private val dataStore: DataStore<Preferences>,
    ) {
        private object Keys {
            val PROFILE_JSON = stringPreferencesKey("profile_json")
            val USER_ID = longPreferencesKey("user_id")
            val ONBOARDING_DONE_HINT = booleanPreferencesKey("onboarding_done_hint")
        }

        private val preferencesFlow: Flow<Preferences> =
            dataStore.data.catch { exception ->
                if (exception is IOException) emit(emptyPreferences()) else throw exception
            }

        /** 마지막으로 성공적으로 받은 프로필의 wire JSON. 받은 적 없으면 null. */
        public val profileJson: Flow<String?> = preferencesFlow.map { prefs -> prefs[Keys.PROFILE_JSON] }

        /** 현재 세션 사용자의 id — [profileJson] 과 같은 응답에서 온 값. 프로필을 받기 전에는 null. */
        public val userId: Flow<Long?> = preferencesFlow.map { prefs -> prefs[Keys.USER_ID] }

        /**
         * 로그인 응답으로 아는 온보딩 완료 여부. 프로필을 아직 받지 못한 채 서버 조회가 실패했을 때의 시작 판정
         * 근거다. 기록이 없으면 null.
         */
        public val onboardingDoneHint: Flow<Boolean?> = preferencesFlow.map { prefs -> prefs[Keys.ONBOARDING_DONE_HINT] }

        /** 프로필 JSON 과 그 주인의 id 를 한 번에 저장한다 — 둘이 어긋난 채로 읽히는 순간이 없어야 한다. */
        public suspend fun saveProfile(
            json: String,
            userId: Long,
        ) {
            require(json.isNotBlank()) { "profile json must not be blank" }
            dataStore.edit { prefs ->
                prefs[Keys.PROFILE_JSON] = json
                prefs[Keys.USER_ID] = userId
            }
        }

        public suspend fun setOnboardingDoneHint(done: Boolean) {
            dataStore.edit { prefs -> prefs[Keys.ONBOARDING_DONE_HINT] = done }
        }
    }
