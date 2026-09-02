package com.cambridge.feature.onboarding.presentation.experience

import androidx.compose.runtime.Immutable
import com.cambridge.core.model.experience.ExperienceType
import com.cambridge.feature.onboarding.presentation.shared.model.OnboardingFieldError
import java.time.LocalDate
import java.time.YearMonth

/**
 * Step 3 「경험 추가」 시트 상태 — 온보딩용 빠른 입력이라 유형별 상세 폼(F1-3) 대신 공통 5개 필드만 받는다.
 *
 * [primary]·[secondary] 의 뜻은 [type] 에 따라 다르다 — [ExperienceEditorRules] 참고.
 * 필드 오류는 사유만 두고 문구는 시트가 리소스로 만든다.
 */
@Immutable
public data class ExperienceEditorState(
    public val type: ExperienceType = ExperienceType.Project,
    public val title: String = "",
    public val startDate: String = "",
    public val endDate: String = "",
    public val primary: String = "",
    public val secondary: String = "",
    public val titleError: OnboardingFieldError? = null,
    public val startDateError: OnboardingFieldError? = null,
    public val endDateError: OnboardingFieldError? = null,
    public val primaryError: OnboardingFieldError? = null,
    public val secondaryError: OnboardingFieldError? = null,
    public val isSubmitting: Boolean = false,
) {
    public val isInputEnabled: Boolean
        get() = !isSubmitting

    /** 제목만 있으면 제출을 시도할 수 있다 — 나머지 검증은 제출 시점에 필드 오류로 돌려준다. */
    public val isSubmitEnabled: Boolean
        get() = !isSubmitting && title.isNotBlank()
}

/** User intentions emitted by [ExperienceQuickAddSheet]. */
public sealed interface ExperienceQuickAddEvent {
    public data class TypeSelected(
        public val type: ExperienceType,
    ) : ExperienceQuickAddEvent

    public data class TitleChanged(
        public val value: String,
    ) : ExperienceQuickAddEvent

    public data class StartDateChanged(
        public val value: String,
    ) : ExperienceQuickAddEvent

    public data class EndDateChanged(
        public val value: String,
    ) : ExperienceQuickAddEvent

    public data class PrimaryChanged(
        public val value: String,
    ) : ExperienceQuickAddEvent

    public data class SecondaryChanged(
        public val value: String,
    ) : ExperienceQuickAddEvent

    public data object Submitted : ExperienceQuickAddEvent

    public data object Dismissed : ExperienceQuickAddEvent
}

/**
 * 유형별 필드 의미와 필수 규칙 — 기능 스펙 F1-3 「유형별 입력 필드」를 빠른 입력 5필드로 접은 것.
 *
 * | 유형 | primary | secondary | 시작 |
 * |---|---|---|---|
 * | 프로젝트 | 역할 | 요약 | 필수 |
 * | 수상 | 수상 등급 (필수) | 주관 기관 | 연도로 사용 |
 * | 인턴 | 회사명 (필수) | 직무 (필수) | 필수 |
 * | 대외활동 | 기관명 (필수) | 성과 요약 | 선택 |
 * | 자격증 | 발급 기관 | (없음) | 취득 연월 |
 */
public object ExperienceEditorRules {
    public const val MAX_TITLE_LENGTH: Int = 50
    public const val MAX_TEXT_LENGTH: Int = 100

    public fun isStartDateRequired(type: ExperienceType): Boolean = type == ExperienceType.Project || type == ExperienceType.Intern

    public fun hasEndDate(type: ExperienceType): Boolean = type != ExperienceType.Award && type != ExperienceType.Certificate

    public fun isPrimaryRequired(type: ExperienceType): Boolean =
        type == ExperienceType.Award || type == ExperienceType.Intern || type == ExperienceType.Activity

    public fun hasSecondary(type: ExperienceType): Boolean = type != ExperienceType.Certificate

    public fun isSecondaryRequired(type: ExperienceType): Boolean = type == ExperienceType.Intern

    /** `YYYY.MM` 을 그 달 1일로 읽는다. 형식이 다르면 null. */
    public fun parseYearMonth(value: String): LocalDate? {
        val match = YEAR_MONTH.matchEntire(value.trim()) ?: return null
        val (year, month) = match.destructured
        return YearMonth.of(year.toInt(), month.toInt()).atDay(1)
    }

    private val YEAR_MONTH = Regex("""^(\d{4})\.(0[1-9]|1[0-2])$""")
}
