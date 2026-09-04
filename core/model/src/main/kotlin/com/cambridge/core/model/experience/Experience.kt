package com.cambridge.core.model.experience

import java.net.URI
import java.time.Instant

/** 경험 카드 등록 상한 — 기능 스펙 F1-3 (최대 30개). */
public const val MAX_EXPERIENCE_CARDS: Int = 30

/**
 * 프로젝트 카드 하나가 가질 수 있는 기술 태그 개수 상한.
 *
 * 관심 분야 태그(`MAX_PROFILE_TAGS` = 5)보다 넉넉하다 — 그쪽은 프로필 전체의 관심사라 5개면 충분하지만,
 * 프로젝트 하나의 스택은 언어·프레임워크·DI·네트워크·테스트로 쉽게 대여섯을 넘는다. 반면 Step 3 카드 목록이
 * 태그를 한 줄 흐름으로 그리므로(`OnboardingStep3Screen`) 카드가 태그 벽이 되지 않을 선이 필요하다.
 */
public const val MAX_EXPERIENCE_TECH_TAGS: Int = 10

/** 기술 태그 한 개의 길이 상한. 가장 긴 축인 "Kotlin Multiplatform"(20자)이 들어가는 선. */
public const val MAX_EXPERIENCE_TECH_TAG_LENGTH: Int = 20

/**
 * 성과·결과물 링크 길이 상한.
 *
 * GitHub·Notion·배포 주소는 100자 안쪽이다. 상한은 그보다 여유를 두되, 추적 파라미터가 잔뜩 붙은 주소가
 * 서버 `data` JSON 과 카드 목록에 통째로 실려 오는 것은 막는다.
 */
public const val MAX_EXPERIENCE_LINK_LENGTH: Int = 200

/**
 * 성과·결과물 링크가 카드에 실릴 수 있는 값인가 — 길이와 **스킴**을 함께 본다.
 *
 * 스킴을 화면이 아니라 모델에서 보는 이유는, 이 값이 서버를 거쳐 **다시 사용자에게 보이고 눌리는** 값이기
 * 때문이다. `javascript:`·`file:` 같은 스킴이 카드에 실려 돌아다니지 않게 http·https 절대 주소로 좁힌다.
 * 판정이 화면에만 있으면 입력 경로가 늘 때마다 한 벌씩 늘고, 두 벌은 반드시 어긋난다(#208).
 */
public fun isAllowedExperienceLink(value: String): Boolean {
    val trimmed = value.trim()
    if (trimmed.isEmpty() || trimmed.length > MAX_EXPERIENCE_LINK_LENGTH) return false
    val uri = runCatching { URI(trimmed) }.getOrNull() ?: return false
    val scheme = uri.scheme?.lowercase() ?: return false
    if (scheme != "http" && scheme != "https") return false
    // 한글 도메인은 host 가 null 이고 authority 에만 남는다 — 둘 중 하나라도 있으면 주소로 본다.
    return !(uri.host ?: uri.authority).isNullOrBlank()
}

/** 경험 유형 — API_SPEC v0.1 §3 `type`. */
public enum class ExperienceType(
    public val wireValue: String,
) {
    Project("project"),
    Award("award"),
    Intern("intern"),
    Activity("activity"),
    Certificate("cert"),
    ;

    /**
     * 기간(시작~종료)을 갖는 유형인가 — 수상·자격증은 기간이 아니라 **시점 하나**를 갖는다(F1-3).
     *
     * 화면 계약이 아니라 모델에 두는 이유는 「수상 카드에 종료 시점이 있다」가 데이터로 성립하면 안 되기
     * 때문이다. 시트도 이 값을 참조한다(`ExperienceEditorRules.hasPeriod`).
     */
    public val hasPeriod: Boolean get() = this != Award && this != Certificate

    /**
     * 시작 시점이 필수인 유형인가 — 프로젝트·인턴은 기간이 곧 카드의 뼈대다(F1-3).
     *
     * [ExperienceDraft] 에만 건다. 이미 서버에 있는 카드([Experience])까지 막으면 우리가 못 고치는 값
     * 때문에 목록 전체가 못 열린다.
     */
    public val requiresStartPoint: Boolean get() = this == Project || this == Intern

    /**
     * 이 유형이 담을 수 있는 **가장 굵은** 정밀도(하한).
     *
     * 연도만 남은 자격증 카드는 「취득 연월」이라는 필드의 뜻을 채우지 못한다. 프로젝트·인턴·대외활동도
     * 시트가 최소 `YYYY.MM` 을 받으므로 연도만 아는 기간은 어느 경로로도 생기지 않는다.
     *
     * ### 프로젝트·인턴의 하한을 「날짜」로 두지 않은 이유
     * 이슈 #207 은 「프로젝트·인턴=날짜」로 적었지만, 그것을 하한으로 걸면 시트가 담지 못하는 일을
     * **지어내야** 카드를 만들 수 있다 — 이 이슈가 없애려는 넓히기를 모델이 강제하는 셈이다. 시점 칸이
     * `YYYY.MM` 인 근거는 `ExperienceEditorRules` KDoc 에 있다. 그래서 날짜는 [maxPointPrecision]
     * 으로 두고, 서버가 준 일이나 사용자가 손대지 않은 일은 그대로 실려 다닌다(#171).
     */
    public val minPointPrecision: ExperiencePrecision
        get() = if (this == Award) ExperiencePrecision.Year else ExperiencePrecision.YearMonth

    /**
     * 이 유형이 담을 수 있는 **가장 자세한** 정밀도(상한) — 「없던 정밀도」를 갖지 못하게 하는 벽이다.
     *
     * 수상은 연도까지다(#166). 서버가 `startDate: "2025-03-02"` 를 줘도 모델에는 `2025` 만 남는다 —
     * 수상의 정본은 `data.year` 이고, 공통 `startDate` 칸의 월·일은 그 유형에서 뜻을 갖지 않는다.
     * 자격증은 같은 이유로 연월까지다. 좁히는 것은 매퍼가 하고(`ExperiencePointWire`), 여기서는 그 결과가
     * 지켜졌는지만 본다.
     */
    public val maxPointPrecision: ExperiencePrecision
        get() =
            when (this) {
                Award -> ExperiencePrecision.Year
                Certificate -> ExperiencePrecision.YearMonth
                Project, Intern, Activity -> ExperiencePrecision.Date
            }

    public companion object {
        public fun fromWireValue(value: String): ExperienceType? = entries.firstOrNull { it.wireValue == value }
    }
}

/**
 * 유형별 입력 필드 — 기능 스펙 F1-3 「유형별 입력 필드」.
 *
 * 제목(프로젝트명·활동명·자격증명)은 [Experience.title] 이 갖고, 시점은 [Experience.startPoint]·
 * [Experience.endPoint] 가 갖는다.
 *
 * ### 시점이 상세 필드에 없는 이유 (#207)
 * 예전에는 수상의 `year`(Int)·자격증의 `acquiredYearMonth`(문자열 + 정규식)가 여기 있었다. 같은 사실이
 * 두 자리에 나뉘어 있으면 반드시 어긋나고, 어긋난 뒤에는 어느 쪽이 사실인지 알 수 없다. 지금은 정밀도를
 * [ExperiencePoint] 가 알므로 시점의 정본은 **기간 쪽 한 곳**이다. 와이어의 `year`·`acquiredYearMonth`
 * 키는 그대로 쓴다 — 서버 계약은 그대로 두고 모델만 모았다(`ExperienceMapper`).
 */
public sealed interface ExperienceDetails {
    public val type: ExperienceType

    /**
     * 프로젝트: 사용 기술(태그), 역할, 성과/결과물 링크.
     *
     * ### 상한이 왜 화면이 아니라 여기 있는가 (#208)
     * 태그 개수·길이와 링크 형식은 「사용자가 시트에서 지켜야 하는 규칙」이 아니라 **이 카드가 성립하는
     * 조건**이다. 화면에만 두면 입력 경로가 늘 때마다 한 벌씩 늘고(마이 탭 편집 #179), 두 벌은 반드시
     * 어긋난다. `MAX_EXPERIENCE_CARDS`·`MAX_PROFILE_TAGS` 가 이미 모델에 있는 것과 같은 자리다.
     *
     * 서버가 상한을 넘는 값을 주면 매퍼가 담기 전에 걸러 낸다(`ExperienceMapper`) — 우리가 못 고치는
     * 값으로 `require` 를 깨뜨려 목록 전체를 못 열게 만들지 않는다.
     */
    public data class Project(
        val role: String?,
        val techs: List<String>,
        val summary: String?,
        val link: String?,
    ) : ExperienceDetails {
        override val type: ExperienceType get() = ExperienceType.Project

        init {
            require(role == null || role.isNotBlank()) { "role must be null or non-blank" }
            require(techs.all(String::isNotBlank)) { "techs must not be blank" }
            require(techs.distinct().size == techs.size) { "techs must be unique" }
            require(techs.size <= MAX_EXPERIENCE_TECH_TAGS) { "techs must be at most $MAX_EXPERIENCE_TECH_TAGS" }
            require(techs.all { it.length <= MAX_EXPERIENCE_TECH_TAG_LENGTH }) {
                "each tech must be at most $MAX_EXPERIENCE_TECH_TAG_LENGTH characters"
            }
            require(summary == null || summary.isNotBlank()) { "summary must be null or non-blank" }
            require(link == null || isAllowedExperienceLink(link)) {
                "link must be null or an http(s) address within $MAX_EXPERIENCE_LINK_LENGTH characters"
            }
        }
    }

    /** 수상 이력: 공모전명·수상 등급 필수, 주관 기관. 수상 연도는 [Experience.startPoint] 가 갖는다. */
    public data class Award(
        val contestName: String,
        val rank: String,
        val organizer: String?,
    ) : ExperienceDetails {
        override val type: ExperienceType get() = ExperienceType.Award

        init {
            require(contestName.isNotBlank()) { "contestName must not be blank" }
            require(rank.isNotBlank()) { "rank must not be blank" }
            require(organizer == null || organizer.isNotBlank()) { "organizer must be null or non-blank" }
        }
    }

    /** 인턴·직무 경험: 회사명·직무 필수, 주요 업무 요약. */
    public data class Intern(
        val company: String,
        val role: String,
        val summary: String?,
    ) : ExperienceDetails {
        override val type: ExperienceType get() = ExperienceType.Intern

        init {
            require(company.isNotBlank()) { "company must not be blank" }
            require(role.isNotBlank()) { "role must not be blank" }
            require(summary == null || summary.isNotBlank()) { "summary must be null or non-blank" }
        }
    }

    /** 대외활동: 기관명 필수, 역할·성과 요약. */
    public data class Activity(
        val organization: String,
        val role: String?,
        val summary: String?,
    ) : ExperienceDetails {
        override val type: ExperienceType get() = ExperienceType.Activity

        init {
            require(organization.isNotBlank()) { "organization must not be blank" }
            require(role == null || role.isNotBlank()) { "role must be null or non-blank" }
            require(summary == null || summary.isNotBlank()) { "summary must be null or non-blank" }
        }
    }

    /** 자격증: 발급 기관. 취득 연월은 [Experience.startPoint] 가 갖는다. */
    public data class Certificate(
        val issuer: String?,
    ) : ExperienceDetails {
        override val type: ExperienceType get() = ExperienceType.Certificate

        init {
            require(issuer == null || issuer.isNotBlank()) { "issuer must be null or non-blank" }
        }
    }
}

/**
 * 경험 카드 생성·수정 입력. 기간 필수 여부는 유형별 규칙(F1-3)을 따른다.
 *
 * @property startPoint 시작 시점. 기간이 없는 유형(수상·자격증)은 **그 유형의 시점 하나**가 여기 온다.
 * @property endPoint 종료 시점. 기간이 있는 유형만 가질 수 있다.
 */
public data class ExperienceDraft(
    val title: String,
    val startPoint: ExperiencePoint?,
    val endPoint: ExperiencePoint?,
    val details: ExperienceDetails,
) {
    init {
        require(title.isNotBlank()) { "title must not be blank" }
        requireValidExperiencePeriod(details.type, startPoint, endPoint)
        require(!details.type.requiresStartPoint || startPoint != null) { "${details.type} requires startPoint" }
    }

    public val type: ExperienceType get() = details.type
}

/**
 * 등록된 경험 카드.
 *
 * 시작 시점 필수 규칙은 [ExperienceDraft] 에만 있다 — 서버가 준 카드는 우리가 못 고치므로, 시작 시점이
 * 빠졌다고 목록 전체를 못 열게 만들지 않는다. 정밀도 상한은 매퍼가 좁혀서 넣는다(`ExperienceMapper`).
 */
public data class Experience(
    val id: Long,
    val title: String,
    val startPoint: ExperiencePoint?,
    val endPoint: ExperiencePoint?,
    val details: ExperienceDetails,
    val createdAt: Instant?,
) {
    init {
        require(title.isNotBlank()) { "title must not be blank" }
        requireValidExperiencePeriod(details.type, startPoint, endPoint)
    }

    public val type: ExperienceType get() = details.type
}

/**
 * 유형이 담을 수 있는 시점인지 본다 — [Experience] 와 [ExperienceDraft] 가 같은 계약을 쓰게 한 곳에 둔다.
 *
 * 「없던 정밀도가 생기지 않는다」(#166)를 여기서 데이터로 못 박는다. 수상 카드는 월·일을 **가질 수 없고**,
 * 자격증 카드는 일을 가질 수 없다. 시트가 실수해도 모델이 받지 않는다.
 */
private fun requireValidExperiencePeriod(
    type: ExperienceType,
    startPoint: ExperiencePoint?,
    endPoint: ExperiencePoint?,
) {
    startPoint?.let { requireAllowedPrecision(type, it, "startPoint") }
    endPoint?.let { requireAllowedPrecision(type, it, "endPoint") }
    require(endPoint == null || type.hasPeriod) { "$type has a single point, not a period" }
    require(endPoint == null || startPoint != null) { "endPoint requires startPoint" }
    require(startPoint == null || endPoint == null || !endPoint.isBefore(startPoint)) { "endPoint must not be before startPoint" }
}

private fun requireAllowedPrecision(
    type: ExperienceType,
    point: ExperiencePoint,
    field: String,
) {
    require(point.precision >= type.minPointPrecision && point.precision <= type.maxPointPrecision) {
        "$field precision ${point.precision} is not allowed for $type " +
            "(${type.minPointPrecision}..${type.maxPointPrecision})"
    }
}
