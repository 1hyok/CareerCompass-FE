package com.cambridge.core.network.interceptor

import com.cambridge.core.network.model.ApiException
import com.cambridge.core.network.support.LocalHttpServer
import com.cambridge.core.network.token.AccessTokenExpiryTracker
import com.cambridge.core.network.token.TokenReissuer
import com.careercompass.core.common.reporting.ErrorReporter
import com.careercompass.core.domain.error.SessionEndedException
import com.careercompass.core.domain.repository.AuthRepository
import com.careercompass.core.domain.testing.FakeAuthRepository
import com.careercompass.core.model.auth.TokenBundle
import okhttp3.OkHttpClient
import okhttp3.Request
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Test
import java.net.UnknownHostException

class AuthInterceptorTest {
    private object NoopReporter : ErrorReporter {
        override fun writeFailure(
            throwable: Throwable,
            attributes: Map<String, String>,
        ) = Unit
    }

    private val server = LocalHttpServer { LocalHttpServer.Reply(200, """{"ok":true}""") }
    private val tracker = AccessTokenExpiryTracker { 0L }

    @After
    fun tearDown() {
        server.close()
    }

    private fun client(repository: FakeAuthRepository): OkHttpClient {
        val lazy = dagger.Lazy<AuthRepository> { repository }
        return OkHttpClient
            .Builder()
            .addInterceptor(AuthInterceptor(lazy, tracker, TokenReissuer(lazy, tracker, NoopReporter)))
            .build()
    }

    private fun OkHttpClient.get(): Int =
        newCall(Request.Builder().url("${server.baseUrl}/api/v1/users/me").build()).execute().use { it.code }

    @Test
    fun `저장 토큰을 Bearer 로 붙인다`() {
        val repository = FakeAuthRepository(accessToken = "old", refreshToken = "refresh")

        assertEquals(200, client(repository).get())

        assertEquals("Bearer old", server.requests.single().authorization)
        assertEquals(0, repository.rotateTokenCalls)
    }

    @Test
    fun `토큰이 없으면 헤더 없이 보내고 남은 만료 기록을 지운다`() {
        tracker.record(expiresInSeconds = 10)

        assertEquals(200, client(FakeAuthRepository()).get())

        assertNull(server.requests.single().authorization)
        assertFalse(tracker.isExpiringSoon())
    }

    @Test
    fun `만료 임박이면 선제 회전한 새 토큰을 붙인다`() {
        tracker.record(expiresInSeconds = 10)
        val repository =
            FakeAuthRepository(accessToken = "old", refreshToken = "refresh", rotatedTokens = TokenBundle("new", "refresh-2", 3600))

        assertEquals(200, client(repository).get())

        assertEquals("Bearer new", server.requests.single().authorization)
        assertEquals(1, repository.rotateTokenCalls)
    }

    @Test
    fun `선제 재발급이 일시 실패하면 기존 토큰으로 진행한다`() {
        tracker.record(expiresInSeconds = 10)
        val repository =
            FakeAuthRepository(accessToken = "old", refreshToken = "refresh").apply {
                onRotateToken = { Result.failure(UnknownHostException("dns")) }
            }

        assertEquals(200, client(repository).get())

        assertEquals("Bearer old", server.requests.single().authorization)
        assertEquals(0, repository.clearSessionCalls)
    }

    @Test
    fun `선제 재발급이 확정 거절되면 죽은 토큰으로 보내지 않고 실패시킨다`() {
        tracker.record(expiresInSeconds = 10)
        val repository =
            FakeAuthRepository(accessToken = "old", refreshToken = "refresh").apply {
                onRotateToken = { Result.failure(ApiException("AUTH_INVALID", null, "거절", status = 401)) }
            }

        assertThrows(TokenReissueFailureException::class.java) { client(repository).get() }

        assertEquals(emptyList<LocalHttpServer.Recorded>(), server.requests)
        assertEquals(1, repository.clearSessionCalls)
    }

    @Test
    fun `세션이 교체됐으면 요청을 보내지 않고 실패시킨다`() {
        tracker.record(expiresInSeconds = 10)
        val repository =
            FakeAuthRepository(accessToken = "old", refreshToken = "refresh").apply {
                onRotateToken = { Result.failure(SessionEndedException("로그아웃")) }
            }

        assertThrows(TokenReissueFailureException::class.java) { client(repository).get() }

        assertEquals(emptyList<LocalHttpServer.Recorded>(), server.requests)
        assertEquals(0, repository.clearSessionCalls)
    }
}
