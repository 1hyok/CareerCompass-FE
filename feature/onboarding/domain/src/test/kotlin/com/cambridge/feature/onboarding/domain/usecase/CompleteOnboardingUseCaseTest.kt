package com.cambridge.feature.onboarding.domain.usecase

import com.cambridge.core.domain.testing.FakeUserProfileRepository
import com.cambridge.feature.onboarding.domain.model.OnboardingProgress
import com.cambridge.feature.onboarding.domain.testing.FakeOnboardingProgressRepository
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException

class CompleteOnboardingUseCaseTest {
    @Test
    fun `완료 기록 후 프로필을 다시 읽는다`() =
        runTest {
            var refreshed = 0
            val profile = sampleProfile(onboardingDone = true)
            val userProfileRepository =
                FakeUserProfileRepository(
                    onRefreshProfile = {
                        refreshed += 1
                        Result.success(profile)
                    },
                )
            val progressRepository = FakeOnboardingProgressRepository()

            val result = CompleteOnboardingUseCase(progressRepository, userProfileRepository)()

            assertTrue(result.isSuccess)
            assertEquals(OnboardingProgress.Completed, progressRepository.progressState.value)
            assertEquals(1, refreshed)
        }

    @Test
    fun `프로필 갱신 실패는 완료를 막지 않는다`() =
        runTest {
            val userProfileRepository = FakeUserProfileRepository(onRefreshProfile = { Result.failure(IOException("offline")) })
            val progressRepository = FakeOnboardingProgressRepository()

            assertTrue(CompleteOnboardingUseCase(progressRepository, userProfileRepository)().isSuccess)
            assertEquals(1, progressRepository.markCompletedCalls)
        }

    @Test
    fun `완료 기록 실패는 프로필을 읽지 않고 실패로 전파한다`() =
        runTest {
            val failure = IllegalStateException("disk")
            val userProfileRepository = FakeUserProfileRepository.strict()
            val progressRepository = FakeOnboardingProgressRepository(onMarkCompleted = { Result.failure(failure) })

            assertSame(failure, CompleteOnboardingUseCase(progressRepository, userProfileRepository)().exceptionOrNull())
        }
}
