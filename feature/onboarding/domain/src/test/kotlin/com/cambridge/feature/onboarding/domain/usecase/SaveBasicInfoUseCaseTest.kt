package com.cambridge.feature.onboarding.domain.usecase

import com.cambridge.feature.onboarding.domain.model.OnboardingStep
import com.cambridge.feature.onboarding.domain.testing.FakeOnboardingProgressRepository
import com.careercompass.core.domain.testing.FakeUserProfileRepository
import com.careercompass.core.model.user.UserProfileUpdate
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException

class SaveBasicInfoUseCaseTest {
    private val userProfileRepository = FakeUserProfileRepository(initialProfile = sampleProfile())
    private val progressRepository = FakeOnboardingProgressRepository()
    private val useCase = SaveBasicInfoUseCase(userProfileRepository, progressRepository)

    @Test
    fun `프로필을 수정한 뒤 진행 상태를 JobPreference 로 옮긴다`() =
        runTest {
            val result = useCase(name = " 정일혁 ", school = "건국대학교", department = "컴퓨터공학부", gpa = 3.87, gradYear = 2027)

            assertTrue(result.isSuccess)
            assertEquals(
                listOf(UserProfileUpdate(name = "정일혁", school = "건국대학교", department = "컴퓨터공학부", gpa = 3.87, gradYear = 2027)),
                userProfileRepository.updates,
            )
            assertEquals(listOf(OnboardingStep.JobPreference), progressRepository.savedSteps)
        }

    @Test
    fun `선택 값은 null 로 보낼 수 있다`() =
        runTest {
            useCase(name = "정일혁", school = "건국대학교", department = "컴퓨터공학부", gpa = null, gradYear = null)

            val update = userProfileRepository.updates.single()
            assertEquals(null, update.gpa)
            assertEquals(null, update.gradYear)
        }

    @Test
    fun `프로필 수정이 실패하면 진행 상태를 바꾸지 않고 실패를 돌려준다`() =
        runTest {
            val failure = IOException("offline")
            userProfileRepository.onUpdateProfile = { Result.failure(failure) }

            val result = useCase(name = "정일혁", school = "건국대학교", department = "컴퓨터공학부", gpa = null, gradYear = null)

            assertSame(failure, result.exceptionOrNull())
            assertTrue(progressRepository.savedSteps.isEmpty())
        }

    @Test
    fun `진행 상태 저장 실패도 실패로 전파한다`() =
        runTest {
            val failure = IllegalStateException("disk")
            progressRepository.onSave = { Result.failure(failure) }

            val result = useCase(name = "정일혁", school = "건국대학교", department = "컴퓨터공학부", gpa = null, gradYear = null)

            assertSame(failure, result.exceptionOrNull())
        }

    @Test
    fun `필수 값이 비면 요청 전에 거부한다`() {
        assertThrows(IllegalArgumentException::class.java) {
            kotlinx.coroutines.runBlocking { useCase(name = " ", school = "건국대학교", department = "컴퓨터공학부", gpa = null, gradYear = null) }
        }
        assertThrows(IllegalArgumentException::class.java) {
            kotlinx.coroutines.runBlocking { useCase(name = "정일혁", school = "건국대학교", department = "컴퓨터공학부", gpa = 4.6, gradYear = null) }
        }
        assertTrue(userProfileRepository.updates.isEmpty())
    }
}
