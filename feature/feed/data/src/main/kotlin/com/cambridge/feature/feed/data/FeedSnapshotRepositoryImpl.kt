package com.cambridge.feature.feed.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import com.cambridge.core.common.result.runCatchingCancellable
import com.cambridge.feature.feed.data.di.FeedSnapshotDataStore
import com.cambridge.feature.feed.data.snapshot.FeedSnapshotDto
import com.cambridge.feature.feed.data.snapshot.toDomain
import com.cambridge.feature.feed.data.snapshot.toDto
import com.cambridge.feature.feed.domain.model.FeedSnapshot
import com.cambridge.feature.feed.domain.repository.FeedSnapshotRepository
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 피드 스냅샷의 SESSION 스코프 DataStore 구현.
 *
 * 스냅샷 전체를 JSON 문자열 키 하나([Keys.SNAPSHOT_JSON])에 둔다 — 항목별 키로 쪼개면 부분 손상 시 일관성이
 * 깨지고, 파일당 1개인 DataStore 를 그대로 쓰면서 원자적으로 덮어쓰기 쉽다. 저장은 [FeedSnapshot.MAX_POSTINGS]
 * 건까지 자른다. 해석 실패(형식 변경·모르는 열거값·불변식 위반)는 「스냅샷 없음」으로 읽고, 읽기 자체의
 * [IOException] 도 빈 값으로 본다.
 */
@Singleton
internal class FeedSnapshotRepositoryImpl
    @Inject
    constructor(
        @param:FeedSnapshotDataStore private val dataStore: DataStore<Preferences>,
        private val json: Json,
    ) : FeedSnapshotRepository {
        private object Keys {
            val SNAPSHOT_JSON = stringPreferencesKey("snapshot_json")
        }

        override suspend fun save(snapshot: FeedSnapshot): Result<Unit> =
            runCatchingCancellable {
                val encoded = json.encodeToString(FeedSnapshotDto.serializer(), snapshot.truncated().toDto())
                dataStore.edit { prefs -> prefs[Keys.SNAPSHOT_JSON] = encoded }
                Unit
            }

        override suspend fun load(): Result<FeedSnapshot?> =
            runCatchingCancellable {
                dataStore.data
                    .catch { exception ->
                        if (exception is IOException) emit(emptyPreferences()) else throw exception
                    }.first()[Keys.SNAPSHOT_JSON]
                    ?.let(::decodeOrNull)
            }

        override suspend fun clear(): Result<Unit> =
            runCatchingCancellable {
                dataStore.edit { prefs -> prefs.remove(Keys.SNAPSHOT_JSON) }
                Unit
            }

        private fun FeedSnapshot.truncated(): FeedSnapshot =
            if (postings.size <= FeedSnapshot.MAX_POSTINGS) this else copy(postings = postings.take(FeedSnapshot.MAX_POSTINGS))

        private fun decodeOrNull(raw: String): FeedSnapshot? =
            try {
                json.decodeFromString(FeedSnapshotDto.serializer(), raw).toDomain()
            } catch (_: SerializationException) {
                null
            } catch (_: IllegalArgumentException) {
                null
            }
    }
