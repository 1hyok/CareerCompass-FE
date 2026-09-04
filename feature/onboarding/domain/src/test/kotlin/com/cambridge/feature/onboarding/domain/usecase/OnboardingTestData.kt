package com.cambridge.feature.onboarding.domain.usecase

import com.careercompass.core.model.user.JobInterest
import com.careercompass.core.model.user.UserProfile

internal fun sampleProfile(
    onboardingDone: Boolean = false,
    name: String? = "정일혁",
): UserProfile =
    UserProfile(
        id = 1L,
        name = name,
        school = "건국대학교",
        department = "컴퓨터공학부",
        gpa = 3.87,
        gradYear = 2027,
        jobInterests = listOf(JobInterest(code = "backend", priority = 1)),
        tags = listOf("AI"),
        onboardingDone = onboardingDone,
        completion = 40,
    )
