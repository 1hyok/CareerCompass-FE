package com.cambridge.core.domain.repository

import com.cambridge.core.model.user.JobInterest
import com.cambridge.core.model.user.UserProfile
import com.cambridge.core.model.user.UserProfileUpdate
import kotlinx.coroutines.flow.Flow

/** 내 프로필 계약 — API_SPEC v0.1 §2 `/users/me`. */
public interface UserProfileRepository {
    /** 마지막으로 성공적으로 받은 프로필. 아직 받은 적 없으면 null. 세션 정리 시 null 로 돌아간다. */
    public val profile: Flow<UserProfile?>

    /** `GET /users/me` — 성공하면 [profile] 도 갱신한다. */
    public suspend fun refreshProfile(): Result<UserProfile>

    /**
     * 마지막으로 알려진 온보딩 완료 여부 — 저장된 프로필이 있으면 그 값, 없으면 로그인 응답이 남긴 힌트, 둘 다
     * 없으면 null. 서버 조회가 실패했을 때 시작 화면을 고르는 근거다.
     */
    public suspend fun lastKnownOnboardingDone(): Boolean?

    /** `PATCH /users/me` — 빈 수정은 요청 없이 현재 프로필을 돌려준다. */
    public suspend fun updateProfile(update: UserProfileUpdate): Result<UserProfile>

    /** `PUT /users/me/job-interests` — 최소 1, 최대 3. */
    public suspend fun replaceJobInterests(interests: List<JobInterest>): Result<Unit>

    /** `PUT /users/me/tags` — 최소 1, 최대 5. */
    public suspend fun replaceTags(tags: List<String>): Result<Unit>
}
