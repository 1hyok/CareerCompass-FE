package com.cambridge.feature.feed.data.di

import javax.inject.Qualifier

/** Hilt 한정자: 피드 오프라인 스냅샷 저장 전용 [androidx.datastore.core.DataStore] 바인딩. */
@Qualifier
@Retention(AnnotationRetention.BINARY)
internal annotation class FeedSnapshotDataStore
