package com.cambridge.feature.onboarding.presentation.experience

import androidx.compose.runtime.Immutable
import com.cambridge.core.model.experience.ExperienceType
import com.cambridge.feature.onboarding.presentation.shared.model.OnboardingFieldError
import java.net.URI
import java.time.LocalDate
import java.time.YearMonth

/**
 * Step 3 「경험 추가·수정」 시트 상태 — 공통 5개 필드 + 유형별 상세(F1-3) 입력.
 *
 * [primary]·[secondary]·[detail] 의 뜻은 [type] 에 따라 다르다 — [ExperienceEditorRules] 참고.
 * 필드 오류는 사유만 두고 문구는 시트가 리소스로 만든다.
 *
 * ### 상세 입력을 왜 접어 두는가 (#139)
 * Step 3 는 기능 스펙 F1-2 가 「1개 이상 입력 시 정확도 상승」으로 정의한 **선택 단계**다. 상세 필드를 항상
 * 펼쳐 두면 시트가 길어져 「나중에 하자」가 늘고, 그러면 카드 하나조차 안 남아 F3-2 우대 조건 매칭이 오히려
 * 더 굶는다. 그래서 필수 규칙이 걸린 공통 5필드만 항상 보이고, 상세는 [isDetailExpanded] 로 접는다.
 * **이미 값이 있는 카드를 고칠 때는 펼친 채로 연다** — 접혀 있으면 사용자는 그 값이 지워졌다고 읽는다.
 *
 * @property experienceId 수정 중인 카드의 id. null 이면 신규 등록이다. 수정 중에는 [type] 을 바꾸지 않는다 —
 * 유형마다 필드 의미가 달라 바꾸려면 지우고 다시 만들어야 한다.
 * @property techs 확정된 기술 태그. 입력칸([techInput])의 글자는 아직 태그가 아니다.
 * @property isDetailExpanded 상세 입력 영역을 펼쳤는가. 접혀 있어도 값은 상태에 그대로 살아 저장에 실린다 —
 * 접기는 보이기만 줄이는 것이지 값을 버리는 것이 아니다.
 */
@Immutable
public data class ExperienceEditorState(
    public val experienceId: Long? = null,
    public val type: ExperienceType = ExperienceType.Project,
    public val title: String = "",
    public val startDate: String = "",
    public val endDate: String = "",
    public val primary: String = "",
    public val secondary: String = "",
    public val techs: List<String> = emptyList(),
    public val techInput: String = "",
    public val link: String = "",
    public val detail: String = "",
    public val isDetailExpanded: Boolean = false,
    public val titleError: OnboardingFieldError? = null,
    public val startDateError: OnboardingFieldError? = null,
    public val endDateError: OnboardingFieldError? = null,
    public val primaryError: OnboardingFieldError? = null,
    public val secondaryError: OnboardingFieldError? = null,
    public val techInputError: OnboardingFieldError? = null,
    public val linkError: OnboardingFieldError? = null,
    public val detailError: OnboardingFieldError? = null,
    public val isSubmitting: Boolean = false,
) {
    public val isInputEnabled: Boolean
        get() = !isSubmitting

    /** 제목만 있으면 제출을 시도할 수 있다 — 나머지 검증은 제출 시점에 필드 오류로 돌려준다. */
    public val isSubmitEnabled: Boolean
        get() = !isSubmitting && title.isNotBlank()

    /** 기존 카드를 고치는 중인가. 시트 제목·제출 문구와 유형 잠금이 이 값으로 갈린다. */
    public val isEditing: Boolean
        get() = experienceId != null

    /** 접힌 상세 영역에 이미 채워진 값이 있는가 — 접힌 채로도 「입력됨」을 알리려고 시트가 쓴다. */
    public val hasDetailValues: Boolean
        get() = techs.isNotEmpty() || link.isNotBlank() || detail.isNotBlank()
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

    /** 기술 태그 입력칸의 글자가 바뀌었다. 아직 태그가 된 것은 아니다. */
    public data class TechInputChanged(
        public val value: String,
    ) : ExperienceQuickAddEvent

    /** 입력칸의 글자를 태그로 확정한다(키보드 완료·칩 추가). */
    public data object TechTagSubmitted : ExperienceQuickAddEvent

    public data class TechTagRemoved(
        public val tag: String,
    ) : ExperienceQuickAddEvent

    public data class LinkChanged(
        public val value: String,
    ) : ExperienceQuickAddEvent

    public data class DetailChanged(
        public val value: String,
    ) : ExperienceQuickAddEvent

    /** 상세 입력 영역을 펼치거나 접는다. */
    public data object DetailSectionToggled : ExperienceQuickAddEvent

    public data object Submitted : ExperienceQuickAddEvent

    public data object Dismissed : ExperienceQuickAddEvent
}

/**
 * 유형별 필드 의미와 필수 규칙 — 기능 스펙 F1-3 「유형별 입력 필드」를 시트 필드에 대응시킨 표.
 *
 * | 유형 | primary | secondary | 시작 | 상세(접힘) |
 * |---|---|---|---|---|
 * | 프로젝트 | 역할 | 요약 | 필수 | 사용 기술 태그 + 성과·결과물 링크 |
 * | 수상 | 수상 등급 (필수) | 주관 기관 | 연도로 사용 | (없음) |
 * | 인턴 | 회사명 (필수) | 직무 (필수) | 필수 | 주요 업무 요약 |
 * | 대외활동 | 기관명 (필수) | 성과 요약 | 선택 | 역할 |
 * | 자격증 | 발급 기관 | (없음) | 취득 연월 | (없음) |
 *
 * 이 표가 `ExperienceDetails` 의 전 필드를 덮는다 — 시트가 모르는 필드는 이제 없다. 그래서 수정 저장은
 * 「원본에서 물려받기」 대신 **시트 왕복이 무손실**이라는 계약으로 지킨다(`Experience.toEditorState()` 참고).
 *
 * ### 유형을 바꾸면 이전 유형에만 있던 값은
 * **지우지 않고 시트에 그대로 두되, 저장할 때 새 유형이 쓰지 않는 값은 버린다.** 이미 [primary]·[secondary]
 * 가 그렇게 동작하고 있어 규칙을 두 벌로 만들지 않는다. 칩을 잘못 눌렀다가 되돌아온 사용자가 친 글을 잃지
 * 않는 쪽이기도 하다. 「저장 때 버린다」는 `ExperienceEditorState.toDraft()` 가 이 표만 보고 값을 읽어
 * 구조적으로 보장된다.
 */
public object ExperienceEditorRules {
    public const val MAX_TITLE_LENGTH: Int = 50
    public const val MAX_TEXT_LENGTH: Int = 100

    /**
     * 기술 태그 개수 상한.
     *
     * 관심 분야 태그(`MAX_PROFILE_TAGS` = 5)보다 넉넉하다 — 그쪽은 프로필 전체의 관심사라 5개면 충분하지만,
     * 프로젝트 하나의 스택은 언어·프레임워크·DI·네트워크·테스트로 쉽게 대여섯을 넘는다. 반면 Step 3 카드 목록이
     * 태그를 한 줄 흐름으로 그리므로(`OnboardingStep3Screen`) 카드가 태그 벽이 되지 않을 선이 필요하다.
     */
    public const val MAX_TECH_TAGS: Int = 10

    /** 기술 태그 한 개의 길이 상한. 가장 긴 축인 "Kotlin Multiplatform"(20자)이 들어가는 선. */
    public const val MAX_TECH_TAG_LENGTH: Int = 20

    /**
     * 성과·결과물 링크 길이 상한.
     *
     * GitHub·Notion·배포 주소는 100자 안쪽이다. 상한은 그보다 여유를 두되, 추적 파라미터가 잔뜩 붙은 주소가
     * 서버 `data` JSON 과 카드 목록에 통째로 실려 오는 것은 막는다.
     */
    public const val MAX_LINK_LENGTH: Int = 200

    public fun isStartDateRequired(type: ExperienceType): Boolean = type == ExperienceType.Project || type == ExperienceType.Intern

    public fun hasEndDate(type: ExperienceType): Boolean = type != ExperienceType.Award && type != ExperienceType.Certificate

    public fun isPrimaryRequired(type: ExperienceType): Boolean =
        type == ExperienceType.Award || type == ExperienceType.Intern || type == ExperienceType.Activity

    public fun hasSecondary(type: ExperienceType): Boolean = type != ExperienceType.Certificate

    public fun isSecondaryRequired(type: ExperienceType): Boolean = type == ExperienceType.Intern

    /** 사용 기술 태그를 받는 유형인가 — F1-3 표에서 프로젝트에만 있다. */
    public fun hasTechTags(type: ExperienceType): Boolean = type == ExperienceType.Project

    /** 성과·결과물 링크를 받는 유형인가 — F1-3 표에서 프로젝트에만 있다. */
    public fun hasLink(type: ExperienceType): Boolean = type == ExperienceType.Project

    /** 자유 서술 상세를 받는 유형인가 — 인턴은 주요 업무 요약, 대외활동은 역할. */
    public fun hasDetail(type: ExperienceType): Boolean = type == ExperienceType.Intern || type == ExperienceType.Activity

    /** 「자세히」 영역 자체를 그리는가. 수상·자격증은 상세 필드가 없어 접을 것도 없다. */
    public fun hasDetailSection(type: ExperienceType): Boolean = hasTechTags(type) || hasLink(type) || hasDetail(type)

    /** 관심 분야 태그와 같은 규칙으로 `#` 과 앞뒤 공백을 떼어 낸다. */
    public fun normalizeTechTag(raw: String): String = raw.trim().trimStart('#').trim()

    /**
     * 링크가 http/https 절대 주소인가.
     *
     * 서버가 받아 다시 사용자에게 보여 주는 값이라 스킴을 http·https 로 좁힌다 —
     * `javascript:`·`file:` 같은 스킴이 카드에 실려 돌아다니지 않게.
     */
    public fun isValidLink(value: String): Boolean {
        val trimmed = value.trim()
        if (trimmed.isEmpty() || trimmed.length > MAX_LINK_LENGTH) return false
        val uri = runCatching { URI(trimmed) }.getOrNull() ?: return false
        val scheme = uri.scheme?.lowercase() ?: return false
        if (scheme != "http" && scheme != "https") return false
        // 한글 도메인은 host 가 null 이고 authority 에만 남는다 — 둘 중 하나라도 있으면 주소로 본다.
        return !(uri.host ?: uri.authority).isNullOrBlank()
    }

    /** `YYYY.MM` 을 그 달 1일로 읽는다. 형식이 다르면 null. */
    public fun parseYearMonth(value: String): LocalDate? {
        val match = YEAR_MONTH.matchEntire(value.trim()) ?: return null
        val (year, month) = match.destructured
        return YearMonth.of(year.toInt(), month.toInt()).atDay(1)
    }

    private val YEAR_MONTH = Regex("""^(\d{4})\.(0[1-9]|1[0-2])$""")
}
