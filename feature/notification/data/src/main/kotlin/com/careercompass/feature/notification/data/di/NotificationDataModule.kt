package com.careercompass.feature.notification.data.di

import com.careercompass.feature.notification.data.NotificationRepositoryImpl
import com.careercompass.feature.notification.domain.repository.NotificationRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * 알림 data 바인딩.
 *
 * `public` 인 이유는 [FeedDataModule][com.careercompass.feature.feed.data.di.FeedDataModule] 과 같다 —
 * `app` androidTest 가 `@TestInstallIn(replaces = [NotificationDataModule::class])` 로 fake 를 끼울 수 있게
 * 둔다. 바인딩 메서드만 `internal` 로 닫는다.
 */
@Module
@InstallIn(SingletonComponent::class)
public abstract class NotificationDataModule {
    @Binds
    @Singleton
    internal abstract fun bindNotificationRepository(impl: NotificationRepositoryImpl): NotificationRepository
}
