package com.cambridge.core.domain.usecase.auth

import com.cambridge.core.domain.testing.FakeAuthRepository
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class LogoutUseCaseTest {
    @Test
    fun `로그아웃은 리포지토리 로그아웃을 한 번 호출한다`() =
        runTest {
            val repository = FakeAuthRepository(loggedIn = true, accessToken = "access", refreshToken = "refresh")

            val result = LogoutUseCase(repository)()

            assertEquals(Result.success(Unit), result)
            assertEquals(1, repository.logoutCalls)
            assertFalse(repository.loggedIn)
        }
}
