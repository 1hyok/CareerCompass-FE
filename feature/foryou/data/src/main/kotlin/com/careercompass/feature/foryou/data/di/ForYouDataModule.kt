package com.careercompass.feature.foryou.data.di

import com.careercompass.feature.foryou.data.ForYouRepositoryImpl
import com.careercompass.feature.foryou.data.RoadmapRepositoryImpl
import com.careercompass.feature.foryou.data.StrengthExportRepositoryImpl
import com.careercompass.feature.foryou.domain.repository.ForYouRepository
import com.careercompass.feature.foryou.domain.repository.RoadmapRepository
import com.careercompass.feature.foryou.domain.repository.StrengthExportRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * For You data 바인딩 — API_SPEC v0.1 §7 의 세 계약.
 *
 * `public` 인 이유는 [com.careercompass.feature.feed.data.di.FeedDataModule] 과 같다 — `app` androidTest 가
 * `@TestInstallIn(replaces = [ForYouDataModule::class])` 로 fake 로 갈아끼울 수 있게 둔다.
 * 바인딩 메서드만 `internal` 로 닫는다.
 */
@Module
@InstallIn(SingletonComponent::class)
public abstract class ForYouDataModule {
    @Binds
    @Singleton
    internal abstract fun bindForYouRepository(impl: ForYouRepositoryImpl): ForYouRepository

    @Binds
    @Singleton
    internal abstract fun bindRoadmapRepository(impl: RoadmapRepositoryImpl): RoadmapRepository

    @Binds
    @Singleton
    internal abstract fun bindStrengthExportRepository(impl: StrengthExportRepositoryImpl): StrengthExportRepository
}
