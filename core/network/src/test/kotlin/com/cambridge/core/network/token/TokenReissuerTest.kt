package com.cambridge.core.network.token

import com.cambridge.core.common.reporting.ErrorReporter
import com.cambridge.core.domain.repository.AuthRepository
import com.cambridge.core.domain.testing.FakeAuthRepository
import com.cambridge.core.model.auth.TokenBundle
import com.cambridge.core.network.model.ApiException
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
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

    @Test
    fun `회전 성공 시 새 토큰과 만료 기록을 남긴다`() {
        val repository =
            FakeAuthRepository(
                accessToken = "old",
                refreshToken = "refresh",
                rotatedTokens = TokenBundle("new", "refresh-2", expiresInSeconds = 3600),
            )

        val outcome = reissuer(repository).reissue(expectedAccessToken = "old")

        assertEquals(TokenReissuer.Outcome.Rotated("new"), outcome)
        assertEquals(1, repository.rotateTokenCalls)
        assertEquals("new", repository.accessToken)
    }

    @Test
    fun `다른 경로가 먼저 회전했으면 재발급 없이 현재 토큰을 돌려준다`() {
        val repository = FakeAuthRepository(accessToken = "already-new", refreshToken = "refresh")

        val outcome = reissuer(repository).reissue(expectedAccessToken = "old")

        assertEquals(TokenReissuer.Outcome.TokenAlreadyChanged("already-new"), outcome)
        assertEquals(0, repository.rotateTokenCalls)
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

        val first = reissuer.reissue(expectedAccessToken = "old")
        val second = reissuer.reissue(expectedAccessToken = "old")

        assertTrue(first is TokenReissuer.Outcome.AuthenticationRejected)
        assertEquals(first, second)
        assertEquals(1, repository.rotateTokenCalls)
        assertEquals(1, repository.clearSessionCalls)
        assertTrue(reporter.attributes.isEmpty())
    }

    @Test
    fun `전송 실패는 세션을 유지하고 계측한다`() {
        val repository =
            FakeAuthRepository(accessToken = "old", refreshToken = "refresh").apply {
                onRotateToken = { Result.failure(UnknownHostException("dns")) }
            }

        val outcome = reissuer(repository).reissue(expectedAccessToken = "old")

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

        assertTrue(reissuer(serverFailure).reissue("old") is TokenReissuer.Outcome.ServerFailure)
        assertTrue(reissuer(rejected).reissue("old") is TokenReissuer.Outcome.AuthenticationRejected)
    }
}
