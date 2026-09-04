package com.careercompass.core.data.mapper

import com.careercompass.core.model.posting.PostingType
import com.careercompass.core.model.posting.SuitabilityAxisKind
import com.careercompass.core.model.posting.SuitabilityLabel
import com.careercompass.core.network.dto.PostingBoardDto
import com.careercompass.core.network.dto.PostingDetailDto
import com.careercompass.core.network.dto.PostingDto
import com.careercompass.core.network.dto.PostingFormQuestionDto
import com.careercompass.core.network.dto.PostingParsedDto
import com.careercompass.core.network.dto.PostingQualificationsDto
import com.careercompass.core.network.dto.SuitabilityAxisDto
import com.careercompass.core.network.dto.SuitabilityDto
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Test
import java.time.Instant
import java.time.LocalDate

class PostingMapperTest {
    private fun dto(
        type: String = "recruit",
        score: Int? = 88,
        scoreLabel: String? = "very_suitable",
        collectedAt: String = "2026-05-18T07:00:00+09:00",
    ) = PostingDto(
        id = 101,
        title = "2026 카카오 SW 인턴십",
        type = type,
        board = PostingBoardDto(3, "공식 채용"),
        dueDate = "2026-05-25",
        collectedAt = collectedAt,
        score = score,
        scoreLabel = scoreLabel,
        isRead = false,
        isBookmarked = true,
    )

    @Test
    fun `목록 항목을 도메인으로 옮기며 시각은 UTC Instant 로 정규화한다`() {
        val posting = PostingMapper.toPosting(dto())

        assertEquals(PostingType.Recruit, posting.type)
        assertEquals(LocalDate.of(2026, 5, 25), posting.dueDate)
        assertEquals(Instant.parse("2026-05-17T22:00:00Z"), posting.collectedAt)
        assertEquals(SuitabilityLabel.VerySuitable, posting.scoreLabel)
    }

    @Test
    fun `알 수 없는 유형은 Other, 알 수 없는 레이블은 점수로 다시 계산한다`() {
        val posting = PostingMapper.toPosting(dto(type = "hackathon", score = 65, scoreLabel = "brand_new_label"))

        assertEquals(PostingType.Other, posting.type)
        assertEquals(SuitabilityLabel.Suitable, posting.scoreLabel)
    }

    @Test
    fun `점수가 없으면 레이블도 없다`() {
        val posting = PostingMapper.toPosting(dto(score = null, scoreLabel = "very_suitable"))

        assertNull(posting.score)
        assertNull(posting.scoreLabel)
    }

    @Test
    fun `계약과 다른 시각 형식은 실패시킨다`() {
        assertThrows(IllegalStateException::class.java) { PostingMapper.toPosting(dto(collectedAt = "2026/05/18")) }
    }

    @Test
    fun `상세의 파싱 결과와 적합도를 옮기고 알 수 없는 축은 제외한다`() {
        val detail =
            PostingMapper.toDetail(
                PostingDetailDto(
                    id = 101,
                    title = "t",
                    type = "recruit",
                    board = PostingBoardDto(3, "b"),
                    rawContent = "본문",
                    url = "https://x",
                    dueDate = null,
                    collectedAt = "2026-05-18T07:00:00+09:00",
                    isRead = true,
                    isBookmarked = false,
                    parsed =
                        PostingParsedDto(
                            keywords = listOf("Spring", " ", "Spring", "Kotlin"),
                            qualifications = PostingQualificationsDto(year = "2학년 이상", gpa = ""),
                            preferences = listOf("RDB 1년+"),
                            formQuestions = listOf(PostingFormQuestionDto(2, "강점", 400), PostingFormQuestionDto(1, "동기", 0)),
                        ),
                    suitability =
                        SuitabilityDto(
                            score = 88,
                            label = "very_suitable",
                            breakdown = listOf(SuitabilityAxisDto("field_similarity", 95, 40), SuitabilityAxisDto("unknown_axis", 1, 1)),
                            strengthComment = "강점",
                            weaknessComment = "",
                        ),
                    similar = listOf(dto()),
                ),
            )

        val parsed = requireNotNull(detail.parsed)
        assertEquals(listOf("Spring", "Kotlin"), parsed.keywords)
        assertNull(parsed.qualifications.gpa)
        assertEquals(listOf(1, 2), parsed.formQuestions.map { it.order })
        assertNull(parsed.formQuestions.first().maxChars)
        val suitability = requireNotNull(detail.suitability)
        assertEquals(listOf(SuitabilityAxisKind.FieldSimilarity), suitability.breakdown.map { it.kind })
        assertNull(suitability.weaknessComment)
        assertEquals(1, detail.similar.size)
    }
}
