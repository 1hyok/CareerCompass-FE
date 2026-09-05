package com.careercompass.feature.profile.domain

import com.careercompass.core.domain.error.CoreDataFailure
import com.careercompass.core.model.application.PastApplication
import com.careercompass.core.model.application.PastApplicationCategory
import com.careercompass.core.model.application.PastApplicationItem
import com.careercompass.core.model.application.UploadFile
import com.careercompass.core.model.experience.Experience
import com.careercompass.core.model.experience.ExperienceDetails
import com.careercompass.core.model.experience.ExperienceDraft
import com.careercompass.core.model.experience.ExperiencePoint
import com.careercompass.core.model.user.JobInterest
import com.careercompass.core.model.user.UserProfile
import java.io.ByteArrayInputStream
import java.io.IOException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import java.time.LocalDate

internal fun profile(
    id: Long = 1L,
    name: String? = "정일혁",
    completion: Int = 78,
    onboardingDone: Boolean = true,
): UserProfile =
    UserProfile(
        id = id,
        name = name,
        school = "건국대학교",
        department = "컴퓨터공학부",
        gpa = 3.87,
        gradYear = 2027,
        jobInterests = listOf(JobInterest(code = "backend", priority = 1)),
        tags = listOf("AI"),
        onboardingDone = onboardingDone,
        completion = completion,
    )

internal fun projectDraft(title: String = "CareerCompass"): ExperienceDraft =
    ExperienceDraft(
        title = title,
        startPoint = ExperiencePoint.Date(LocalDate.of(2025, 9, 1)),
        endPoint = null,
        details =
            ExperienceDetails.Project(
                role = "안드로이드",
                techs = listOf("Kotlin", "Compose"),
                summary = "공고 자동 분석 서비스",
                link = "https://github.com/Team-CareerCompass/CareerCompass-FE",
            ),
    )

internal fun experience(
    id: Long,
    title: String = "경험 $id",
): Experience =
    Experience(
        id = id,
        title = title,
        startPoint = ExperiencePoint.Date(LocalDate.of(2025, 9, 1)),
        endPoint = null,
        details = ExperienceDetails.Project(role = "안드로이드", techs = listOf("Kotlin"), summary = null, link = null),
        createdAt = null,
    )

internal fun pastApplication(
    id: Long,
    label: String = "지원서 $id",
): PastApplication =
    PastApplication(
        id = id,
        label = label,
        items =
            listOf(
                PastApplicationItem(id = id * 10, category = PastApplicationCategory.Motivation, content = "지원 동기", confident = true),
            ),
        createdAt = null,
    )

internal fun uploadFile(fileName: String = "2024_카카오_인턴.pdf"): UploadFile =
    UploadFile(fileName = fileName, sizeBytes = 16L) { ByteArrayInputStream(ByteArray(16)) }

/**
 * 이슈 #174 가 고정하라고 못 박은 실패 네 갈래 — 401 · 422 `LIMIT_EXCEEDED` · 503 · 네트워크 끊김.
 *
 * 각 use case 가 이 값들을 **그대로** 흘려보내는지가 판정 대상이다. 도중에 감싸거나 삼키면 화면이
 * `when` 으로 가를 근거를 잃는다.
 */
internal enum class ServerFailure(
    val create: () -> Throwable,
) {
    Unauthorized({ CoreDataFailure.Unauthorized(code = "AUTH_REQUIRED", cause = IOException("401")) }),
    LimitExceeded({ CoreDataFailure.LimitExceeded(code = "LIMIT_EXCEEDED", cause = IOException("422")) }),
    ServiceUnavailable({ CoreDataFailure.ServiceUnavailable(code = "LLM_UNAVAILABLE", cause = IOException("503")) }),
    NetworkUnavailable({ CoreDataFailure.NetworkUnavailable(UnknownHostException("api.careercompass")) }),
    Timeout({ CoreDataFailure.NetworkUnavailable(SocketTimeoutException("read timed out")) }),
    ;

    fun <T> asResult(): Result<T> = Result.failure(create())
}
