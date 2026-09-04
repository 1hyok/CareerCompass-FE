package com.cambridge.core.data.mapper

import com.cambridge.core.model.experience.ExperienceDetails
import com.cambridge.core.model.experience.ExperienceDraft
import com.cambridge.core.model.experience.ExperiencePoint
import com.cambridge.core.model.experience.ExperienceType
import com.cambridge.core.model.experience.MAX_EXPERIENCE_TECH_TAGS
import com.cambridge.core.model.experience.MAX_EXPERIENCE_TECH_TAG_LENGTH
import com.cambridge.core.network.dto.ExperienceDto
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
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
        assertEquals(ExperiencePoint.Date(LocalDate.of(2025, 9, 1)), experience.startPoint)
        assertEquals("프론트엔드", details.role)
        assertEquals(listOf("Android", "Kotlin"), details.techs)
        assertEquals(null, details.summary)
        assertEquals(null, details.link)
    }

    @Test
    fun `상한을 넘는 서버 태그는 앱을 죽이지 않고 담을 수 있는 만큼만 담는다`() {
        // 다른 클라이언트가 만든 카드일 수 있고 우리가 못 고치는 값이다 — 카드 하나로 목록 전체가 안 열리면
        // 사용자가 할 수 있는 일이 없다. 길이를 넘는 태그는 **자르지 않고 버린다**(자르면 없던 이름이 된다).
        val techs = (1..12).joinToString(",") { "\"tech$it\"" }
        val tooLong = "a".repeat(MAX_EXPERIENCE_TECH_TAG_LENGTH + 1)
        val experience =
            ExperienceMapper.toExperience(
                ExperienceDto(
                    id = 11,
                    type = "project",
                    title = "CareerCompass",
                    data = json.parseToJsonElement("""{"techs":[$techs,"$tooLong"]}""").jsonObject,
                ),
            )

        val details = experience.details as ExperienceDetails.Project
        assertEquals(MAX_EXPERIENCE_TECH_TAGS, details.techs.size)
        assertEquals("tech1", details.techs.first())
        assertFalse(details.techs.contains(tooLong))
    }

    @Test
    fun `모델이 받지 않는 서버 링크는 버린다`() {
        // 잘라 담으면 다른 곳을 가리키거나 열리지 않는 주소가 된다. 스킴이 어긋난 링크도 카드에 싣지 않는다.
        val experience =
            ExperienceMapper.toExperience(
                ExperienceDto(
                    id = 12,
                    type = "project",
                    title = "CareerCompass",
                    data = json.parseToJsonElement("""{"link":"javascript:alert(1)"}""").jsonObject,
                ),
            )

        assertNull((experience.details as ExperienceDetails.Project).link)
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
                    startPoint = ExperiencePoint.YearMonth(2025, 6),
                    endPoint = null,
                    details = ExperienceDetails.Certificate(issuer = null),
                ),
            )

        assertEquals("cert", request.type)
        // 자격증의 시점은 `data.acquiredYearMonth` 가 정본이다 — 공통 칸에 적으려면 없는 일을 지어내야 한다(#166).
        assertEquals(null, request.startDate)
        assertEquals("""{"acquiredYearMonth":"2025-06"}""", request.data.toString())
    }

    @Test
    fun `수상의 정밀도는 서버를 왕복해도 연도 그대로다`() {
        // 서버가 `startDate` 에 무엇을 적어 보내든 수상의 정본은 `data.year` 다 — 공통 칸의 월·일은 뜻이 없다.
        val experience =
            ExperienceMapper.toExperience(
                ExperienceDto(
                    id = 3,
                    type = "award",
                    title = "SW 공모전",
                    startDate = "2025-03-02",
                    data = json.parseToJsonElement("""{"contestName":"SW 공모전","rank":"대상","year":2025}""").jsonObject,
                ),
            )

        assertEquals(ExperiencePoint.Year(2025), experience.startPoint)

        val request =
            ExperienceMapper.toRequest(
                ExperienceDraft(
                    title = experience.title,
                    startPoint = experience.startPoint,
                    endPoint = null,
                    details = experience.details,
                ),
            )

        assertNull(request.startDate)
        assertEquals("""{"contestName":"SW 공모전","rank":"대상","year":2025}""", request.data.toString())
    }

    @Test
    fun `수상에 연도가 없으면 공통 날짜 칸을 연도로 좁혀 읽는다`() {
        // 좁히기는 도출이라 안전하다. 반대로 연도에서 월·일을 채우는 길은 모델에 없다.
        val experience =
            ExperienceMapper.toExperience(
                ExperienceDto(
                    id = 4,
                    type = "award",
                    title = "SW 공모전",
                    startDate = "2025-03-02",
                    endDate = "2025-04-02",
                    data = json.parseToJsonElement("""{"contestName":"SW 공모전","rank":"대상"}""").jsonObject,
                ),
            )

        assertEquals(ExperiencePoint.Year(2025), experience.startPoint)
        // 기간이 없는 유형의 종료는 뜻이 없어 버린다 — 모델도 받지 않는다.
        assertNull(experience.endPoint)
    }

    @Test
    fun `형식이 어긋난 취득 연월은 못 본 것으로 두고 공통 칸을 읽는다`() {
        // 선택 필드 하나가 어긋났다고 카드를 통째로 실패시키면 사용자가 할 수 있는 일이 없다.
        val experience =
            ExperienceMapper.toExperience(
                ExperienceDto(
                    id = 5,
                    type = "cert",
                    title = "SQLD",
                    startDate = "2025-06-15",
                    data = json.parseToJsonElement("""{"acquiredYearMonth":"2025.06"}""").jsonObject,
                ),
            )

        assertEquals(ExperiencePoint.YearMonth(2025, 6), experience.startPoint)
    }

    @Test
    fun `프로젝트의 연월 정밀도는 와이어를 건너면 그 달 1일로 굳는다`() {
        // 우리가 못 지키는 부분을 계약으로 적어 둔다 — 서버 계약(API_SPEC v0.1 §3)의 공통 칸은 `YYYY-MM-DD`
        // 하나뿐이라 연월을 그대로 실을 자리가 없다. 근거와 버린 대안은 `ExperiencePointWire` KDoc.
        val request =
            ExperienceMapper.toRequest(
                ExperienceDraft(
                    title = "CareerCompass",
                    startPoint = ExperiencePoint.YearMonth(2025, 9),
                    endPoint = null,
                    details = ExperienceDetails.Project(role = null, techs = emptyList(), summary = null, link = null),
                ),
            )

        assertEquals("2025-09-01", request.startDate)
    }

    @Test
    fun `프로젝트의 일은 와이어를 왕복해도 깎이지 않는다`() {
        val draft =
            ExperienceDraft(
                title = "CareerCompass",
                startPoint = ExperiencePoint.Date(LocalDate.of(2025, 9, 15)),
                endPoint = ExperiencePoint.Date(LocalDate.of(2025, 10, 20)),
                details = ExperienceDetails.Project(role = null, techs = emptyList(), summary = null, link = null),
            )

        val request = ExperienceMapper.toRequest(draft)
        val restored =
            ExperienceMapper.toExperience(
                ExperienceDto(
                    id = 9,
                    type = request.type,
                    title = request.title,
                    startDate = request.startDate,
                    endDate = request.endDate,
                    data = request.data,
                ),
            )

        assertEquals(draft.startPoint, restored.startPoint)
        assertEquals(draft.endPoint, restored.endPoint)
    }
}
