package com.cambridge.core.model.experience

import java.time.Instant
import java.time.LocalDate

/** 경험 카드 등록 상한 — 기능 스펙 F1-3 (최대 30개). */
public const val MAX_EXPERIENCE_CARDS: Int = 30

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

    public companion object {
        public fun fromWireValue(value: String): ExperienceType? = entries.firstOrNull { it.wireValue == value }
    }
}

/**
 * 유형별 입력 필드 — 기능 스펙 F1-3 「유형별 입력 필드」.
 *
 * 제목(프로젝트명·활동명·자격증명)은 [Experience.title] 이 갖고, 기간은 [Experience.startDate]·[Experience.endDate] 가 갖는다.
 */
public sealed interface ExperienceDetails {
    public val type: ExperienceType

    /** 프로젝트: 사용 기술(태그), 역할, 성과/결과물 링크. */
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
            require(summary == null || summary.isNotBlank()) { "summary must be null or non-blank" }
            require(link == null || link.isNotBlank()) { "link must be null or non-blank" }
        }
    }

    /** 수상 이력: 공모전명·수상 등급 필수, 연도·주관 기관. */
    public data class Award(
        val contestName: String,
        val rank: String,
        val year: Int?,
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

    /** 자격증: 발급 기관·취득 연월(YYYY-MM). */
    public data class Certificate(
        val issuer: String?,
        val acquiredYearMonth: String?,
    ) : ExperienceDetails {
        override val type: ExperienceType get() = ExperienceType.Certificate

        init {
            require(issuer == null || issuer.isNotBlank()) { "issuer must be null or non-blank" }
            require(acquiredYearMonth == null || YEAR_MONTH.matches(acquiredYearMonth)) {
                "acquiredYearMonth must be null or YYYY-MM"
            }
        }

        private companion object {
            val YEAR_MONTH = Regex("""^\d{4}-(0[1-9]|1[0-2])$""")
        }
    }
}

/** 경험 카드 생성·수정 입력. 기간 필수 여부는 유형별 규칙(F1-3)을 따른다. */
public data class ExperienceDraft(
    val title: String,
    val startDate: LocalDate?,
    val endDate: LocalDate?,
    val details: ExperienceDetails,
) {
    init {
        require(title.isNotBlank()) { "title must not be blank" }
        require(startDate == null || endDate == null || !endDate.isBefore(startDate)) { "endDate must not be before startDate" }
        val requiresPeriod = details is ExperienceDetails.Project || details is ExperienceDetails.Intern
        require(!requiresPeriod || startDate != null) { "${details.type} requires startDate" }
    }

    public val type: ExperienceType get() = details.type
}

/** 등록된 경험 카드. */
public data class Experience(
    val id: Long,
    val title: String,
    val startDate: LocalDate?,
    val endDate: LocalDate?,
    val details: ExperienceDetails,
    val createdAt: Instant?,
) {
    init {
        require(title.isNotBlank()) { "title must not be blank" }
        require(startDate == null || endDate == null || !endDate.isBefore(startDate)) { "endDate must not be before startDate" }
    }

    public val type: ExperienceType get() = details.type
}
