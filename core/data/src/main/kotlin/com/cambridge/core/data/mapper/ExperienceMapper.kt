package com.cambridge.core.data.mapper

import com.cambridge.core.model.experience.Experience
import com.cambridge.core.model.experience.ExperienceDetails
import com.cambridge.core.model.experience.ExperienceDraft
import com.cambridge.core.model.experience.ExperiencePoint
import com.cambridge.core.model.experience.ExperienceType
import com.cambridge.core.network.dto.ExperienceDto
import com.cambridge.core.network.dto.ExperienceRequestDto
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

/**
 * 경험 카드의 유형별 `data` 객체를 [ExperienceDetails] 와 상호 변환한다 — API_SPEC v0.1 §3.
 *
 * 키 이름은 명세의 프로젝트 예시(`role·techs·summary·link`)를 따르고, 나머지 유형은 기능 스펙 F1-3 의 필드를
 * camelCase 로 옮긴 가정이다. 서버가 확정되면 여기 한 곳만 고친다.
 *
 * 시점(`startDate`·`data.year`·`data.acquiredYearMonth`)은 정밀도 판정이 붙어 있어 [ExperiencePointWire]
 * 한 곳에 모아 뒀다 — 모델에서는 상세 필드가 아니라 기간이 시점의 정본이다(#207).
 */
internal object ExperienceMapper {
    fun toExperience(dto: ExperienceDto): Experience {
        val type =
            ExperienceType.fromWireValue(dto.type)
                ?: throw IllegalStateException("알 수 없는 경험 유형입니다: ${dto.type}")
        return Experience(
            id = dto.id,
            title = dto.title,
            startPoint = ExperiencePointWire.readStart(type, dto.data, dto.startDate),
            endPoint = ExperiencePointWire.readEnd(type, dto.endDate),
            details = toDetails(type, dto.data),
            createdAt = dto.createdAt?.let(WireTime::parseInstant),
        )
    }

    fun toRequest(draft: ExperienceDraft): ExperienceRequestDto =
        ExperienceRequestDto(
            type = draft.type.wireValue,
            title = draft.title,
            startDate = ExperiencePointWire.writeWireDate(draft.type, draft.startPoint),
            endDate = ExperiencePointWire.writeWireDate(draft.type, draft.endPoint),
            data = toData(draft.details, draft.startPoint),
        )

    private fun toDetails(
        type: ExperienceType,
        data: JsonObject,
    ): ExperienceDetails =
        when (type) {
            ExperienceType.Project -> {
                ExperienceDetails.Project(
                    role = data.stringOrNull("role"),
                    techs = data.stringsOrEmpty("techs"),
                    summary = data.stringOrNull("summary"),
                    link = data.stringOrNull("link"),
                )
            }

            ExperienceType.Award -> {
                ExperienceDetails.Award(
                    contestName = data.requireString("contestName"),
                    rank = data.requireString("rank"),
                    organizer = data.stringOrNull("organizer"),
                )
            }

            ExperienceType.Intern -> {
                ExperienceDetails.Intern(
                    company = data.requireString("company"),
                    role = data.requireString("role"),
                    summary = data.stringOrNull("summary"),
                )
            }

            ExperienceType.Activity -> {
                ExperienceDetails.Activity(
                    organization = data.requireString("organization"),
                    role = data.stringOrNull("role"),
                    summary = data.stringOrNull("summary"),
                )
            }

            ExperienceType.Certificate -> {
                ExperienceDetails.Certificate(issuer = data.stringOrNull("issuer"))
            }
        }

    /**
     * 상세 필드를 `data` 객체로 옮긴다.
     *
     * 수상의 `year` 와 자격증의 `acquiredYearMonth` 는 모델에서 [ExperienceDraft.startPoint] 로 옮겨 갔지만
     * **와이어 계약은 그대로**라, 시점에서 되돌려 적는다.
     */
    private fun toData(
        details: ExperienceDetails,
        startPoint: ExperiencePoint?,
    ): JsonObject =
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
                    ExperiencePointWire.writeAwardYear(startPoint)?.let { put("year", it) }
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
                    ExperiencePointWire.writeAcquiredYearMonth(startPoint)?.let { put("acquiredYearMonth", it) }
                }
            }
        }
}
