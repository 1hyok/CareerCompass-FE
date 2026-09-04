package com.careercompass.feature.feed.data.di

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import com.careercompass.core.datastore.LocalStoreRegistry
import com.careercompass.core.datastore.StoreScope
import com.careercompass.feature.feed.data.FeedSnapshotRepositoryImpl
import com.careercompass.feature.feed.domain.repository.FeedSnapshotRepository
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * 피드 data 바인딩.
 *
 * `public` 인 이유는 [com.careercompass.feature.onboarding.data.di.OnboardingDataModule] 과 같다 — `app` androidTest 가
 * `@TestInstallIn(replaces = [FeedDataModule::class])` 로 fake 로 갈아끼울 수 있게 둔다. 바인딩 메서드만 `internal` 로 닫는다.
 */
@Module
@InstallIn(SingletonComponent::class)
public abstract class FeedDataModule {
    @Binds
    @Singleton
    internal abstract fun bindFeedSnapshotRepository(impl: FeedSnapshotRepositoryImpl): FeedSnapshotRepository
}

/** 저장소 획득 — SESSION 스코프라 로그아웃 시 [LocalStoreRegistry.clearScope] 가 함께 비운다. */
@Module
@InstallIn(SingletonComponent::class)
internal object FeedSnapshotStoreModule {
    /** 저장 파일명 계약 — 바꾸면 이전에 받아 둔 스냅샷이 사라진다(기능상 손실은 다음 조회 한 번). */
    const val STORE_NAME: String = "FeedSnapshot"

    @Provides
    @Singleton
    @FeedSnapshotDataStore
    fun provideFeedSnapshotDataStore(registry: LocalStoreRegistry): DataStore<Preferences> =
        registry.store(name = STORE_NAME, scope = StoreScope.SESSION)
}
