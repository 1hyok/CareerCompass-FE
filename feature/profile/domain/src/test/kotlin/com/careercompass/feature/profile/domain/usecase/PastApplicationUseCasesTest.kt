package com.careercompass.feature.profile.domain.usecase

import com.careercompass.core.domain.error.CoreDataFailure
import com.careercompass.core.domain.testing.FakePastApplicationRepository
import com.careercompass.core.model.application.MAX_PAST_APPLICATIONS
import com.careercompass.core.model.application.PastApplicationCategory
import com.careercompass.feature.profile.domain.ServerFailure
import com.careercompass.feature.profile.domain.error.ProfileFailure
import com.careercompass.feature.profile.domain.pastApplication
import com.careercompass.feature.profile.domain.uploadFile
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** API_SPEC v0.1 §4 `/past-applications` 를 덮는 use case 들. */
class PastApplicationUseCasesTest {
    @Test
    fun `목록은 항목까지 함께 돌려준다`() =
        runTest {
            val repository = FakePastApplicationRepository(initial = listOf(pastApplication(1L), pastApplication(2L)))

            val applications = GetPastApplicationsUseCase(repository)().getOrThrow()

            assertEquals(listOf(1L, 2L), applications.map { it.id })
            assertEquals(
                PastApplicationCategory.Motivation,
                applications
                    .first()
                    .items
                    .single()
                    .category,
            )
        }

    @Test
    fun `상한 아래면 라벨을 다듬어 올린다`() =
        runTest {
            val repository = FakePastApplicationRepository(initial = List(MAX_PAST_APPLICATIONS - 1) { pastApplication(it + 1L) })

            val uploaded = UploadPastApplicationUseCase(repository)(uploadFile(), "  2024 카카오 인턴 자소서  ").getOrThrow()

            assertEquals("2024 카카오 인턴 자소서", uploaded.label)
            assertEquals("2024 카카오 인턴 자소서", repository.uploads.single().second)
        }

    @Test
    fun `지원서가 10개면 파일을 보내기 전에 PastApplicationLimitReached 로 거절한다`() =
        runTest {
            val repository = FakePastApplicationRepository(initial = List(MAX_PAST_APPLICATIONS) { pastApplication(it + 1L) })

            val failure = UploadPastApplicationUseCase(repository)(uploadFile(), "새 지원서").exceptionOrNull()

            assertTrue(failure is ProfileFailure.PastApplicationLimitReached)
            assertEquals(MAX_PAST_APPLICATIONS, (failure as ProfileFailure.PastApplicationLimitReached).limit)
            assertTrue(repository.uploads.isEmpty())
        }

    @Test
    fun `목록 조회가 실패하면 올리지 않고 그 실패를 돌려준다`() =
        runTest {
            val repository =
                FakePastApplicationRepository.strict().apply {
                    onGetPastApplications = { ServerFailure.ServiceUnavailable.asResult() }
                }

            val failure = UploadPastApplicationUseCase(repository)(uploadFile(), "새 지원서").exceptionOrNull()

            assertTrue(failure is CoreDataFailure.ServiceUnavailable)
            assertTrue(repository.uploads.isEmpty())
        }

    @Test
    fun `빈 라벨은 목록 조회 전에 거절한다`() =
        runTest {
            val repository = FakePastApplicationRepository.strict()

            assertTrue(runCatching { UploadPastApplicationUseCase(repository)(uploadFile(), "   ") }.isFailure)
            assertTrue(repository.uploads.isEmpty())
        }

    @Test
    fun `항목 분류 조정은 서버가 돌려준 항목을 그대로 쓴다`() =
        runTest {
            val repository = FakePastApplicationRepository(initial = listOf(pastApplication(1L)))

            val updated =
                UpdatePastApplicationItemCategoryUseCase(repository)(
                    applicationId = 1L,
                    itemId = 10L,
                    category = PastApplicationCategory.Aspiration,
                ).getOrThrow()

            assertEquals(PastApplicationCategory.Aspiration, updated.category)
            assertEquals(
                PastApplicationCategory.Aspiration,
                repository.applications
                    .single()
                    .items
                    .single()
                    .category,
            )
        }

    @Test
    fun `삭제는 그 지원서만 지운다`() =
        runTest {
            val repository = FakePastApplicationRepository(initial = listOf(pastApplication(1L), pastApplication(2L)))

            DeletePastApplicationUseCase(repository)(1L).getOrThrow()

            assertEquals(listOf(2L), repository.applications.map { it.id })
        }
}
