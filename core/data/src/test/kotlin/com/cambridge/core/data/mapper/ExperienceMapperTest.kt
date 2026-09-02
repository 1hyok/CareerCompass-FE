package com.cambridge.core.data.mapper

import com.cambridge.core.model.experience.ExperienceDetails
import com.cambridge.core.model.experience.ExperienceDraft
import com.cambridge.core.model.experience.ExperienceType
import com.cambridge.core.network.dto.ExperienceDto
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test
import java.time.LocalDate

class ExperienceMapperTest {
    private val json = Json

    @Test
    fun `프로젝트 data 객체를 세부 정보로 옮긴다`() {
        val experience =
            ExperienceMapper.toExperience(
                ExperienceDto(
                    id = 7,
                    type = "project",
                    title = "CareerCompass",
                    startDate = "2025-09-01",
                    endDate = null,
                    data =
                        json
                            .parseToJsonElement(
                                """{"role":"프론트엔드","techs":["Android","Kotlin","Android"],"summary":"","link":null}""",
                            ).jsonObject,
                ),
            )

        val details = experience.details as ExperienceDetails.Project
        assertEquals(ExperienceType.Project, experience.type)
        assertEquals(LocalDate.of(2025, 9, 1), experience.startDate)
        assertEquals("프론트엔드", details.role)
        assertEquals(listOf("Android", "Kotlin"), details.techs)
        assertEquals(null, details.summary)
        assertEquals(null, details.link)
    }

    @Test
    fun `필수 필드가 없는 수상 data 는 실패시킨다`() {
        assertThrows(IllegalStateException::class.java) {
            ExperienceMapper.toExperience(
                ExperienceDto(id = 1, type = "award", title = "공모전", data = json.parseToJsonElement("""{"rank":"대상"}""").jsonObject),
            )
        }
    }

    @Test
    fun `알 수 없는 유형은 실패시킨다`() {
        assertThrows(IllegalStateException::class.java) {
            ExperienceMapper.toExperience(
                ExperienceDto(id = 1, type = "volunteer", title = "봉사", data = json.parseToJsonElement("{}").jsonObject),
            )
        }
    }

    @Test
    fun `초안을 요청으로 옮기며 null 필드는 넣지 않는다`() {
        val request =
            ExperienceMapper.toRequest(
                ExperienceDraft(
                    title = "SQLD",
                    startDate = null,
                    endDate = null,
                    details = ExperienceDetails.Certificate(issuer = null, acquiredYearMonth = "2025-06"),
                ),
            )

        assertEquals("cert", request.type)
        assertEquals(null, request.startDate)
        assertEquals("""{"acquiredYearMonth":"2025-06"}""", request.data.toString())
    }
}
