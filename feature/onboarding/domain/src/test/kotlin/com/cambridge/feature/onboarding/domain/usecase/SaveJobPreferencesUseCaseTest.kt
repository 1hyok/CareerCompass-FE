package com.cambridge.feature.onboarding.domain.usecase

import com.cambridge.core.domain.testing.FakeUserProfileRepository
import com.cambridge.core.model.user.JobInterest
import com.cambridge.feature.onboarding.domain.model.OnboardingStep
import com.cambridge.feature.onboarding.domain.testing.FakeOnboardingProgressRepository
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException

class SaveJobPreferencesUseCaseTest {
    private val userProfileRepository = FakeUserProfileRepository(initialProfile = sampleProfile())
    private val progressRepository = FakeOnboardingProgressRepository()
    private val useCase = SaveJobPreferencesUseCase(userProfileRepository, progressRepository)

    @Test
    fun `직무는 순서대로 우선순위를 매기고 태그를 교체한 뒤 Experience 로 옮긴다`() =
        runTest {
            val result = useCase(jobCodes = listOf("frontend", "backend"), tags = listOf("AI", "스타트업"))

            assertTrue(result.isSuccess)
            assertEquals(
                listOf(listOf(JobInterest("frontend", 1), JobInterest("backend", 2))),
                userProfileRepository.replacedJobInterests,
            )
            assertEquals(listOf(listOf("AI", "스타트업")), userProfileRepository.replacedTags)
            assertEquals(listOf(OnboardingStep.Experience), progressRepository.savedSteps)
        }

    @Test
    fun `직무 교체가 실패하면 태그는 보내지 않는다`() =
        runTest {
            val failure = IOException("offline")
            userProfileRepository.onReplaceJobInterests = { Result.failure(failure) }

            val result = useCase(jobCodes = listOf("backend"), tags = listOf("AI"))

            assertSame(failure, result.exceptionOrNull())
            assertTrue(userProfileRepository.replacedTags.isEmpty())
            assertTrue(progressRepository.savedSteps.isEmpty())
        }

    @Test
    fun `태그 교체가 실패하면 진행 상태를 바꾸지 않는다`() =
        runTest {
            val failure = IOException("offline")
            userProfileRepository.onReplaceTags = { Result.failure(failure) }

            val result = useCase(jobCodes = listOf("backend"), tags = listOf("AI"))

            assertSame(failure, result.exceptionOrNull())
            assertTrue(progressRepository.savedSteps.isEmpty())
        }

    @Test
    fun `개수 상한과 목록 밖 코드는 요청 전에 거부한다`() {
        assertThrows(IllegalArgumentException::class.java) {
            kotlinx.coroutines.runBlocking { useCase(jobCodes = emptyList(), tags = listOf("AI")) }
        }
        assertThrows(IllegalArgumentException::class.java) {
            kotlinx.coroutines.runBlocking { useCase(jobCodes = listOf("backend", "frontend", "mobile", "qa"), tags = listOf("AI")) }
        }
        assertThrows(IllegalArgumentException::class.java) {
            kotlinx.coroutines.runBlocking { useCase(jobCodes = listOf("unknown"), tags = listOf("AI")) }
        }
        assertThrows(IllegalArgumentException::class.java) {
            kotlinx.coroutines.runBlocking { useCase(jobCodes = listOf("backend"), tags = List(6) { "tag$it" }) }
        }
        assertThrows(IllegalArgumentException::class.java) {
            kotlinx.coroutines.runBlocking { useCase(jobCodes = listOf("backend"), tags = listOf("AI", "AI")) }
        }
        assertTrue(userProfileRepository.replacedJobInterests.isEmpty())
    }
}
