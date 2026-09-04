package com.cambridge.core.data.di

import com.cambridge.core.data.repoimpl.application.PastApplicationRepositoryImpl
import com.cambridge.core.data.repoimpl.board.BoardRepositoryImpl
import com.cambridge.core.data.repoimpl.experience.ExperienceRepositoryImpl
import com.cambridge.core.data.repoimpl.posting.PostingRepositoryImpl
import com.cambridge.core.data.repoimpl.settings.AppSettingsRepositoryImpl
import com.cambridge.core.data.repoimpl.user.UserProfileRepositoryImpl
import com.cambridge.core.domain.repository.BoardRepository
import com.cambridge.core.domain.repository.ExperienceRepository
import com.cambridge.core.domain.repository.PastApplicationRepository
import com.cambridge.core.domain.repository.PostingRepository
import com.cambridge.core.domain.repository.UserProfileRepository
import com.cambridge.core.domain.settings.AppSettingsRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/** 사용자 데이터 바인딩 — androidTest 가 `@TestInstallIn(replaces = [CoreRepositoryModule::class])` 로 교체한다. */
@Module
@InstallIn(SingletonComponent::class)
public abstract class CoreRepositoryModule {
    @Binds
    @Singleton
    internal abstract fun bindUserProfileRepository(impl: UserProfileRepositoryImpl): UserProfileRepository

    @Binds
    @Singleton
    internal abstract fun bindExperienceRepository(impl: ExperienceRepositoryImpl): ExperienceRepository

    @Binds
    @Singleton
    internal abstract fun bindPastApplicationRepository(impl: PastApplicationRepositoryImpl): PastApplicationRepository

    @Binds
    @Singleton
    internal abstract fun bindPostingRepository(impl: PostingRepositoryImpl): PostingRepository

    @Binds
    @Singleton
    internal abstract fun bindBoardRepository(impl: BoardRepositoryImpl): BoardRepository

    @Binds
    @Singleton
    internal abstract fun bindAppSettingsRepository(impl: AppSettingsRepositoryImpl): AppSettingsRepository
}
