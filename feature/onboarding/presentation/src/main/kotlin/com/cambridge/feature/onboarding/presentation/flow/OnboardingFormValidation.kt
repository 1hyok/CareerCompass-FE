package com.cambridge.feature.onboarding.presentation.flow

import com.cambridge.core.model.user.MAX_GRADE_POINT_AVERAGE
import com.cambridge.core.model.user.MIN_GRADUATION_YEAR
import com.cambridge.feature.onboarding.presentation.shared.model.OnboardingFieldError

/** Step 1 검증 규칙 — 기능 스펙 F1-2 Step 1 (이름 20자·학과 30자·학점 0.0~4.5·졸업 연도 2000~). */
internal object OnboardingStep1Rules {
    const val MAX_NAME_LENGTH = 20
    const val MAX_MAJOR_LENGTH = 30

    /** 입력 중에는 빈 값을 탓하지 않는다([requireValue] false) — 「다음」 을 눌렀을 때만 필수 오류를 낸다. */
    fun validateText(
        value: String,
        maxLength: Int,
        requireValue: Boolean,
    ): OnboardingFieldError? =
        when {
            value.isBlank() -> if (requireValue) OnboardingFieldError.Required else null
            value.trim().length > maxLength -> OnboardingFieldError.TooLong(maxLength)
            else -> null
        }

    fun validateGradePointAverage(value: String): OnboardingFieldError? {
        if (value.isBlank()) return null
        val parsed = parseGradePointAverage(value) ?: return OnboardingFieldError.InvalidFormat
        return if (parsed in 0.0..MAX_GRADE_POINT_AVERAGE) null else OnboardingFieldError.OutOfRange
    }

    fun parseGradePointAverage(value: String): Double? = value.trim().takeIf(GPA_PATTERN::matches)?.toDoubleOrNull()

    /** `YYYY` 또는 `YYYY.MM`. 서버에는 연도만 보내므로 월 없는 값도 받는다(프로필 프리필이 연도만 안다). */
    fun validateGraduationDate(value: String): OnboardingFieldError? {
        if (value.isBlank()) return null
        val year = parseGraduationYear(value) ?: return OnboardingFieldError.InvalidFormat
        return if (year >= MIN_GRADUATION_YEAR) null else OnboardingFieldError.OutOfRange
    }

    fun parseGraduationYear(value: String): Int? =
        GRADUATION_PATTERN
            .matchEntire(value.trim())
            ?.groupValues
            ?.get(1)
            ?.toIntOrNull()

    private val GPA_PATTERN = Regex("""^\d(\.\d{1,2})?$""")
    private val GRADUATION_PATTERN = Regex("""^(\d{4})(?:\.(0[1-9]|1[0-2]))?$""")
}

/** Step 2 태그 정규화 — 앞의 `#` 과 양끝 공백을 걷어낸다. */
internal fun normalizeInterestTag(raw: String): String = raw.trim().trimStart('#').trim()
