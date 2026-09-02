package com.cambridge.core.datastore.di

import javax.inject.Qualifier

/** Hilt 한정자: 내 프로필 캐시 전용 [androidx.datastore.core.DataStore] 바인딩(SESSION 스코프). */
@Qualifier
@Retention(AnnotationRetention.BINARY)
public annotation class ProfileDataStore
