package com.cambridge.core.domain.testing

import com.cambridge.core.domain.repository.UserProfileRepository
import com.cambridge.core.model.user.JobInterest
import com.cambridge.core.model.user.UserProfile
import com.cambridge.core.model.user.UserProfileUpdate
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import java.util.concurrent.CopyOnWriteArrayList

/** [UserProfileRepository] fake 정본. 기본은 메모리 프로필을 즉시 갱신하고 호출을 기록한다. */
public class FakeUserProfileRepository(
    initialProfile: UserProfile? = null,
    public var onRefreshProfile: (suspend () -> Result<UserProfile>)? = null,
    public var onUpdateProfile: (suspend (UserProfileUpdate) -> Result<UserProfile>)? = null,
    public var onReplaceJobInterests: (suspend (List<JobInterest>) -> Result<Unit>)? = null,
    public var onReplaceTags: (suspend (List<String>) -> Result<Unit>)? = null,
    public var onLastKnownOnboardingDone: (suspend () -> Boolean?)? = null,
    /** 저장 프로필이 없을 때 [lastKnownOnboardingDone] 이 돌려줄 로그인 힌트. */
    public var onboardingDoneHint: Boolean? = null,
) : UserProfileRepository {
    public val profileState: MutableStateFlow<UserProfile?> = MutableStateFlow(initialProfile)
    public val updates: CopyOnWriteArrayList<UserProfileUpdate> = CopyOnWriteArrayList()
    public val replacedJobInterests: CopyOnWriteArrayList<List<JobInterest>> = CopyOnWriteArrayList()
    public val replacedTags: CopyOnWriteArrayList<List<String>> = CopyOnWriteArrayList()

    override val profile: Flow<UserProfile?> get() = profileState

    override suspend fun refreshProfile(): Result<UserProfile> {
        onRefreshProfile?.let { return it() }
        return profileState.value?.let { Result.success(it) }
            ?: Result.failure(IllegalStateException("프로필이 준비되지 않았습니다."))
    }

    override suspend fun lastKnownOnboardingDone(): Boolean? {
        onLastKnownOnboardingDone?.let { return it() }
        return profileState.value?.onboardingDone ?: onboardingDoneHint
    }

    override suspend fun updateProfile(update: UserProfileUpdate): Result<UserProfile> {
        updates += update
        onUpdateProfile?.let { return it(update) }
        val current = profileState.value ?: return Result.failure(IllegalStateException("프로필이 준비되지 않았습니다."))
        val updated =
            current.copy(
                name = update.name ?: current.name,
                school = update.school ?: current.school,
                department = update.department ?: current.department,
                gpa = update.gpa ?: current.gpa,
                gradYear = update.gradYear ?: current.gradYear,
            )
        profileState.value = updated
        return Result.success(updated)
    }

    override suspend fun replaceJobInterests(interests: List<JobInterest>): Result<Unit> {
        replacedJobInterests += interests
        onReplaceJobInterests?.let { return it(interests) }
        profileState.value = profileState.value?.copy(jobInterests = interests)
        return Result.success(Unit)
    }

    override suspend fun replaceTags(tags: List<String>): Result<Unit> {
        replacedTags += tags
        onReplaceTags?.let { return it(tags) }
        profileState.value = profileState.value?.copy(tags = tags)
        return Result.success(Unit)
    }

    public companion object {
        public fun strict(initialProfile: UserProfile? = null): FakeUserProfileRepository =
            FakeUserProfileRepository(
                initialProfile = initialProfile,
                onRefreshProfile = { unexpectedCall("UserProfileRepository.refreshProfile") },
                onUpdateProfile = { unexpectedCall("UserProfileRepository.updateProfile") },
                onReplaceJobInterests = { unexpectedCall("UserProfileRepository.replaceJobInterests") },
                onReplaceTags = { unexpectedCall("UserProfileRepository.replaceTags") },
                onLastKnownOnboardingDone = { unexpectedCall("UserProfileRepository.lastKnownOnboardingDone") },
            )
    }
}
