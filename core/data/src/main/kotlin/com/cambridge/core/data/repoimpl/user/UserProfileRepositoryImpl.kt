package com.cambridge.core.data.repoimpl.user

import com.cambridge.core.common.result.runCatchingCancellable
import com.cambridge.core.data.failure.mapDataFailure
import com.cambridge.core.data.mapper.UserMapper
import com.cambridge.core.datastore.TokenDataSource
import com.cambridge.core.domain.repository.UserProfileRepository
import com.cambridge.core.model.user.JobInterest
import com.cambridge.core.model.user.MAX_JOB_INTERESTS
import com.cambridge.core.model.user.MAX_PROFILE_TAGS
import com.cambridge.core.model.user.UserProfile
import com.cambridge.core.model.user.UserProfileUpdate
import com.cambridge.core.network.dto.JobInterestsRequestDto
import com.cambridge.core.network.dto.TagsRequestDto
import com.cambridge.core.network.model.requireData
import com.cambridge.core.network.model.requireOk
import com.cambridge.core.network.service.UserApiService
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
internal class UserProfileRepositoryImpl
    @Inject
    constructor(
        private val userApiService: UserApiService,
        private val tokenDataSource: TokenDataSource,
    ) : UserProfileRepository {
        private val cache = MutableStateFlow<UserProfile?>(null)

        /** 세션이 끝나면 캐시 값을 흘리지 않는다 — 다음 로그인 사용자에게 이전 프로필이 보이면 안 된다. */
        override val profile: Flow<UserProfile?> =
            combine(tokenDataSource.isLoggedIn, cache) { loggedIn, cached -> cached.takeIf { loggedIn } }

        override suspend fun refreshProfile(): Result<UserProfile> =
            runCatchingCancellable {
                UserMapper.toProfile(userApiService.getMe().requireData()).also { cache.value = it }
            }.mapDataFailure()

        override suspend fun updateProfile(update: UserProfileUpdate): Result<UserProfile> {
            if (update.isEmpty) return cache.value?.let { Result.success(it) } ?: refreshProfile()
            return runCatchingCancellable {
                UserMapper.toProfile(userApiService.updateMe(UserMapper.toUpdateRequest(update)).requireData()).also { cache.value = it }
            }.mapDataFailure()
        }

        override suspend fun replaceJobInterests(interests: List<JobInterest>): Result<Unit> {
            require(interests.size in 1..MAX_JOB_INTERESTS) { "job interests must be 1..$MAX_JOB_INTERESTS" }
            require(interests.map(JobInterest::code).distinct().size == interests.size) { "job interest codes must be unique" }
            return runCatchingCancellable {
                userApiService.replaceJobInterests(JobInterestsRequestDto(interests.map(UserMapper::toJobInterestDto))).requireOk()
                cache.update { it?.copy(jobInterests = interests) }
            }.mapDataFailure()
        }

        override suspend fun replaceTags(tags: List<String>): Result<Unit> {
            require(tags.size in 1..MAX_PROFILE_TAGS) { "tags must be 1..$MAX_PROFILE_TAGS" }
            require(tags.all(String::isNotBlank) && tags.distinct().size == tags.size) { "tags must be non-blank and unique" }
            return runCatchingCancellable {
                userApiService.replaceTags(TagsRequestDto(tags)).requireOk()
                cache.update { it?.copy(tags = tags) }
            }.mapDataFailure()
        }
    }
