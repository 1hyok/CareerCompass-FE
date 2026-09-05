package com.careercompass.feature.profile.domain.usecase

import com.careercompass.core.domain.testing.FakeUserProfileRepository
import com.careercompass.core.model.user.JobInterest
import com.careercompass.core.model.user.MAX_JOB_INTERESTS
import com.careercompass.core.model.user.MAX_PROFILE_TAGS
import com.careercompass.core.model.user.UserProfileUpdate
import com.careercompass.feature.profile.domain.profile
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** API_SPEC v0.1 §2 `/users/me` 를 덮는 use case 들. */
class MyProfileUseCasesTest {
    @Test
    fun `프로필을 아직 받은 적 없으면 캐시는 null 을 낸다`() =
        runTest {
            val repository = FakeUserProfileRepository.strict()

            assertNull(ObserveMyProfileUseCase(repository)().first())
        }

    @Test
    fun `갱신이 성공하면 캐시도 그 값으로 바뀐다`() =
        runTest {
            val repository = FakeUserProfileRepository(initialProfile = profile(completion = 40))
            repository.onRefreshProfile = {
                repository.profileState.value = profile(completion = 78)
                Result.success(profile(completion = 78))
            }

            val refreshed = RefreshMyProfileUseCase(repository)().getOrThrow()

            assertEquals(78, refreshed.completion)
            assertEquals(78, ObserveMyProfileUseCase(repository)().first()?.completion)
        }

    @Test
    fun `기본 정보 수정은 넘긴 부분 수정을 그대로 보낸다`() =
        runTest {
            val repository = FakeUserProfileRepository(initialProfile = profile())
            val update = UserProfileUpdate(department = "스마트ICT융합공학과", gradYear = 2028)

            val updated = UpdateBasicInfoUseCase(repository)(update).getOrThrow()

            assertEquals(update, repository.updates.single())
            assertEquals("스마트ICT융합공학과", updated.department)
            assertEquals(2028, updated.gradYear)
            assertEquals("정일혁", updated.name)
        }

    @Test
    fun `빈 수정도 막지 않고 현재 프로필을 돌려준다`() =
        runTest {
            val repository = FakeUserProfileRepository(initialProfile = profile())

            val updated = UpdateBasicInfoUseCase(repository)(UserProfileUpdate()).getOrThrow()

            assertEquals(profile(), updated)
        }

    @Test
    fun `희망 직무는 고른 순서가 곧 우선순위다`() =
        runTest {
            val repository = FakeUserProfileRepository(initialProfile = profile())

            ReplaceJobInterestsUseCase(repository)(listOf("android", "backend", "devops")).getOrThrow()

            assertEquals(
                listOf(
                    JobInterest(code = "android", priority = 1),
                    JobInterest(code = "backend", priority = 2),
                    JobInterest(code = "devops", priority = 3),
                ),
                repository.replacedJobInterests.single(),
            )
        }

    @Test
    fun `희망 직무 개수가 범위를 벗어나면 요청하지 않는다`() =
        runTest {
            val repository = FakeUserProfileRepository.strict()

            assertTrue(runCatching { ReplaceJobInterestsUseCase(repository)(emptyList()) }.isFailure)
            assertTrue(
                runCatching {
                    ReplaceJobInterestsUseCase(repository)(List(MAX_JOB_INTERESTS + 1) { "job$it" })
                }.isFailure,
            )
            assertTrue(repository.replacedJobInterests.isEmpty())
        }

    @Test
    fun `같은 직무를 두 번 고르면 거절한다`() =
        runTest {
            val repository = FakeUserProfileRepository.strict()

            assertTrue(runCatching { ReplaceJobInterestsUseCase(repository)(listOf("backend", "backend")) }.isFailure)
            assertTrue(repository.replacedJobInterests.isEmpty())
        }

    @Test
    fun `태그는 앞뒤 공백을 다듬어 보낸다`() =
        runTest {
            val repository = FakeUserProfileRepository(initialProfile = profile())

            ReplaceProfileTagsUseCase(repository)(listOf(" AI ", "스타트업")).getOrThrow()

            assertEquals(listOf("AI", "스타트업"), repository.replacedTags.single())
        }

    @Test
    fun `다듬고 나서 같아지는 태그는 중복으로 거절한다`() =
        runTest {
            val repository = FakeUserProfileRepository.strict()

            assertTrue(runCatching { ReplaceProfileTagsUseCase(repository)(listOf("AI", " AI")) }.isFailure)
            assertTrue(repository.replacedTags.isEmpty())
        }

    @Test
    fun `태그 개수가 범위를 벗어나면 요청하지 않는다`() =
        runTest {
            val repository = FakeUserProfileRepository.strict()

            assertTrue(runCatching { ReplaceProfileTagsUseCase(repository)(emptyList()) }.isFailure)
            assertTrue(
                runCatching {
                    ReplaceProfileTagsUseCase(repository)(List(MAX_PROFILE_TAGS + 1) { "태그$it" })
                }.isFailure,
            )
            assertTrue(repository.replacedTags.isEmpty())
        }

    @Test
    fun `공백뿐인 태그는 거절한다`() =
        runTest {
            val repository = FakeUserProfileRepository.strict()

            assertTrue(runCatching { ReplaceProfileTagsUseCase(repository)(listOf("AI", "   ")) }.isFailure)
            assertTrue(repository.replacedTags.isEmpty())
        }
}
