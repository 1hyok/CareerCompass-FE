package com.careercompass.feature.profile.domain.usecase

import com.careercompass.core.domain.error.CoreDataFailure
import com.careercompass.core.domain.testing.FakeExperienceRepository
import com.careercompass.core.domain.testing.FakePastApplicationRepository
import com.careercompass.core.domain.testing.FakeUserProfileRepository
import com.careercompass.core.model.application.PastApplicationCategory
import com.careercompass.core.model.paging.CursorPage
import com.careercompass.core.model.user.UserProfileUpdate
import com.careercompass.feature.profile.domain.ServerFailure
import com.careercompass.feature.profile.domain.projectDraft
import com.careercompass.feature.profile.domain.uploadFile
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * §2·§3·§4 의 실패 경로 고정 — 이슈 #174 완료 조건.
 *
 * 서버가 낸 실패는 use case 를 지나며 **바뀌지 않는다.** 감싸거나 다른 타입으로 옮기면 화면이
 * [CoreDataFailure] 하위 타입으로 `when` 을 가를 수 없고, 리포팅도 서버 코드를 잃는다. 그래서
 * 「실패했다」가 아니라 **같은 인스턴스인지**를 본다.
 *
 * 갈래는 401(`AUTH_REQUIRED`) · 422(`LIMIT_EXCEEDED`) · 503(`LLM_UNAVAILABLE`) · 네트워크 끊김 ·
 * 응답 대기 중 끊김 다섯이다. 마지막 둘은 같은 타입이지만
 * [CoreDataFailure.NetworkUnavailable.isTimeout] 으로 갈리므로 그 판정까지 함께 잠근다.
 */
class ProfileFailurePathTest {
    @Test
    fun `프로필 조회 갱신은 서버 실패를 그대로 흘려보낸다`() =
        runTest {
            eachFailure { expected ->
                val repository = FakeUserProfileRepository.strict()
                repository.onRefreshProfile = { Result.failure(expected) }

                RefreshMyProfileUseCase(repository)().exceptionOrNull()
            }
        }

    @Test
    fun `기본 정보 수정은 서버 실패를 그대로 흘려보낸다`() =
        runTest {
            eachFailure { expected ->
                val repository = FakeUserProfileRepository.strict()
                repository.onUpdateProfile = { Result.failure(expected) }

                UpdateBasicInfoUseCase(repository)(UserProfileUpdate(name = "정일혁")).exceptionOrNull()
            }
        }

    @Test
    fun `희망 직무 교체는 서버 실패를 그대로 흘려보낸다`() =
        runTest {
            eachFailure { expected ->
                val repository = FakeUserProfileRepository.strict()
                repository.onReplaceJobInterests = { Result.failure(expected) }

                ReplaceJobInterestsUseCase(repository)(listOf("backend")).exceptionOrNull()
            }
        }

    @Test
    fun `관심 태그 교체는 서버 실패를 그대로 흘려보낸다`() =
        runTest {
            eachFailure { expected ->
                val repository = FakeUserProfileRepository.strict()
                repository.onReplaceTags = { Result.failure(expected) }

                ReplaceProfileTagsUseCase(repository)(listOf("AI")).exceptionOrNull()
            }
        }

    @Test
    fun `경험 카드 조회와 개수는 서버 실패를 그대로 흘려보낸다`() =
        runTest {
            eachFailure { expected ->
                val repository = experienceRepositoryFailingList(expected)

                GetExperiencePageUseCase(repository)().exceptionOrNull()
            }
            eachFailure { expected ->
                val repository = experienceRepositoryFailingList(expected)

                CountExperiencesUseCase(repository)().exceptionOrNull()
            }
        }

    @Test
    fun `경험 카드 등록은 개수 조회와 등록 어느 쪽 실패든 그대로 흘려보낸다`() =
        runTest {
            eachFailure { expected ->
                val repository = experienceRepositoryFailingList(expected)

                CreateExperienceUseCase(repository)(projectDraft()).exceptionOrNull()
            }
            eachFailure { expected ->
                val repository =
                    FakeExperienceRepository.strict().apply {
                        onGetExperiences = { _, _, _ -> Result.success(CursorPage.empty()) }
                        onCreateExperience = { Result.failure(expected) }
                    }

                CreateExperienceUseCase(repository)(projectDraft()).exceptionOrNull()
            }
        }

    @Test
    fun `경험 카드 수정과 삭제는 서버 실패를 그대로 흘려보낸다`() =
        runTest {
            eachFailure { expected ->
                val repository =
                    FakeExperienceRepository.strict().apply { onUpdateExperience = { _, _ -> Result.failure(expected) } }

                UpdateExperienceUseCase(repository)(id = 1L, draft = projectDraft()).exceptionOrNull()
            }
            eachFailure { expected ->
                val repository = FakeExperienceRepository.strict().apply { onDeleteExperience = { Result.failure(expected) } }

                DeleteExperienceUseCase(repository)(1L).exceptionOrNull()
            }
        }

    @Test
    fun `과거 지원서 조회는 서버 실패를 그대로 흘려보낸다`() =
        runTest {
            eachFailure { expected ->
                val repository = pastApplicationRepositoryFailingList(expected)

                GetPastApplicationsUseCase(repository)().exceptionOrNull()
            }
        }

    @Test
    fun `과거 지원서 업로드는 목록 조회와 업로드 어느 쪽 실패든 그대로 흘려보낸다`() =
        runTest {
            eachFailure { expected ->
                val repository = pastApplicationRepositoryFailingList(expected)

                UploadPastApplicationUseCase(repository)(uploadFile(), "지원서").exceptionOrNull()
            }
            eachFailure { expected ->
                val repository =
                    FakePastApplicationRepository.strict().apply {
                        onGetPastApplications = { Result.success(emptyList()) }
                        onUpload = { _, _ -> Result.failure(expected) }
                    }

                UploadPastApplicationUseCase(repository)(uploadFile(), "지원서").exceptionOrNull()
            }
        }

    @Test
    fun `과거 지원서 항목 조정과 삭제는 서버 실패를 그대로 흘려보낸다`() =
        runTest {
            eachFailure { expected ->
                val repository =
                    FakePastApplicationRepository.strict().apply {
                        onUpdateItemCategory = { _, _, _ -> Result.failure(expected) }
                    }

                UpdatePastApplicationItemCategoryUseCase(repository)(
                    applicationId = 1L,
                    itemId = 10L,
                    category = PastApplicationCategory.Aspiration,
                ).exceptionOrNull()
            }
            eachFailure { expected ->
                val repository = FakePastApplicationRepository.strict().apply { onDelete = { Result.failure(expected) } }

                DeletePastApplicationUseCase(repository)(1L).exceptionOrNull()
            }
        }

    @Test
    fun `연결 실패와 응답 대기 중 끊김은 같은 타입 안에서 갈린다`() {
        val offline = ServerFailure.NetworkUnavailable.create() as CoreDataFailure.NetworkUnavailable
        val timeout = ServerFailure.Timeout.create() as CoreDataFailure.NetworkUnavailable

        assertTrue("연결 자체가 안 된 것은 타임아웃이 아니다", !offline.isTimeout)
        assertTrue("응답을 기다리다 끊은 것은 타임아웃이다", timeout.isTimeout)
    }

    private fun experienceRepositoryFailingList(failure: Throwable) =
        FakeExperienceRepository.strict().apply { onGetExperiences = { _, _, _ -> Result.failure(failure) } }

    private fun pastApplicationRepositoryFailingList(failure: Throwable) =
        FakePastApplicationRepository.strict().apply { onGetPastApplications = { Result.failure(failure) } }

    /** 다섯 갈래를 모두 돌리고, 넘어온 실패가 심어 둔 그 인스턴스인지 본다. */
    private suspend fun eachFailure(run: suspend (Throwable) -> Throwable?) {
        for (variant in ServerFailure.entries) {
            val expected = variant.create()

            assertSame("$variant 가 use case 를 지나며 바뀌었다", expected, run(expected))
        }
    }
}
