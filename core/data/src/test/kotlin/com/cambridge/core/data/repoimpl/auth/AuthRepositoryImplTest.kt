package com.cambridge.core.data.repoimpl.auth

import com.cambridge.core.data.support.FakeLocalStoreRegistry
import com.cambridge.core.data.support.InMemoryPreferencesDataStore
import com.cambridge.core.datastore.DeviceDataSource
import com.cambridge.core.datastore.ProfileDataSource
import com.cambridge.core.datastore.StoreScope
import com.cambridge.core.datastore.TokenDataSource
import com.cambridge.core.domain.error.CoreAuthFailure
import com.cambridge.core.domain.error.SessionEndedException
import com.cambridge.core.model.auth.Session
import com.cambridge.core.model.auth.SocialProvider
import com.cambridge.core.network.dto.BiometricRegisterRequestDto
import com.cambridge.core.network.dto.LogoutRequestDto
import com.cambridge.core.network.dto.RefreshDto
import com.cambridge.core.network.dto.RefreshRequestDto
import com.cambridge.core.network.dto.SocialLoginDto
import com.cambridge.core.network.dto.SocialLoginRequestDto
import com.cambridge.core.network.model.ApiErrorDto
import com.cambridge.core.network.model.ApiException
import com.cambridge.core.network.model.BaseResponse
import com.cambridge.core.network.service.AuthApiService
import com.cambridge.core.network.service.SocialLoginProvider
import com.cambridge.core.network.service.TokenApiService
import com.cambridge.core.network.token.AccessTokenExpiryTracker
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.net.UnknownHostException

class AuthRepositoryImplTest {
    private class FakeAuthApi : AuthApiService {
        var loginResponse: () -> BaseResponse<SocialLoginDto> = {
            BaseResponse(ok = true, data = SocialLoginDto("access", "refresh", isNewUser = true, expiresIn = 3600))
        }
        val loginRequests = mutableListOf<Pair<SocialLoginProvider, SocialLoginRequestDto>>()
        var logoutThrows: Throwable? = null
        val logoutRequests = mutableListOf<LogoutRequestDto>()
        val biometricRequests = mutableListOf<BiometricRegisterRequestDto>()

        override suspend fun socialLogin(
            provider: SocialLoginProvider,
            body: SocialLoginRequestDto,
        ): BaseResponse<SocialLoginDto> {
            loginRequests += provider to body
            return loginResponse()
        }

        override suspend fun logout(body: LogoutRequestDto): BaseResponse<Unit> {
            logoutRequests += body
            logoutThrows?.let { throw it }
            return BaseResponse(ok = true)
        }

        override suspend fun registerBiometric(body: BiometricRegisterRequestDto): BaseResponse<Unit> {
            biometricRequests += body
            return BaseResponse(ok = true)
        }
    }

    private class FakeTokenApi : TokenApiService {
        var response: suspend () -> BaseResponse<RefreshDto> = { BaseResponse(ok = true, data = RefreshDto("access-2", "refresh-2", 1800)) }

        override suspend fun refresh(body: RefreshRequestDto): BaseResponse<RefreshDto> = response()
    }

    private val authApi = FakeAuthApi()
    private val tokenApi = FakeTokenApi()
    private val registry = FakeLocalStoreRegistry()
    private val tokenDataSource = TokenDataSource(registry.store("Token", StoreScope.SESSION))
    private val deviceDataSource = DeviceDataSource(InMemoryPreferencesDataStore())
    private val profileDataSource = ProfileDataSource(registry.store("Profile", StoreScope.SESSION))
    private var now = 0L
    private val tracker = AccessTokenExpiryTracker { now }
    private val repository =
        AuthRepositoryImpl(
            tokenDataSource = tokenDataSource,
            deviceDataSource = deviceDataSource,
            authApiService = authApi,
            tokenApiService = tokenApi,
            expiryTracker = tracker,
            localStoreRegistry = registry,
            profileDataSource = profileDataSource,
        )

    @Test
    fun `소셜 로그인은 기기 식별자를 실어 보내고 세션을 돌려준다`() =
        runTest {
            val session = repository.socialLogin(SocialProvider.Kakao, "kakao-token", fcmToken = null).getOrThrow()

            val (provider, request) = authApi.loginRequests.single()
            assertEquals(SocialLoginProvider.Kakao, provider)
            assertEquals(deviceDataSource.getOrCreateDeviceId(), request.deviceId)
            assertNull(request.fcmToken)
            assertEquals(Session("access", "refresh", isNewUser = true, expiresInSeconds = 3600), session)
            assertFalse(repository.isLoggedIn.first())
        }

    @Test
    fun `세션 저장은 토큰과 만료 기록을 남긴다`() =
        runTest {
            repository.saveSession(Session("access", "refresh", isNewUser = false, expiresInSeconds = 30)).getOrThrow()

            assertTrue(repository.isLoggedIn.first())
            assertEquals("access", repository.getAccessToken().getOrThrow())
            assertTrue(tracker.isExpiringSoon())
        }

    @Test
    fun `세션 저장은 신규 여부를 온보딩 완료 힌트로 남긴다`() =
        runTest {
            repository.saveSession(Session("access", "refresh", isNewUser = true, expiresInSeconds = 3600)).getOrThrow()
            assertEquals(false, profileDataSource.onboardingDoneHint.first())

            repository.saveSession(Session("access-2", "refresh-2", isNewUser = false, expiresInSeconds = 3600)).getOrThrow()
            assertEquals(true, profileDataSource.onboardingDoneHint.first())

            repository.logout().getOrThrow()
            assertNull(profileDataSource.onboardingDoneHint.first())
        }

    @Test
    fun `로그인 거절과 전송 실패를 인증 사유로 옮긴다`() =
        runTest {
            authApi.loginResponse = { throw ApiException("AUTH_INVALID", null, "거절", status = 401) }
            assertTrue(repository.socialLogin(SocialProvider.Google, "t", null).exceptionOrNull() is CoreAuthFailure.SocialLoginRejected)

            authApi.loginResponse = { throw UnknownHostException("dns") }
            assertTrue(repository.socialLogin(SocialProvider.Google, "t", null).exceptionOrNull() is CoreAuthFailure.NetworkUnavailable)

            authApi.loginResponse = { BaseResponse(ok = false, error = ApiErrorDto("AUTH_INVALID", "만료")) }
            assertTrue(repository.socialLogin(SocialProvider.Google, "t", null).exceptionOrNull() is CoreAuthFailure.SocialLoginRejected)
        }

    @Test
    fun `토큰 회전은 새 토큰을 저장하고 돌려준다`() =
        runTest {
            tokenDataSource.saveTokens("access", "refresh")

            val bundle = repository.rotateToken().getOrThrow()

            assertEquals("access-2", bundle.accessToken)
            assertEquals("access-2", tokenDataSource.getAccessToken())
            assertEquals("refresh-2", tokenDataSource.getRefreshToken())
        }

    @Test
    fun `리프레시 토큰이 없으면 회전은 실패한다`() =
        runTest {
            assertTrue(repository.rotateToken().isFailure)
        }

    @Test
    fun `로그아웃은 서버 실패와 무관하게 SESSION 저장소를 비운다`() =
        runTest {
            tokenDataSource.saveTokens("access", "refresh")
            tracker.record(3600)
            authApi.logoutThrows = UnknownHostException("offline")

            repository.logout().getOrThrow()

            assertEquals("refresh", authApi.logoutRequests.single().refreshToken)
            assertEquals(listOf(StoreScope.SESSION), registry.clearedScopes)
            assertFalse(repository.isLoggedIn.first())
            assertFalse(tracker.isExpiringSoon())
        }

    @Test
    fun `회전 도중 로그아웃이 끝나면 회전 결과를 버리고 세션 종료로 실패한다`() =
        runTest {
            tokenDataSource.saveTokens("access", "refresh")
            val gate = CompletableDeferred<Unit>()
            tokenApi.response = {
                gate.await()
                BaseResponse(ok = true, data = RefreshDto("access-2", "refresh-2", 1800))
            }
            val rotation = async { repository.rotateToken() }
            runCurrent()

            repository.logout().getOrThrow()
            gate.complete(Unit)
            val result = rotation.await()

            assertTrue(result.exceptionOrNull() is SessionEndedException)
            assertNull(tokenDataSource.getAccessToken())
            assertNull(tokenDataSource.getRefreshToken())
            assertFalse(repository.isLoggedIn.first())
        }

    @Test
    fun `회전 도중 세션 정리가 끝나도 회전 결과를 버린다`() =
        runTest {
            tokenDataSource.saveTokens("access", "refresh")
            val gate = CompletableDeferred<Unit>()
            tokenApi.response = {
                gate.await()
                BaseResponse(ok = true, data = RefreshDto("access-2", "refresh-2", 1800))
            }
            val rotation = async { repository.rotateToken() }
            runCurrent()

            repository.clearSession().getOrThrow()
            gate.complete(Unit)

            assertTrue(rotation.await().exceptionOrNull() is SessionEndedException)
            assertNull(tokenDataSource.getAccessToken())
        }

    @Test
    fun `지문 등록은 기기 식별자를 보내고 기기 플래그를 켠다`() =
        runTest {
            repository.registerBiometric().getOrThrow()

            assertEquals(deviceDataSource.getOrCreateDeviceId(), authApi.biometricRequests.single().deviceId)
            assertTrue(repository.isBiometricEnabled.first())
        }
}
