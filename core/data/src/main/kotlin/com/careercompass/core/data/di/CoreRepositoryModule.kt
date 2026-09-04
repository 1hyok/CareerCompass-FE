package com.careercompass.core.data.di

import com.careercompass.core.data.repoimpl.application.PastApplicationRepositoryImpl
import com.careercompass.core.data.repoimpl.board.BoardRepositoryImpl
import com.careercompass.core.data.repoimpl.experience.ExperienceRepositoryImpl
import com.careercompass.core.data.repoimpl.posting.PostingRepositoryImpl
import com.careercompass.core.data.repoimpl.settings.AppSettingsRepositoryImpl
import com.careercompass.core.data.repoimpl.user.UserProfileRepositoryImpl
import com.careercompass.core.domain.repository.BoardRepository
import com.careercompass.core.domain.repository.ExperienceRepository
import com.careercompass.core.domain.repository.PastApplicationRepository
import com.careercompass.core.domain.repository.PostingRepository
import com.careercompass.core.domain.repository.UserProfileRepository
import com.careercompass.core.domain.settings.AppSettingsRepository
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
