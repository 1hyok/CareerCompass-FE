package com.cambridge.careercompass_fe.di

import com.cambridge.core.data.di.CoreAuthModule
import com.cambridge.core.data.di.CoreRepositoryModule
import com.cambridge.core.domain.device.DeviceIdentityProvider
import com.cambridge.core.domain.repository.AuthRepository
import com.cambridge.core.domain.repository.BoardRepository
import com.cambridge.core.domain.repository.ExperienceRepository
import com.cambridge.core.domain.repository.PastApplicationRepository
import com.cambridge.core.domain.repository.PostingRepository
import com.cambridge.core.domain.repository.UserProfileRepository
import com.cambridge.core.domain.testing.FakeAuthRepository
import com.cambridge.core.domain.testing.FakeBoardRepository
import com.cambridge.core.domain.testing.FakeExperienceRepository
import com.cambridge.core.domain.testing.FakePastApplicationRepository
import com.cambridge.core.domain.testing.FakePostingRepository
import com.cambridge.core.domain.testing.FakeUserProfileRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.components.SingletonComponent
import dagger.hilt.testing.TestInstallIn
import javax.inject.Singleton

/**
 * managed-device 계측은 실제 서버·OAuth 대신 core:domain testFixtures 의 fake 를 주입한다.
 *
 * fake 인스턴스를 `@Provides` 로 노출해 두면 테스트가 같은 인스턴스를 주입받아(`@Inject lateinit var`) 세션·프로필
 * 상태를 시나리오별로 조정할 수 있다. 기본은 «세션 없음» 이라 앱이 로그인 화면에서 시작한다.
 */
@Module
@TestInstallIn(components = [SingletonComponent::class], replaces = [CoreAuthModule::class])
object TestCoreAuthModule {
    @Provides
    @Singleton
    fun provideFakeAuthRepository(): FakeAuthRepository = FakeAuthRepository()

    @Provides
    @Singleton
    fun provideAuthRepository(fake: FakeAuthRepository): AuthRepository = fake

    @Provides
    @Singleton
    fun provideDeviceIdentityProvider(): DeviceIdentityProvider =
        object : DeviceIdentityProvider {
            override suspend fun deviceId(): String = "instrumentation-device"
        }
}

@Module
@TestInstallIn(components = [SingletonComponent::class], replaces = [CoreRepositoryModule::class])
object TestCoreRepositoryModule {
    @Provides
    @Singleton
    fun provideFakeUserProfileRepository(): FakeUserProfileRepository = FakeUserProfileRepository()

    @Provides
    @Singleton
    fun provideUserProfileRepository(fake: FakeUserProfileRepository): UserProfileRepository = fake

    @Provides
    @Singleton
    fun provideFakeExperienceRepository(): FakeExperienceRepository = FakeExperienceRepository()

    @Provides
    @Singleton
    fun provideExperienceRepository(fake: FakeExperienceRepository): ExperienceRepository = fake

    @Provides
    @Singleton
    fun provideFakePastApplicationRepository(): FakePastApplicationRepository = FakePastApplicationRepository()

    @Provides
    @Singleton
    fun providePastApplicationRepository(fake: FakePastApplicationRepository): PastApplicationRepository = fake

    @Provides
    @Singleton
    fun provideFakePostingRepository(): FakePostingRepository = FakePostingRepository()

    @Provides
    @Singleton
    fun providePostingRepository(fake: FakePostingRepository): PostingRepository = fake

    @Provides
    @Singleton
    fun provideFakeBoardRepository(): FakeBoardRepository = FakeBoardRepository()

    @Provides
    @Singleton
    fun provideBoardRepository(fake: FakeBoardRepository): BoardRepository = fake
}
