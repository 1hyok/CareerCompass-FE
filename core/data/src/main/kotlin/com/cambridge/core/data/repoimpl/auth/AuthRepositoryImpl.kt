package com.cambridge.core.data.repoimpl.auth

import com.cambridge.core.common.result.runCatchingCancellable
import com.cambridge.core.data.failure.mapAuthFailure
import com.cambridge.core.data.failure.mapDataFailure
import com.cambridge.core.data.mapper.AuthMapper
import com.cambridge.core.datastore.DeviceDataSource
import com.cambridge.core.datastore.LocalStoreRegistry
import com.cambridge.core.datastore.StoreScope
import com.cambridge.core.datastore.TokenDataSource
import com.cambridge.core.domain.repository.AuthRepository
import com.cambridge.core.model.auth.Session
import com.cambridge.core.model.auth.SocialProvider
import com.cambridge.core.model.auth.TokenBundle
import com.cambridge.core.network.dto.BiometricRegisterRequestDto
import com.cambridge.core.network.dto.LogoutRequestDto
import com.cambridge.core.network.dto.RefreshRequestDto
import com.cambridge.core.network.dto.SocialLoginRequestDto
import com.cambridge.core.network.model.requireData
import com.cambridge.core.network.model.requireOk
import com.cambridge.core.network.service.AuthApiService
import com.cambridge.core.network.service.TokenApiService
import com.cambridge.core.network.token.AccessTokenExpiryTracker
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

internal class AuthRepositoryImpl
    @Inject
    constructor(
        private val tokenDataSource: TokenDataSource,
        private val deviceDataSource: DeviceDataSource,
        private val authApiService: AuthApiService,
        private val tokenApiService: TokenApiService,
        // 발급 응답의 expiresIn 으로 선제 reissue deadline 을 기록하고, 세션 종료 시 함께 정리한다.
        private val expiryTracker: AccessTokenExpiryTracker,
        // 로그아웃 시 SESSION 스코프 로컬 저장소 일괄 정리.
        private val localStoreRegistry: LocalStoreRegistry,
    ) : AuthRepository {
        override val isLoggedIn: Flow<Boolean> get() = tokenDataSource.isLoggedIn

        override val isBiometricEnabled: Flow<Boolean> get() = deviceDataSource.isBiometricEnabled

        override suspend fun socialLogin(
            provider: SocialProvider,
            providerToken: String,
            fcmToken: String?,
        ): Result<Session> =
            runCatchingCancellable {
                val data =
                    authApiService
                        .socialLogin(
                            provider = AuthMapper.toWireProvider(provider),
                            body =
                                SocialLoginRequestDto(
                                    accessToken = providerToken,
                                    deviceId = deviceDataSource.getOrCreateDeviceId(),
                                    fcmToken = fcmToken,
                                ),
                        ).requireData()
                AuthMapper.toSession(data)
            }.mapAuthFailure()

        override suspend fun saveSession(session: Session): Result<Unit> =
            runCatchingCancellable {
                tokenDataSource.saveTokens(accessToken = session.accessToken, refreshToken = session.refreshToken)
                recordIssuedExpiresIn(session.expiresInSeconds)
            }

        override suspend fun getAccessToken(): Result<String?> = runCatchingCancellable { tokenDataSource.getAccessToken() }

        override suspend fun getRefreshToken(): Result<String?> = runCatchingCancellable { tokenDataSource.getRefreshToken() }

        override suspend fun rotateToken(): Result<TokenBundle> =
            runCatchingCancellable {
                val refreshToken = tokenDataSource.getRefreshToken() ?: error("리프레시 토큰이 존재하지 않습니다.")
                val bundle = AuthMapper.toTokenBundle(tokenApiService.refresh(RefreshRequestDto(refreshToken)).requireData())
                tokenDataSource.saveTokens(accessToken = bundle.accessToken, refreshToken = bundle.refreshToken)
                bundle
            }

        /**
         * 서버 로그아웃은 best-effort — 네트워크가 실패해도 사용자는 로그아웃 상태로 가야 한다.
         * 로컬 정리는 서버 호출 뒤여야 한다: 로그아웃 요청도 AuthInterceptor 를 지나므로 그 시점엔 토큰이 살아 있어야 한다.
         */
        override suspend fun logout(): Result<Unit> =
            runCatchingCancellable {
                tokenDataSource.getRefreshToken()?.let { refreshToken ->
                    runCatchingCancellable { authApiService.logout(LogoutRequestDto(refreshToken)) }
                }
                clearLocalSession()
            }

        override suspend fun clearSession(): Result<Unit> = runCatchingCancellable { clearLocalSession() }

        override suspend fun registerBiometric(): Result<Unit> =
            runCatchingCancellable {
                authApiService.registerBiometric(BiometricRegisterRequestDto(deviceDataSource.getOrCreateDeviceId())).requireOk()
                deviceDataSource.setBiometricEnabled(true)
            }.mapDataFailure()

        override suspend fun setBiometricEnabled(enabled: Boolean): Result<Unit> =
            runCatchingCancellable { deviceDataSource.setBiometricEnabled(enabled) }

        private suspend fun clearLocalSession() {
            localStoreRegistry.clearScope(StoreScope.SESSION)
            // tracker 는 network 계층 in-memory 상태라 레지스트리 관할 밖. 남기면 재로그인 후 이전 토큰 기준 deadline 으로 오판한다.
            expiryTracker.clear()
        }

        private fun recordIssuedExpiresIn(expiresInSeconds: Long?) {
            expiresInSeconds?.let(expiryTracker::record) ?: expiryTracker.clear()
        }
    }
