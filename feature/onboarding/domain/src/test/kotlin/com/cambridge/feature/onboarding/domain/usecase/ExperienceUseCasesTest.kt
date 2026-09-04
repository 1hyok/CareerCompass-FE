package com.cambridge.feature.onboarding.domain.usecase

import com.cambridge.core.domain.testing.FakeExperienceRepository
import com.cambridge.core.model.experience.Experience
import com.cambridge.core.model.experience.ExperienceDetails
import com.cambridge.core.model.experience.ExperienceDraft
import com.cambridge.core.model.experience.ExperiencePoint
import com.cambridge.core.model.experience.MAX_EXPERIENCE_CARDS
import com.cambridge.core.model.paging.CursorPage
import com.cambridge.feature.onboarding.domain.model.OnboardingStep
import com.cambridge.feature.onboarding.domain.testing.FakeOnboardingProgressRepository
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException
import java.time.LocalDate

class ExperienceUseCasesTest {
    private val existing =
        Experience(
            id = 7L,
            title = "CareerCompass",
            startPoint = ExperiencePoint.Date(LocalDate.of(2025, 9, 1)),
            endPoint = null,
            details = ExperienceDetails.Project(role = "안드로이드", techs = listOf("Kotlin"), summary = null, link = null),
            createdAt = null,
        )

    @Test
    fun `경험 목록은 유형 필터 없이 상한 개수로 한 번에 읽는다`() =
        runTest {
            var requested: Triple<Any?, String?, Int>? = null
            val repository =
                FakeExperienceRepository(
                    onGetExperiences = { type, cursor, limit ->
                        requested = Triple(type, cursor, limit)
                        Result.success(CursorPage(items = listOf(existing), nextCursor = null))
                    },
                )

            val result = GetOnboardingExperiencesUseCase(repository)()

            assertEquals(listOf(existing), result.getOrThrow())
            assertEquals(Triple(null, null, MAX_EXPERIENCE_CARDS), requested)
        }

    @Test
    fun `경험 추가는 저장소 결과를 그대로 돌려준다`() =
        runTest {
            val repository = FakeExperienceRepository()
            val draft =
                ExperienceDraft(
                    title = "수상",
                    startPoint = ExperiencePoint.Year(2025),
                    endPoint = null,
                    details = ExperienceDetails.Award(contestName = "수상", rank = "대상", organizer = null),
                )

            val created = AddExperienceUseCase(repository)(draft).getOrThrow()

            assertEquals("수상", created.title)
            assertEquals(listOf(draft), repository.createdDrafts)
        }

    @Test
    fun `경험 추가 실패는 그대로 전파한다`() =
        runTest {
            val failure = IOException("offline")
            val repository = FakeExperienceRepository(onCreateExperience = { Result.failure(failure) })
            val draft =
                ExperienceDraft(
                    title = "인턴",
                    startPoint = ExperiencePoint.YearMonth(2025, 1),
                    endPoint = null,
                    details = ExperienceDetails.Intern(company = "카카오", role = "개발", summary = null),
                )

            assertSame(failure, AddExperienceUseCase(repository)(draft).exceptionOrNull())
        }

    @Test
    fun `경험 수정은 저장소의 같은 카드를 갈아 끼운다`() =
        runTest {
            val repository = FakeExperienceRepository(initial = listOf(existing))
            val draft =
                ExperienceDraft(
                    title = "CareerCompass 리뉴얼",
                    startPoint = ExperiencePoint.Date(LocalDate.of(2025, 9, 1)),
                    endPoint = ExperiencePoint.Date(LocalDate.of(2026, 2, 1)),
                    details = ExperienceDetails.Project(role = "안드로이드", techs = listOf("Kotlin"), summary = "요약", link = null),
                )

            val updated = UpdateExperienceUseCase(repository)(id = 7L, draft = draft).getOrThrow()

            assertEquals("CareerCompass 리뉴얼", updated.title)
            assertEquals(ExperiencePoint.Date(LocalDate.of(2026, 2, 1)), updated.endPoint)
            assertEquals(listOf(updated), repository.experiences.toList())
        }

    @Test
    fun `경험 수정 실패는 그대로 전파한다`() =
        runTest {
            val failure = IOException("offline")
            val repository = FakeExperienceRepository(onUpdateExperience = { _, _ -> Result.failure(failure) })
            val draft =
                ExperienceDraft(
                    title = "CareerCompass",
                    startPoint = ExperiencePoint.Date(LocalDate.of(2025, 9, 1)),
                    endPoint = null,
                    details = ExperienceDetails.Project(role = null, techs = emptyList(), summary = null, link = null),
                )

            assertSame(failure, UpdateExperienceUseCase(repository)(id = 7L, draft = draft).exceptionOrNull())
        }

    @Test
    fun `경험 삭제는 목록에서 카드를 빼고 실패는 전파한다`() =
        runTest {
            val repository = FakeExperienceRepository(initial = listOf(existing))

            assertTrue(DeleteExperienceUseCase(repository)(7L).isSuccess)
            assertTrue(repository.experiences.isEmpty())
            assertTrue(DeleteExperienceUseCase(repository)(7L).isFailure)
        }

    @Test
    fun `Step 3 를 지나면 진행 상태만 PastApplication 으로 옮긴다`() =
        runTest {
            val progressRepository = FakeOnboardingProgressRepository()

            assertTrue(ProceedToPastApplicationUseCase(progressRepository)().isSuccess)
            assertEquals(listOf(OnboardingStep.PastApplication), progressRepository.savedSteps)
        }
}
