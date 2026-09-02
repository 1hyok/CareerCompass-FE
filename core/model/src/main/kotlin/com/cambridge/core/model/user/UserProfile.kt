package com.cambridge.core.model.user

/** 희망 직무 상한 — API_SPEC v0.1 §2 `PUT /users/me/job-interests` (최소 1, 최대 3). */
public const val MAX_JOB_INTERESTS: Int = 3

/** 관심 분야 태그 상한 — API_SPEC v0.1 §2 `PUT /users/me/tags` (최소 1, 최대 5). */
public const val MAX_PROFILE_TAGS: Int = 5

/** 학점 만점 — 기능 스펙 F1-2 Step 1 (0.0 ~ 4.5). */
public const val MAX_GRADE_POINT_AVERAGE: Double = 4.5

/** 최소 졸업 예정 연도 — 기능 스펙 F1-2 Step 1 (2000~). */
public const val MIN_GRADUATION_YEAR: Int = 2000

/**
 * 희망 직무 한 건.
 *
 * @property code 서비스 직무 목록의 코드(예: `backend`).
 * @property priority 1 이 가장 우선.
 */
public data class JobInterest(
    val code: String,
    val priority: Int,
) {
    init {
        require(code.isNotBlank()) { "code must not be blank" }
        require(priority >= 1) { "priority must be at least 1" }
    }
}

/**
 * `GET /users/me` 가 돌려주는 내 프로필.
 *
 * @property completion 프로필 완성도(0~100).
 */
public data class UserProfile(
    val id: Long,
    val name: String?,
    val school: String?,
    val department: String?,
    val gpa: Double?,
    val gradYear: Int?,
    val jobInterests: List<JobInterest>,
    val tags: List<String>,
    val onboardingDone: Boolean,
    val completion: Int,
) {
    init {
        require(name == null || name.isNotBlank()) { "name must be null or non-blank" }
        require(school == null || school.isNotBlank()) { "school must be null or non-blank" }
        require(department == null || department.isNotBlank()) { "department must be null or non-blank" }
        require(gpa == null || gpa in 0.0..MAX_GRADE_POINT_AVERAGE) { "gpa must be within 0.0..$MAX_GRADE_POINT_AVERAGE" }
        require(gradYear == null || gradYear >= MIN_GRADUATION_YEAR) { "gradYear must be at least $MIN_GRADUATION_YEAR" }
        require(jobInterests.map(JobInterest::code).distinct().size == jobInterests.size) { "job interest codes must be unique" }
        require(tags.all(String::isNotBlank)) { "tags must not be blank" }
        require(tags.distinct().size == tags.size) { "tags must be unique" }
        require(completion in 0..100) { "completion must be within 0..100" }
    }
}

/** `PATCH /users/me` 부분 수정 — null 인 필드는 보내지 않는다. */
public data class UserProfileUpdate(
    val name: String? = null,
    val school: String? = null,
    val department: String? = null,
    val gpa: Double? = null,
    val gradYear: Int? = null,
) {
    init {
        require(name == null || name.isNotBlank()) { "name must be null or non-blank" }
        require(school == null || school.isNotBlank()) { "school must be null or non-blank" }
        require(department == null || department.isNotBlank()) { "department must be null or non-blank" }
        require(gpa == null || gpa in 0.0..MAX_GRADE_POINT_AVERAGE) { "gpa must be within 0.0..$MAX_GRADE_POINT_AVERAGE" }
        require(gradYear == null || gradYear >= MIN_GRADUATION_YEAR) { "gradYear must be at least $MIN_GRADUATION_YEAR" }
    }

    /** 보낼 필드가 하나도 없는 수정은 요청 자체가 의미 없다. */
    public val isEmpty: Boolean
        get() = name == null && school == null && department == null && gpa == null && gradYear == null
}
