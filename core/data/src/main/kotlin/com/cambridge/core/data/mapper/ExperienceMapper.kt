package com.cambridge.core.data.mapper

import com.cambridge.core.model.experience.Experience
import com.cambridge.core.model.experience.ExperienceDetails
import com.cambridge.core.model.experience.ExperienceDraft
import com.cambridge.core.model.experience.ExperienceType
import com.cambridge.core.network.dto.ExperienceDto
import com.cambridge.core.network.dto.ExperienceRequestDto
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put

/**
 * 경험 카드의 유형별 `data` 객체를 [ExperienceDetails] 와 상호 변환한다 — API_SPEC v0.1 §3.
 *
 * 키 이름은 명세의 프로젝트 예시(`role·techs·summary·link`)를 따르고, 나머지 유형은 기능 스펙 F1-3 의 필드를
 * camelCase 로 옮긴 가정이다. 서버가 확정되면 여기 한 곳만 고친다.
 */
internal object ExperienceMapper {
    fun toExperience(dto: ExperienceDto): Experience {
        val type =
            ExperienceType.fromWireValue(dto.type)
                ?: throw IllegalStateException("알 수 없는 경험 유형입니다: ${dto.type}")
        return Experience(
            id = dto.id,
            title = dto.title,
            startDate = dto.startDate?.let(WireTime::parseDate),
            endDate = dto.endDate?.let(WireTime::parseDate),
            details = toDetails(type, dto.data),
            createdAt = dto.createdAt?.let(WireTime::parseInstant),
        )
    }

    fun toRequest(draft: ExperienceDraft): ExperienceRequestDto =
        ExperienceRequestDto(
            type = draft.type.wireValue,
            title = draft.title,
            startDate = draft.startDate?.let(WireTime::formatDate),
            endDate = draft.endDate?.let(WireTime::formatDate),
            data = toData(draft.details),
        )

    private fun toDetails(
        type: ExperienceType,
        data: JsonObject,
    ): ExperienceDetails =
        when (type) {
            ExperienceType.Project -> {
                ExperienceDetails.Project(
                    role = data.string("role"),
                    techs = data.strings("techs"),
                    summary = data.string("summary"),
                    link = data.string("link"),
                )
            }

            ExperienceType.Award -> {
                ExperienceDetails.Award(
                    contestName = data.requiredString("contestName"),
                    rank = data.requiredString("rank"),
                    year = data.int("year"),
                    organizer = data.string("organizer"),
                )
            }

            ExperienceType.Intern -> {
                ExperienceDetails.Intern(
                    company = data.requiredString("company"),
                    role = data.requiredString("role"),
                    summary = data.string("summary"),
                )
            }

            ExperienceType.Activity -> {
                ExperienceDetails.Activity(
                    organization = data.requiredString("organization"),
                    role = data.string("role"),
                    summary = data.string("summary"),
                )
            }

            ExperienceType.Certificate -> {
                ExperienceDetails.Certificate(
                    issuer = data.string("issuer"),
                    acquiredYearMonth = data.string("acquiredYearMonth"),
                )
            }
        }

    private fun toData(details: ExperienceDetails): JsonObject =
        buildJsonObject {
            when (details) {
                is ExperienceDetails.Project -> {
                    details.role?.let { put("role", it) }
                    put("techs", JsonArray(details.techs.map(::JsonPrimitive)))
                    details.summary?.let { put("summary", it) }
                    details.link?.let { put("link", it) }
                }

                is ExperienceDetails.Award -> {
                    put("contestName", details.contestName)
                    put("rank", details.rank)
                    details.year?.let { put("year", it) }
                    details.organizer?.let { put("organizer", it) }
                }

                is ExperienceDetails.Intern -> {
                    put("company", details.company)
                    put("role", details.role)
                    details.summary?.let { put("summary", it) }
                }

                is ExperienceDetails.Activity -> {
                    put("organization", details.organization)
                    details.role?.let { put("role", it) }
                    details.summary?.let { put("summary", it) }
                }

                is ExperienceDetails.Certificate -> {
                    details.issuer?.let { put("issuer", it) }
                    details.acquiredYearMonth?.let { put("acquiredYearMonth", it) }
                }
            }
        }

    private fun JsonObject.string(key: String): String? =
        (this[key]?.takeUnless { it is JsonNull } as? JsonPrimitive)?.contentOrNull?.takeIf(String::isNotBlank)

    private fun JsonObject.requiredString(key: String): String = string(key) ?: throw IllegalStateException("경험 데이터에 '$key' 가 없습니다.")

    private fun JsonObject.int(key: String): Int? = (this[key]?.takeUnless { it is JsonNull } as? JsonPrimitive)?.intOrNull

    private fun JsonObject.strings(key: String): List<String> =
        (this[key]?.takeUnless { it is JsonNull } as? JsonArray)
            ?.jsonArray
            ?.map { it.jsonPrimitive.content }
            ?.filter(String::isNotBlank)
            ?.distinct()
            .orEmpty()
}
