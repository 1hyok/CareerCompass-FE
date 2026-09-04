package com.careercompass.core.domain.usecase.auth

import com.careercompass.core.domain.error.CoreDataFailure
import com.careercompass.core.domain.testing.FakeAuthRepository
import com.careercompass.core.domain.testing.FakeUserProfileRepository
import com.careercompass.core.model.user.UserProfile
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test
import java.net.UnknownHostException

class ResolveSessionEntryUseCaseTest {
    private val authRepository = FakeAuthRepository(loggedIn = true, accessToken = "access", refreshToken = "refresh")

    private fun useCase(profiles: FakeUserProfileRepository) = ResolveSessionEntryUseCase(authRepository, profiles)

    @Test
    fun `프로필 조회가 성공하면 온보딩 완료 여부로 가른다`() =
        runTest {
            val notDone = useCase(FakeUserProfileRepository(profile(onboardingDone = false)))()
            val done = useCase(FakeUserProfileRepository(profile(onboardingDone = true)))()

            assertEquals(SessionEntry(SessionEntryDestination.Onboarding), notDone)
            assertEquals(SessionEntry(SessionEntryDestination.Feed), done)
            assertTrue(authRepository.loggedIn)
        }

    @Test
    fun `401 이면 로컬 세션을 정리하고 로그인으로 보낸다`() =
        runTest {
            val profiles =
                FakeUserProfileRepository().apply {
                    onRefreshProfile = { Result.failure(CoreDataFailure.Unauthorized("AUTH_INVALID", IllegalStateException("만료"))) }
                }

            val entry = useCase(profiles)()

            assertEquals(SessionEntry(SessionEntryDestination.Login), entry)
            assertEquals(1, authRepository.clearSessionCalls)
            assertFalse(authRepository.loggedIn)
        }

    @Test
    fun `그 밖의 실패는 마지막으로 알려진 완료 여부로 판단하고 실패를 실어 보낸다`() =
        runTest {
            val cause = CoreDataFailure.NetworkUnavailable(UnknownHostException("offline"))
            val cachedNotDone =
                FakeUserProfileRepository(profile(onboardingDone = false)).apply { onRefreshProfile = { Result.failure(cause) } }
            val hintDone =
                FakeUserProfileRepository().apply {
                    onRefreshProfile = { Result.failure(cause) }
                    onboardingDoneHint = true
                }

            val fromCache = useCase(cachedNotDone)()
            val fromHint = useCase(hintDone)()

            assertEquals(SessionEntryDestination.Onboarding, fromCache.destination)
            assertSame(cause, fromCache.fallbackCause)
            assertEquals(SessionEntryDestination.Feed, fromHint.destination)
            assertSame(cause, fromHint.fallbackCause)
            assertEquals(0, authRepository.clearSessionCalls)
        }

    @Test
    fun `완료 여부를 전혀 모르면 피드가 아니라 온보딩이다`() =
        runTest {
            val unknown =
                FakeUserProfileRepository().apply {
                    onRefreshProfile = { Result.failure(CoreDataFailure.NetworkUnavailable(UnknownHostException("offline"))) }
                }

            val entry = useCase(unknown)()

            assertEquals(SessionEntryDestination.Onboarding, entry.destination)
            assertNull(unknown.lastKnownOnboardingDone())
        }

    private fun profile(onboardingDone: Boolean) =
        UserProfile(
            id = 1L,
            name = "정일혁",
            school = null,
            department = null,
            gpa = null,
            gradYear = null,
            jobInterests = emptyList(),
            tags = emptyList(),
            onboardingDone = onboardingDone,
            completion = 10,
        )
}
