package com.careercompass.feature.onboarding.data.di

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import com.careercompass.core.datastore.LocalStoreRegistry
import com.careercompass.core.datastore.StoreScope
import com.careercompass.feature.onboarding.data.OnboardingProgressRepositoryImpl
import com.careercompass.feature.onboarding.domain.repository.OnboardingProgressRepository
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * 온보딩 data 바인딩.
 *
 * `public` 인 이유는 [com.careercompass.core.data.di.CoreAuthModule] 과 같다 — `app` androidTest 가
 * `@TestInstallIn(replaces = [OnboardingDataModule::class])` 로 fake 로 갈아끼운다. 바인딩 메서드만 `internal` 로 닫는다.
 */
@Module
@InstallIn(SingletonComponent::class)
public abstract class OnboardingDataModule {
    @Binds
    @Singleton
    internal abstract fun bindOnboardingProgressRepository(impl: OnboardingProgressRepositoryImpl): OnboardingProgressRepository
}

/** 저장소 획득 — SESSION 스코프라 로그아웃 시 [LocalStoreRegistry.clearScope] 가 함께 비운다. */
@Module
@InstallIn(SingletonComponent::class)
internal object OnboardingProgressStoreModule {
    // name 은 저장 파일명 계약 — 바꾸면 진행 중이던 사용자의 재개 지점이 사라진다.
    @Provides
    @Singleton
    @OnboardingProgressDataStore
    fun provideOnboardingProgressDataStore(registry: LocalStoreRegistry): DataStore<Preferences> =
        registry.store(name = "OnboardingProgress", scope = StoreScope.SESSION)
}
