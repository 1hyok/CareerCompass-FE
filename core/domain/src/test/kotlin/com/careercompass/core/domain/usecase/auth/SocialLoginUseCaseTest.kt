package com.careercompass.core.domain.usecase.auth

import com.careercompass.core.domain.error.CoreAuthFailure
import com.careercompass.core.domain.testing.FakeAuthRepository
import com.careercompass.core.model.auth.Session
import com.careercompass.core.model.auth.SocialProvider
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import java.net.UnknownHostException

class SocialLoginUseCaseTest {
    @Test
    fun `로그인 성공 시 세션을 저장하고 신규 여부를 돌려준다`() =
        runTest {
            val repository =
                FakeAuthRepository(
                    session = Session("access", "refresh", isNewUser = true, expiresInSeconds = 3600),
                )

            val outcome = SocialLoginUseCase(repository)(SocialProvider.Kakao, "kakao-token")

            assertEquals(Result.success(SocialLoginOutcome(isNewUser = true)), outcome)
            assertEquals(1, repository.savedSessions.size)
            assertEquals(null, repository.socialLoginCalls.single().fcmToken)
            assertTrue(repository.loggedIn)
        }

    @Test
    fun `서버 로그인이 실패하면 세션을 저장하지 않고 실패를 그대로 돌려준다`() =
        runTest {
            val repository =
                FakeAuthRepository.strict().apply {
                    onSocialLogin = { _, _, _ -> Result.failure(CoreAuthFailure.NetworkUnavailable(UnknownHostException())) }
                }

            val outcome = SocialLoginUseCase(repository)(SocialProvider.Google, "google-id-token")

            assertTrue(outcome.exceptionOrNull() is CoreAuthFailure.NetworkUnavailable)
            assertTrue(repository.savedSessions.isEmpty())
        }

    @Test
    fun `빈 제공자 토큰은 요청 전에 거부한다`() =
        runTest {
            val useCase = SocialLoginUseCase(FakeAuthRepository.strict())

            assertThrows(IllegalArgumentException::class.java) {
                kotlinx.coroutines.runBlocking { useCase(SocialProvider.Kakao, " ") }
            }
        }
}
