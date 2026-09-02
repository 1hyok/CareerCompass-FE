package com.cambridge.core.data.mapper

import com.cambridge.core.model.user.JobInterest
import com.cambridge.core.model.user.UserProfile
import com.cambridge.core.model.user.UserProfileUpdate
import com.cambridge.core.network.dto.JobInterestDto
import com.cambridge.core.network.dto.UpdateProfileRequestDto
import com.cambridge.core.network.dto.UserProfileDto

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
