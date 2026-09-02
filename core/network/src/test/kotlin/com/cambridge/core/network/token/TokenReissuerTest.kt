package com.cambridge.core.network.token

import com.cambridge.core.common.reporting.ErrorReporter
import com.cambridge.core.domain.error.SessionEndedException
import com.cambridge.core.domain.repository.AuthRepository
import com.cambridge.core.domain.testing.FakeAuthRepository
import com.cambridge.core.model.auth.TokenBundle
import com.cambridge.core.network.model.ApiException
import com.cambridge.core.network.token.TokenReissuer.Trigger
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException
import java.net.UnknownHostException

class TokenReissuerTest {
    private class RecordingReporter : ErrorReporter {
        val attributes = mutableListOf<Map<String, String>>()

        override fun writeFailure(
            throwable: Throwable,
            attributes: Map<String, String>,
        ) {
            this.attributes += attributes
        }
    }

    private val reporter = RecordingReporter()
    private val tracker = AccessTokenExpiryTracker { 0L }

    private fun reissuer(repository: AuthRepository) =
        TokenReissuer(
            authRepository = dagger.Lazy { repository },
            expiryTracker = tracker,
            errorReporter = reporter,
        )

    private fun rotatingRepository() =
        FakeAuthRepository(
            accessToken = "old",
            refreshToken = "refresh",
            rotatedTokens = TokenBundle("new", "refresh-2", expiresInSeconds = 3600),
        )

    @Test
    fun `회전 성공 시 새 토큰과 만료 기록을 남긴다`() {
        val repository = rotatingRepository()

        val outcome = reissuer(repository).reissue(expectedAccessToken = "old", trigger = Trigger.Unauthorized)

        assertEquals(TokenReissuer.Outcome.Rotated("new"), outcome)
        assertEquals(1, repository.rotateTokenCalls)
        assertEquals("new", repository.accessToken)
        assertTrue(!tracker.isExpiringSoon())
    }

    @Test
    fun `직전 회전 쌍과 맞는 옛 토큰의 재요청은 재발급 없이 새 토큰을 돌려준다`() {
        val repository = rotatingRepository()
        val reissuer = reissuer(repository)
        reissuer.reissue(expectedAccessToken = "old", trigger = Trigger.Unauthorized)

        val outcome = reissuer.reissue(expectedAccessToken = "old", trigger = Trigger.Unauthorized)

        assertEquals(TokenReissuer.Outcome.TokenAlreadyChanged("new"), outcome)
        assertEquals(1, repository.rotateTokenCalls)
    }

    @Test
    fun `회전 밖에서 교체된 토큰은 세션 교체로 돌리고 세션을 건드리지 않는다`() {
        // 로그아웃 뒤 다른 계정으로 로그인한 상태에서 이전 세션의 요청이 401 을 받은 경우.
        val repository = FakeAuthRepository(accessToken = "other-account", refreshToken = "refresh")

        val outcome = reissuer(repository).reissue(expectedAccessToken = "old", trigger = Trigger.Unauthorized)

        assertTrue(outcome is TokenReissuer.Outcome.SessionChanged)
        assertEquals(0, repository.rotateTokenCalls)
        assertEquals(0, repository.clearSessionCalls)
        assertEquals("other-account", repository.accessToken)
    }

    @Test
    fun `세션이 비어 있으면 재발급하지 않고 세션 교체로 돌린다`() {
        val repository = FakeAuthRepository(accessToken = null, refreshToken = null)

        val outcome = reissuer(repository).reissue(expectedAccessToken = "old", trigger = Trigger.Unauthorized)

        assertTrue(outcome is TokenReissuer.Outcome.SessionChanged)
        assertEquals(0, repository.rotateTokenCalls)
        assertTrue(reporter.attributes.isEmpty())
    }

    @Test
    fun `선제 경로는 락 안에서 만료 임박이 아니면 회전하지 않는다`() {
        // 호출자가 새 토큰과 옛 만료 시각을 따로 읽은 경우 — 직전 회전이 만료 시각까지 갱신해 둔 상태.
        tracker.record(expiresInSeconds = 3600)
        val repository = rotatingRepository()

        val outcome = reissuer(repository).reissue(expectedAccessToken = "old", trigger = Trigger.Preemptive)

        assertEquals(TokenReissuer.Outcome.TokenAlreadyChanged("old"), outcome)
        assertEquals(0, repository.rotateTokenCalls)
    }

    @Test
    fun `401 경로는 만료 임박이 아니어도 회전한다`() {
        tracker.record(expiresInSeconds = 3600)
        val repository = rotatingRepository()

        val outcome = reissuer(repository).reissue(expectedAccessToken = "old", trigger = Trigger.Unauthorized)

        assertEquals(TokenReissuer.Outcome.Rotated("new"), outcome)
        assertEquals(1, repository.rotateTokenCalls)
    }

    @Test
    fun `회전 도중 세션이 끝나면 세션 교체로 돌리고 세션을 다시 지우지 않는다`() {
        val repository =
            FakeAuthRepository(accessToken = "old", refreshToken = "refresh").apply {
                onRotateToken = { Result.failure(SessionEndedException("로그아웃이 끼어듦")) }
            }

        val outcome = reissuer(repository).reissue(expectedAccessToken = "old", trigger = Trigger.Unauthorized)

        assertTrue(outcome is TokenReissuer.Outcome.SessionChanged)
        assertEquals(0, repository.clearSessionCalls)
        assertTrue(reporter.attributes.isEmpty())
    }

    @Test
    fun `refresh 거절이면 세션을 정리하고 같은 토큰의 재요청에 같은 결과를 준다`() {
        val repository =
            FakeAuthRepository(accessToken = "old", refreshToken = "refresh").apply {
                onRotateToken = {
                    Result.failure(ApiException(code = "AUTH_INVALID", serverMessage = null, fallbackMessage = "거절", status = 401))
                }
            }
        val reissuer = reissuer(repository)

        val first = reissuer.reissue(expectedAccessToken = "old", trigger = Trigger.Unauthorized)
        val second = reissuer.reissue(expectedAccessToken = "old", trigger = Trigger.Preemptive)

        assertTrue(first is TokenReissuer.Outcome.AuthenticationRejected)
        assertEquals(first, second)
        assertEquals(1, repository.rotateTokenCalls)
        assertEquals(1, repository.clearSessionCalls)
        assertTrue(reporter.attributes.isEmpty())
    }

    @Test
    fun `세션 정리가 실패해도 거절은 캐시하고 실패를 계측한다`() {
        val repository =
            FakeAuthRepository(accessToken = "old", refreshToken = "refresh").apply {
                onRotateToken = { Result.failure(ApiException("AUTH_INVALID", null, "거절", status = 401)) }
                onClearSession = { Result.failure(IOException("disk")) }
            }
        val reissuer = reissuer(repository)

        val first = reissuer.reissue(expectedAccessToken = "old", trigger = Trigger.Unauthorized)
        val second = reissuer.reissue(expectedAccessToken = "old", trigger = Trigger.Unauthorized)

        assertTrue(first is TokenReissuer.Outcome.AuthenticationRejected)
        assertEquals(first, second)
        assertEquals(1, repository.rotateTokenCalls)
        assertEquals("session_clear", reporter.attributes.single()["auth_stage"])
    }

    @Test
    fun `전송 실패는 세션을 유지하고 계측한다`() {
        val repository =
            FakeAuthRepository(accessToken = "old", refreshToken = "refresh").apply {
                onRotateToken = { Result.failure(UnknownHostException("dns")) }
            }

        val outcome = reissuer(repository).reissue(expectedAccessToken = "old", trigger = Trigger.Unauthorized)

        assertTrue(outcome is TokenReissuer.Outcome.TransportFailure)
        assertEquals(0, repository.clearSessionCalls)
        assertEquals("transport", reporter.attributes.single()["failure_kind"])
    }

    @Test
    fun `5xx 는 서버 장애로 분류하고 400 은 거절로 확정한다`() {
        val serverFailure =
            FakeAuthRepository(accessToken = "old", refreshToken = "refresh").apply {
                onRotateToken = { Result.failure(ApiException("INTERNAL_ERROR", null, "장애", status = 503)) }
            }
        val rejected =
            FakeAuthRepository(accessToken = "old", refreshToken = "refresh").apply {
                onRotateToken = { Result.failure(ApiException("HTTP_400", null, "거절", status = 400)) }
            }

        assertTrue(reissuer(serverFailure).reissue("old", Trigger.Unauthorized) is TokenReissuer.Outcome.ServerFailure)
        assertTrue(reissuer(rejected).reissue("old", Trigger.Unauthorized) is TokenReissuer.Outcome.AuthenticationRejected)
    }
}
