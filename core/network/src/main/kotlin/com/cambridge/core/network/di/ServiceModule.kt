package com.cambridge.core.network.di

import com.cambridge.core.network.service.AuthApiService
import com.cambridge.core.network.service.BoardApiService
import com.cambridge.core.network.service.ExperienceApiService
import com.cambridge.core.network.service.PastApplicationApiService
import com.cambridge.core.network.service.PostingApiService
import com.cambridge.core.network.service.TokenApiService
import com.cambridge.core.network.service.UserApiService
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import retrofit2.Retrofit
import retrofit2.create
import javax.inject.Named
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
public object ServiceModule {
    @Provides
    @Singleton
    public fun provideAuthApiService(
        @Named(NetworkQualifiers.MAIN_RETROFIT) retrofit: Retrofit,
    ): AuthApiService = retrofit.create()

    @Provides
    @Singleton
    public fun provideTokenApiService(
        @Named(NetworkQualifiers.REFRESH_RETROFIT) retrofit: Retrofit,
    ): TokenApiService = retrofit.create()

    @Provides
    @Singleton
    public fun provideUserApiService(
        @Named(NetworkQualifiers.MAIN_RETROFIT) retrofit: Retrofit,
    ): UserApiService = retrofit.create()

    @Provides
    @Singleton
    public fun provideExperienceApiService(
        @Named(NetworkQualifiers.MAIN_RETROFIT) retrofit: Retrofit,
    ): ExperienceApiService = retrofit.create()

    @Provides
    @Singleton
    public fun providePastApplicationApiService(
        @Named(NetworkQualifiers.UPLOAD_RETROFIT) retrofit: Retrofit,
    ): PastApplicationApiService = retrofit.create()

    @Provides
    @Singleton
    public fun providePostingApiService(
        @Named(NetworkQualifiers.MAIN_RETROFIT) retrofit: Retrofit,
    ): PostingApiService = retrofit.create()

    @Provides
    @Singleton
    public fun provideBoardApiService(
        @Named(NetworkQualifiers.MAIN_RETROFIT) retrofit: Retrofit,
    ): BoardApiService = retrofit.create()
}
