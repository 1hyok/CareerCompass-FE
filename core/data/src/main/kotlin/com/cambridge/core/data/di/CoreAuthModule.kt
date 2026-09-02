package com.cambridge.core.data.di

import com.cambridge.core.data.repoimpl.auth.AuthRepositoryImpl
import com.cambridge.core.data.repoimpl.device.DeviceIdentityProviderImpl
import com.cambridge.core.domain.device.DeviceIdentityProvider
import com.cambridge.core.domain.repository.AuthRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * 세션·기기 식별 바인딩.
 *
 * 이 모듈이 `public` 인 이유 — `app` 의 androidTest 가 `@TestInstallIn(replaces = [CoreAuthModule::class])` 로
 * fake 로 갈아끼운다. `replaces` 는 모듈 클래스를 참조하므로 `internal` 로 닫을 수 없고, 바인딩 메서드만
 * `internal` 로 닫으려면 interface 가 아니라 abstract class 여야 한다.
 */
@Module
@InstallIn(SingletonComponent::class)
public abstract class CoreAuthModule {
    @Binds
    @Singleton
    internal abstract fun bindAuthRepository(impl: AuthRepositoryImpl): AuthRepository

    @Binds
    @Singleton
    internal abstract fun bindDeviceIdentityProvider(impl: DeviceIdentityProviderImpl): DeviceIdentityProvider
}
