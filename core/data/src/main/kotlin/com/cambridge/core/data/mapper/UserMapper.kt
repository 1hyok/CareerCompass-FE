package com.cambridge.core.data.mapper

import com.cambridge.core.network.dto.JobInterestDto
import com.cambridge.core.network.dto.UpdateProfileRequestDto
import com.cambridge.core.network.dto.UserProfileDto
import com.careercompass.core.model.user.JobInterest
import com.careercompass.core.model.user.UserProfile
import com.careercompass.core.model.user.UserProfileUpdate

internal object UserMapper {
    fun toProfile(dto: UserProfileDto): UserProfile =
        UserProfile(
            id = dto.id,
            name = dto.name?.takeIf(String::isNotBlank),
            school = dto.school?.takeIf(String::isNotBlank),
            department = dto.department?.takeIf(String::isNotBlank),
            gpa = dto.gpa,
            gradYear = dto.gradYear,
            jobInterests = dto.jobInterests.map { JobInterest(code = it.code, priority = it.priority) },
            tags = dto.tags,
            onboardingDone = dto.onboardingDone,
            completion = dto.completion,
        )

    /** 로컬 캐시 직렬화용 — [toProfile] 의 역방향. 정규화(빈 문자열 → null)는 이미 끝난 값이라 그대로 옮긴다. */
    fun toDto(profile: UserProfile): UserProfileDto =
        UserProfileDto(
            id = profile.id,
            name = profile.name,
            school = profile.school,
            department = profile.department,
            gpa = profile.gpa,
            gradYear = profile.gradYear,
            jobInterests = profile.jobInterests.map(::toJobInterestDto),
            tags = profile.tags,
            onboardingDone = profile.onboardingDone,
            completion = profile.completion,
        )

    fun toUpdateRequest(update: UserProfileUpdate): UpdateProfileRequestDto =
        UpdateProfileRequestDto(
            name = update.name,
            school = update.school,
            department = update.department,
            gpa = update.gpa,
            gradYear = update.gradYear,
        )

    fun toJobInterestDto(interest: JobInterest): JobInterestDto = JobInterestDto(code = interest.code, priority = interest.priority)
}
