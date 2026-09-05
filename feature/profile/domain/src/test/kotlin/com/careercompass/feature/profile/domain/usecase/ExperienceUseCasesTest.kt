package com.careercompass.feature.profile.domain.usecase

import com.careercompass.core.domain.error.CoreDataFailure
import com.careercompass.core.domain.repository.ExperienceRepository
import com.careercompass.core.domain.testing.FakeExperienceRepository
import com.careercompass.core.model.experience.ExperienceType
import com.careercompass.core.model.experience.MAX_EXPERIENCE_CARDS
import com.careercompass.core.model.paging.CursorPage
import com.careercompass.feature.profile.domain.ServerFailure
import com.careercompass.feature.profile.domain.error.ProfileFailure
import com.careercompass.feature.profile.domain.experience
import com.careercompass.feature.profile.domain.projectDraft
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** API_SPEC v0.1 §3 `/experiences` 를 덮는 use case 들. */
class ExperienceUseCasesTest {
    @Test
    fun `목록 조회는 유형과 커서를 그대로 넘기고 다음 커서를 잃지 않는다`() =
        runTest {
            val recorded = mutableListOf<Triple<ExperienceType?, String?, Int>>()
            val repository =
                FakeExperienceRepository.strict().apply {
                    onGetExperiences = { type, cursor, limit ->
                        recorded += Triple(type, cursor, limit)
                        Result.success(CursorPage(items = listOf(experience(1L)), nextCursor = "cursor-2"))
                    }
                }

            val page = GetExperiencePageUseCase(repository)(type = ExperienceType.Project, cursor = "cursor-1").getOrThrow()

            assertEquals(Triple(ExperienceType.Project, "cursor-1", ExperienceRepository.DEFAULT_PAGE_SIZE), recorded.single())
            assertEquals("cursor-2", page.nextCursor)
            assertTrue(page.hasNext)
        }

    @Test
    fun `목록 조회 기본값은 전체 유형 첫 페이지다`() =
        runTest {
            val recorded = mutableListOf<Triple<ExperienceType?, String?, Int>>()
            val repository =
                FakeExperienceRepository.strict().apply {
                    onGetExperiences = { type, cursor, limit ->
                        recorded += Triple(type, cursor, limit)
                        Result.success(CursorPage.empty())
                    }
                }

            GetExperiencePageUseCase(repository)().getOrThrow()

            val (type, cursor, _) = recorded.single()
            assertNull(type)
            assertNull(cursor)
        }

    @Test
    fun `개수는 유형 필터와 무관하게 전체를 센다`() =
        runTest {
            val repository =
                FakeExperienceRepository(initial = List(7) { experience(it + 1L) }).apply {
                    onGetExperiences = { type, _, limit ->
                        assertNull("개수는 필터를 걸지 않는다", type)
                        assertEquals(MAX_EXPERIENCE_CARDS, limit)
                        Result.success(CursorPage(items = experiences.toList(), nextCursor = null))
                    }
                }

            assertEquals(7, CountExperiencesUseCase(repository)().getOrThrow())
        }

    @Test
    fun `상한 아래면 카드를 등록한다`() =
        runTest {
            val repository = FakeExperienceRepository(initial = List(MAX_EXPERIENCE_CARDS - 1) { experience(it + 1L) })

            val created = CreateExperienceUseCase(repository)(projectDraft()).getOrThrow()

            assertEquals("CareerCompass", created.title)
            assertEquals(projectDraft(), repository.createdDrafts.single())
        }

    @Test
    fun `카드가 30개면 요청 없이 ExperienceLimitReached 로 거절한다`() =
        runTest {
            val repository = FakeExperienceRepository(initial = List(MAX_EXPERIENCE_CARDS) { experience(it + 1L) })

            val failure = CreateExperienceUseCase(repository)(projectDraft()).exceptionOrNull()

            assertTrue(failure is ProfileFailure.ExperienceLimitReached)
            assertEquals(MAX_EXPERIENCE_CARDS, (failure as ProfileFailure.ExperienceLimitReached).limit)
            assertTrue(repository.createdDrafts.isEmpty())
        }

    @Test
    fun `개수 조회가 실패하면 등록하지 않고 그 실패를 돌려준다`() =
        runTest {
            val repository =
                FakeExperienceRepository.strict().apply {
                    onGetExperiences = { _, _, _ -> ServerFailure.NetworkUnavailable.asResult() }
                }

            val failure = CreateExperienceUseCase(repository)(projectDraft()).exceptionOrNull()

            assertTrue(failure is CoreDataFailure.NetworkUnavailable)
            assertTrue(repository.createdDrafts.isEmpty())
        }

    @Test
    fun `서버가 상한을 먼저 알려 오면 그 실패를 그대로 흘려보낸다`() =
        runTest {
            val repository =
                FakeExperienceRepository(initial = emptyList()).apply {
                    onCreateExperience = { ServerFailure.LimitExceeded.asResult() }
                }

            val failure = CreateExperienceUseCase(repository)(projectDraft()).exceptionOrNull()

            assertTrue(failure is CoreDataFailure.LimitExceeded)
            assertEquals("LIMIT_EXCEEDED", (failure as CoreDataFailure.LimitExceeded).code)
        }

    @Test
    fun `수정은 개수를 세지 않는다`() =
        runTest {
            var listQueries = 0
            val repository =
                FakeExperienceRepository(initial = List(MAX_EXPERIENCE_CARDS) { experience(it + 1L) }).apply {
                    onGetExperiences = { _, _, _ ->
                        listQueries++
                        Result.success(CursorPage(items = experiences.toList(), nextCursor = null))
                    }
                }

            val updated = UpdateExperienceUseCase(repository)(id = 1L, draft = projectDraft(title = "고친 제목")).getOrThrow()

            assertEquals("고친 제목", updated.title)
            assertEquals(0, listQueries)
        }

    @Test
    fun `삭제는 그 카드만 지운다`() =
        runTest {
            val repository = FakeExperienceRepository(initial = listOf(experience(1L), experience(2L)))

            DeleteExperienceUseCase(repository)(1L).getOrThrow()

            assertEquals(listOf(2L), repository.experiences.map { it.id })
            assertFalse(repository.experiences.any { it.id == 1L })
        }
}
