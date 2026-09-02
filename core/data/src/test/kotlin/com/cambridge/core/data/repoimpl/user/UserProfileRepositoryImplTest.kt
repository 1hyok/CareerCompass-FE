package com.cambridge.core.data.repoimpl.user

import com.cambridge.core.data.support.InMemoryPreferencesDataStore
import com.cambridge.core.datastore.TokenDataSource
import com.cambridge.core.domain.error.CoreDataFailure
import com.cambridge.core.model.user.JobInterest
import com.cambridge.core.model.user.UserProfileUpdate
import com.cambridge.core.network.dto.JobInterestDto
import com.cambridge.core.network.dto.JobInterestsRequestDto
import com.cambridge.core.network.dto.TagsRequestDto
import com.cambridge.core.network.dto.UpdateProfileRequestDto
import com.cambridge.core.network.dto.UserProfileDto
import com.cambridge.core.network.model.ApiException
import com.cambridge.core.network.model.BaseResponse
import com.cambridge.core.network.service.UserApiService
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class UserProfileRepositoryImplTest {
    private class FakeUserApi : UserApiService {
        var profile = UserProfileDto(1, "정일혁", "건국대학교", "컴퓨터공학부", 3.87, 2027, listOf(JobInterestDto("backend", 1)), listOf("AI"), true, 78)
        var getMeThrows: Throwable? = null
        val updates = mutableListOf<UpdateProfileRequestDto>()
        val jobInterests = mutableListOf<JobInterestsRequestDto>()
        val tags = mutableListOf<TagsRequestDto>()

        override suspend fun getMe(): BaseResponse<UserProfileDto> {
            getMeThrows?.let { throw it }
            return BaseResponse(ok = true, data = profile)
        }

        override suspend fun updateMe(body: UpdateProfileRequestDto): BaseResponse<UserProfileDto> {
            updates += body
            profile = profile.copy(name = body.name ?: profile.name, gpa = body.gpa ?: profile.gpa)
            return BaseResponse(ok = true, data = profile)
        }

        override suspend fun replaceJobInterests(body: JobInterestsRequestDto): BaseResponse<Unit> {
            jobInterests += body
            return BaseResponse(ok = true)
        }

        override suspend fun replaceTags(body: TagsRequestDto): BaseResponse<Unit> {
            tags += body
            return BaseResponse(ok = true)
        }
    }

    private val api = FakeUserApi()
    private val tokenDataSource = TokenDataSource(InMemoryPreferencesDataStore())
    private val repository = UserProfileRepositoryImpl(api, tokenDataSource)

    @Test
    fun `조회 성공은 캐시를 갱신하고 로그인 상태에서만 흘린다`() =
        runTest {
            assertNull(repository.profile.first())

            repository.refreshProfile().getOrThrow()

            assertNull(repository.profile.first())
            tokenDataSource.saveTokens("access", "refresh")
            assertEquals("정일혁", repository.profile.first()?.name)
            tokenDataSource.clear()
            assertNull(repository.profile.first())
        }

    @Test
    fun `빈 수정은 요청 없이 현재 프로필을 돌려준다`() =
        runTest {
            repository.refreshProfile().getOrThrow()

            val profile = repository.updateProfile(UserProfileUpdate()).getOrThrow()

            assertEquals("정일혁", profile.name)
            assertTrue(api.updates.isEmpty())
        }

    @Test
    fun `부분 수정과 직무·태그 교체는 캐시에 반영된다`() =
        runTest {
            tokenDataSource.saveTokens("access", "refresh")
            repository.refreshProfile().getOrThrow()

            repository.updateProfile(UserProfileUpdate(name = "일혁")).getOrThrow()
            repository.replaceJobInterests(listOf(JobInterest("frontend", 1), JobInterest("data", 2))).getOrThrow()
            repository.replaceTags(listOf("AI", "환경")).getOrThrow()

            val cached = requireNotNull(repository.profile.first())
            assertEquals("일혁", cached.name)
            assertEquals(listOf("frontend", "data"), cached.jobInterests.map { it.code })
            assertEquals(listOf("AI", "환경"), cached.tags)
            assertEquals(UpdateProfileRequestDto(name = "일혁"), api.updates.single())
        }

    @Test
    fun `직무는 1~3개, 태그는 1~5개 범위를 벗어나면 요청 전에 거부한다`() =
        runTest {
            assertThrows(IllegalArgumentException::class.java) {
                kotlinx.coroutines.runBlocking { repository.replaceJobInterests(emptyList()) }
            }
            assertThrows(IllegalArgumentException::class.java) {
                kotlinx.coroutines.runBlocking { repository.replaceTags(List(6) { "tag$it" }) }
            }
            assertTrue(api.jobInterests.isEmpty() && api.tags.isEmpty())
        }

    @Test
    fun `서버 실패는 도메인 사유로 옮긴다`() =
        runTest {
            api.getMeThrows = ApiException("AUTH_REQUIRED", null, "인증 필요", status = 401)

            assertTrue(repository.refreshProfile().exceptionOrNull() is CoreDataFailure.Unauthorized)
        }
}
