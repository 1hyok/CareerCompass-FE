package com.cambridge.core.data.mapper

import com.cambridge.core.model.posting.Posting
import com.cambridge.core.model.posting.PostingBoardRef
import com.cambridge.core.model.posting.PostingDetail
import com.cambridge.core.model.posting.PostingFormQuestion
import com.cambridge.core.model.posting.PostingParsed
import com.cambridge.core.model.posting.PostingQualifications
import com.cambridge.core.model.posting.PostingType
import com.cambridge.core.model.posting.Suitability
import com.cambridge.core.model.posting.SuitabilityAxis
import com.cambridge.core.model.posting.SuitabilityAxisKind
import com.cambridge.core.model.posting.SuitabilityLabel
import com.cambridge.core.network.dto.PostingBoardDto
import com.cambridge.core.network.dto.PostingDetailDto
import com.cambridge.core.network.dto.PostingDto
import com.cambridge.core.network.dto.PostingParsedDto
import com.cambridge.core.network.dto.SuitabilityDto

/**
 * 공고 wire → 도메인 변환.
 *
 * 값 확장에는 관대하다 — 알 수 없는 `type` 은 [PostingType.Other], 알 수 없는 `scoreLabel` 은 점수에서 다시 계산,
 * 알 수 없는 분석 축은 제외한다. 키 누락은 DTO 파싱 단계에서 이미 실패한다.
 */
internal object PostingMapper {
    fun toPosting(dto: PostingDto): Posting {
        val score = dto.score
        return Posting(
            id = dto.id,
            title = dto.title,
            type = toType(dto.type),
            board = toBoardRef(dto.board),
            dueDate = dto.dueDate?.let(WireTime::parseDate),
            collectedAt = WireTime.parseInstant(dto.collectedAt),
            score = score,
            scoreLabel = score?.let { toLabel(dto.scoreLabel, it) },
            isRead = dto.isRead,
            isBookmarked = dto.isBookmarked,
        )
    }

    fun toDetail(dto: PostingDetailDto): PostingDetail =
        PostingDetail(
            id = dto.id,
            title = dto.title,
            type = toType(dto.type),
            board = toBoardRef(dto.board),
            url = dto.url,
            rawContent = dto.rawContent,
            dueDate = dto.dueDate?.let(WireTime::parseDate),
            collectedAt = WireTime.parseInstant(dto.collectedAt),
            isRead = dto.isRead,
            isBookmarked = dto.isBookmarked,
            parsed = dto.parsed?.let(::toParsed),
            suitability = dto.suitability?.let(::toSuitability),
            similar = dto.similar.map(::toPosting),
        )

    fun toType(wireValue: String): PostingType = PostingType.fromWireValue(wireValue) ?: PostingType.Other

    private fun toBoardRef(dto: PostingBoardDto): PostingBoardRef = PostingBoardRef(id = dto.id, name = dto.name)

    private fun toLabel(
        wireValue: String?,
        score: Int,
    ): SuitabilityLabel = wireValue?.let(SuitabilityLabel::fromWireValue) ?: SuitabilityLabel.fromScore(score)

    private fun toParsed(dto: PostingParsedDto): PostingParsed =
        PostingParsed(
            keywords = dto.keywords.filter(String::isNotBlank).distinct(),
            qualifications =
                PostingQualifications(
                    year = dto.qualifications.year?.takeIf(String::isNotBlank),
                    gpa = dto.qualifications.gpa?.takeIf(String::isNotBlank),
                ),
            preferences = dto.preferences.filter(String::isNotBlank),
            formQuestions =
                dto.formQuestions
                    .filter { it.question.isNotBlank() }
                    .map {
                        PostingFormQuestion(
                            order = it.order,
                            question = it.question,
                            maxChars =
                                it.maxChars?.takeIf { max ->
                                    max > 0
                                },
                        )
                    }.sortedBy(PostingFormQuestion::order),
        )

    private fun toSuitability(dto: SuitabilityDto): Suitability =
        Suitability(
            score = dto.score,
            label = toLabel(dto.label, dto.score),
            breakdown =
                dto.breakdown
                    .mapNotNull { axis ->
                        SuitabilityAxisKind.fromWireValue(axis.axis)?.let { kind ->
                            SuitabilityAxis(kind = kind, score = axis.score, weight = axis.weight)
                        }
                    }.distinctBy(SuitabilityAxis::kind),
            strengthComment = dto.strengthComment?.takeIf(String::isNotBlank),
            weaknessComment = dto.weaknessComment?.takeIf(String::isNotBlank),
        )
}
