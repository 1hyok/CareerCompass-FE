package com.cambridge.core.model.experience

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test
import java.time.LocalDate

class ExperienceDraftTest {
    @Test
    fun `프로젝트와 인턴은 시작일이 필수다`() {
        assertThrows(IllegalArgumentException::class.java) {
            ExperienceDraft(
                title = "CareerCompass",
                startDate = null,
                endDate = null,
                details = ExperienceDetails.Project(role = "프론트엔드", techs = listOf("Android"), summary = null, link = null),
            )
        }
        assertThrows(IllegalArgumentException::class.java) {
            ExperienceDraft(
                title = "백엔드 인턴",
                startDate = null,
                endDate = null,
                details = ExperienceDetails.Intern(company = "카카오", role = "백엔드", summary = null),
            )
        }
    }

    @Test
    fun `수상과 자격증은 기간 없이 등록할 수 있다`() {
        val award =
            ExperienceDraft(
                title = "SW 공모전",
                startDate = null,
                endDate = null,
                details = ExperienceDetails.Award(contestName = "SW 공모전", rank = "대상", year = 2025, organizer = null),
            )
        val certificate =
            ExperienceDraft(
                title = "SQLD",
                startDate = null,
                endDate = null,
                details = ExperienceDetails.Certificate(issuer = "한국데이터산업진흥원", acquiredYearMonth = "2025-06"),
            )

        assertEquals(ExperienceType.Award, award.type)
        assertEquals(ExperienceType.Certificate, certificate.type)
    }

    @Test
    fun `종료일이 시작일보다 앞서면 거부한다`() {
        assertThrows(IllegalArgumentException::class.java) {
            ExperienceDraft(
                title = "CareerCompass",
                startDate = LocalDate.of(2025, 9, 1),
                endDate = LocalDate.of(2025, 8, 1),
                details = ExperienceDetails.Project(role = null, techs = emptyList(), summary = null, link = null),
            )
        }
    }

    @Test
    fun `자격증 취득 연월은 YYYY-MM 형식만 허용한다`() {
        assertThrows(IllegalArgumentException::class.java) {
            ExperienceDetails.Certificate(issuer = null, acquiredYearMonth = "2025.06")
        }
    }
}
