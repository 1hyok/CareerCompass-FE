package com.cambridge.feature.onboarding.domain.usecase

import com.cambridge.core.domain.testing.FakeUserProfileRepository
import com.cambridge.feature.onboarding.domain.model.OnboardingProgress
import com.cambridge.feature.onboarding.domain.model.OnboardingStep
import com.cambridge.feature.onboarding.domain.testing.FakeOnboardingProgressRepository
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertThrows
import org.junit.Test
import java.io.IOException

class ResolveOnboardingEntryUseCaseTest {
    @Test
    fun `서버가 onboardingDone 이면 로컬 기록과 무관하게 완료다`() =
        runTest {
            val profile = sampleProfile(onboardingDone = true)
            val useCase =
                ResolveOnboardingEntryUseCase(
                    userProfileRepository = FakeUserProfileRepository(initialProfile = profile),
                    progressRepository = FakeOnboardingProgressRepository.strict(OnboardingProgress.InProgress(OnboardingStep.Experience)),
                )

            val entry = useCase()

            assertEquals(OnboardingProgress.Completed, entry.progress)
            assertEquals(profile, entry.profile)
            assertNull(entry.profileRefreshFailure)
        }

    @Test
    fun `기록이 없으면 첫 단계부터 시작한다`() =
        runTest {
            val useCase =
                ResolveOnboardingEntryUseCase(
                    userProfileRepository = FakeUserProfileRepository(initialProfile = sampleProfile()),
                    progressRepository = FakeOnboardingProgressRepository(),
                )

            assertEquals(OnboardingProgress.InProgress(OnboardingStep.BasicInfo), useCase().progress)
        }

    @Test
    fun `저장된 단계가 있으면 그 단계부터 재개한다`() =
        runTest {
            val useCase =
                ResolveOnboardingEntryUseCase(
                    userProfileRepository = FakeUserProfileRepository(initialProfile = sampleProfile()),
                    progressRepository = FakeOnboardingProgressRepository(OnboardingProgress.InProgress(OnboardingStep.PastApplication)),
                )

            assertEquals(OnboardingProgress.InProgress(OnboardingStep.PastApplication), useCase().progress)
        }

    @Test
    fun `로컬이 완료면 서버 플래그가 아직 false 여도 완료로 본다`() =
        runTest {
            val useCase =
                ResolveOnboardingEntryUseCase(
                    userProfileRepository = FakeUserProfileRepository(initialProfile = sampleProfile(onboardingDone = false)),
                    progressRepository = FakeOnboardingProgressRepository(OnboardingProgress.Completed),
                )

            assertEquals(OnboardingProgress.Completed, useCase().progress)
        }

    @Test
    fun `프로필 갱신 실패는 진입을 막지 않고 캐시와 실패 원인을 함께 돌려준다`() =
        runTest {
            val cached = sampleProfile()
            val failure = IOException("offline")
            val userProfileRepository =
                FakeUserProfileRepository(initialProfile = cached, onRefreshProfile = { Result.failure(failure) })
            val useCase =
                ResolveOnboardingEntryUseCase(
                    userProfileRepository = userProfileRepository,
                    progressRepository = FakeOnboardingProgressRepository(OnboardingProgress.InProgress(OnboardingStep.JobPreference)),
                )

            val entry = useCase()

            assertEquals(OnboardingProgress.InProgress(OnboardingStep.JobPreference), entry.progress)
            assertEquals(cached, entry.profile)
            assertSame(failure, entry.profileRefreshFailure)
        }

    @Test
    fun `캐시도 없으면 프로필 없이 첫 단계부터 시작한다`() =
        runTest {
            val useCase =
                ResolveOnboardingEntryUseCase(
                    userProfileRepository = FakeUserProfileRepository(onRefreshProfile = { Result.failure(IOException("offline")) }),
                    progressRepository = FakeOnboardingProgressRepository(),
                )

            val entry = useCase()

            assertEquals(OnboardingProgress.InProgress(OnboardingStep.BasicInfo), entry.progress)
            assertNull(entry.profile)
        }

    @Test
    fun `진입 결과는 NotStarted 를 허용하지 않는다`() {
        assertThrows(IllegalArgumentException::class.java) {
            OnboardingEntry(OnboardingProgress.NotStarted, null, null)
        }
    }
}
