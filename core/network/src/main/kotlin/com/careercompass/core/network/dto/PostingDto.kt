package com.careercompass.core.network.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** API_SPEC v0.1 §5 `GET /postings` 응답. */
@Serializable
data class PostingListDto(
    @SerialName("postings")
    val postings: List<PostingDto>,
    @SerialName("nextCursor")
    val nextCursor: String? = null,
)

@Serializable
data class PostingBoardDto(
    @SerialName("id")
    val id: Long,
    @SerialName("name")
    val name: String,
)

/** 공고 목록 항목. `score`·`scoreLabel` 은 파싱 전이면 없다. */
@Serializable
data class PostingDto(
    @SerialName("id")
    val id: Long,
    @SerialName("title")
    val title: String,
    @SerialName("type")
    val type: String,
    @SerialName("board")
    val board: PostingBoardDto,
    @SerialName("dueDate")
    val dueDate: String? = null,
    @SerialName("collectedAt")
    val collectedAt: String,
    @SerialName("score")
    val score: Int? = null,
    @SerialName("scoreLabel")
    val scoreLabel: String? = null,
    @SerialName("isRead")
    val isRead: Boolean,
    @SerialName("isBookmarked")
    val isBookmarked: Boolean,
)

@Serializable
data class PostingQualificationsDto(
    @SerialName("year")
    val year: String? = null,
    @SerialName("gpa")
    val gpa: String? = null,
)

@Serializable
data class PostingFormQuestionDto(
    @SerialName("order")
    val order: Int,
    @SerialName("question")
    val question: String,
    @SerialName("maxChars")
    val maxChars: Int? = null,
)

@Serializable
data class PostingParsedDto(
    @SerialName("keywords")
    val keywords: List<String>,
    @SerialName("qualifications")
    val qualifications: PostingQualificationsDto,
    @SerialName("preferences")
    val preferences: List<String>,
    @SerialName("formQuestions")
    val formQuestions: List<PostingFormQuestionDto>,
)

@Serializable
data class SuitabilityAxisDto(
    @SerialName("axis")
    val axis: String,
    @SerialName("score")
    val score: Int,
    @SerialName("weight")
    val weight: Int,
)

@Serializable
data class SuitabilityDto(
    @SerialName("score")
    val score: Int,
    @SerialName("label")
    val label: String,
    @SerialName("breakdown")
    val breakdown: List<SuitabilityAxisDto>,
    @SerialName("strengthComment")
    val strengthComment: String? = null,
    @SerialName("weaknessComment")
    val weaknessComment: String? = null,
)

/**
 * `GET /postings/{id}` 응답. 명세 예시는 `title·rawContent·url·parsed·suitability·similar` 만 보여 주지만
 * 목록 항목이 갖는 `type·board·dueDate·collectedAt·isRead·isBookmarked` 도 상세에 실린다고 가정한다.
 * `parsed`·`suitability` 는 파싱 완료 전이면 없다.
 */
@Serializable
data class PostingDetailDto(
    @SerialName("id")
    val id: Long,
    @SerialName("title")
    val title: String,
    @SerialName("type")
    val type: String,
    @SerialName("board")
    val board: PostingBoardDto,
    @SerialName("rawContent")
    val rawContent: String,
    @SerialName("url")
    val url: String,
    @SerialName("dueDate")
    val dueDate: String? = null,
    @SerialName("collectedAt")
    val collectedAt: String,
    @SerialName("isRead")
    val isRead: Boolean,
    @SerialName("isBookmarked")
    val isBookmarked: Boolean,
    @SerialName("parsed")
    val parsed: PostingParsedDto? = null,
    @SerialName("suitability")
    val suitability: SuitabilityDto? = null,
    @SerialName("similar")
    val similar: List<PostingDto>,
)
