package com.careercompass.core.model.experience

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test
import java.time.LocalDate

class ExperienceDraftTest {
    @Test
    fun `프로젝트와 인턴은 시작 시점이 필수다`() {
        assertThrows(IllegalArgumentException::class.java) {
            ExperienceDraft(
                title = "CareerCompass",
                startPoint = null,
                endPoint = null,
                details = ExperienceDetails.Project(role = "프론트엔드", techs = listOf("Android"), summary = null, link = null),
            )
        }
        assertThrows(IllegalArgumentException::class.java) {
            ExperienceDraft(
                title = "백엔드 인턴",
                startPoint = null,
                endPoint = null,
                details = ExperienceDetails.Intern(company = "카카오", role = "백엔드", summary = null),
            )
        }
    }

    @Test
    fun `수상과 자격증은 시점 없이 등록할 수 있다`() {
        val award =
            ExperienceDraft(
                title = "SW 공모전",
                startPoint = null,
                endPoint = null,
                details = ExperienceDetails.Award(contestName = "SW 공모전", rank = "대상", organizer = null),
            )
        val certificate =
            ExperienceDraft(
                title = "SQLD",
                startPoint = null,
                endPoint = null,
                details = ExperienceDetails.Certificate(issuer = "한국데이터산업진흥원"),
            )

        assertEquals(ExperienceType.Award, award.type)
        assertEquals(ExperienceType.Certificate, certificate.type)
    }

    @Test
    fun `종료가 시작보다 앞서면 거부한다`() {
        assertThrows(IllegalArgumentException::class.java) {
            ExperienceDraft(
                title = "CareerCompass",
                startPoint = ExperiencePoint.YearMonth(2025, 9),
                endPoint = ExperiencePoint.YearMonth(2025, 8),
                details = ExperienceDetails.Project(role = null, techs = emptyList(), summary = null, link = null),
            )
        }
    }

    @Test
    fun `종료가 시작보다 굵어도 같은 달이면 받는다`() {
        // 6월 20일에 시작해 「6월」에 끝났다 — 종료가 말한 것은 달까지뿐이라 거꾸로 된 기간이 아니다.
        // 예전에는 시트가 이 경우를 미리 눌러 시작의 일을 버렸다. 이제 모델이 정밀도를 알아 그럴 필요가 없다.
        val draft =
            ExperienceDraft(
                title = "카카오 인턴",
                startPoint = ExperiencePoint.Date(LocalDate.of(2025, 6, 20)),
                endPoint = ExperiencePoint.YearMonth(2025, 6),
                details = ExperienceDetails.Intern(company = "카카오", role = "안드로이드", summary = null),
            )

        assertEquals(ExperiencePoint.Date(LocalDate.of(2025, 6, 20)), draft.startPoint)
    }

    @Test
    fun `연도만 아는 수상 카드에 월·일이 생기지 않는다`() {
        // 이슈 #166 그 자체 — 시트가 연도 `2025` 를 `2025-01-01` 로 넓혀 없던 날짜를 만들었다.
        // 이제 모델이 그 값을 아예 받지 않으므로, 어떤 입력 경로가 실수해도 카드에 날짜가 남지 않는다.
        val award = ExperienceDetails.Award(contestName = "SW 공모전", rank = "대상", organizer = null)

        assertThrows(IllegalArgumentException::class.java) {
            ExperienceDraft(title = "SW 공모전", startPoint = ExperiencePoint.YearMonth(2025, 1), endPoint = null, details = award)
        }
        assertThrows(IllegalArgumentException::class.java) {
            ExperienceDraft(
                title = "SW 공모전",
                startPoint = ExperiencePoint.Date(LocalDate.of(2025, 1, 1)),
                endPoint = null,
                details = award,
            )
        }
        assertEquals(
            ExperiencePoint.Year(2025),
            ExperienceDraft(title = "SW 공모전", startPoint = ExperiencePoint.Year(2025), endPoint = null, details = award).startPoint,
        )
    }

    @Test
    fun `연월까지 아는 자격증 카드에 일이 생기지 않는다`() {
        val certificate = ExperienceDetails.Certificate(issuer = null)

        assertThrows(IllegalArgumentException::class.java) {
            ExperienceDraft(
                title = "SQLD",
                startPoint = ExperiencePoint.Date(LocalDate.of(2025, 6, 15)),
                endPoint = null,
                details = certificate,
            )
        }
        // 반대로 연도만 아는 자격증도 「취득 연월」이라는 필드의 뜻을 채우지 못한다.
        assertThrows(IllegalArgumentException::class.java) {
            ExperienceDraft(title = "SQLD", startPoint = ExperiencePoint.Year(2025), endPoint = null, details = certificate)
        }
        assertEquals(
            ExperiencePoint.YearMonth(2025, 6),
            ExperienceDraft(
                title = "SQLD",
                startPoint = ExperiencePoint.YearMonth(2025, 6),
                endPoint = null,
                details = certificate,
            ).startPoint,
        )
    }

    @Test
    fun `있던 일은 모델을 지나도 깎이지 않는다`() {
        // 이슈 #171 의 반대쪽 계약 — 시트가 담지 못하는 정밀도라도 모델은 받아서 그대로 들고 있어야,
        // 「열었다 저장만 했는데 15일이 1일이 되는」 손실을 시트가 되돌릴 수 있다.
        val card =
            Experience(
                id = 1L,
                title = "카카오 인턴",
                startPoint = ExperiencePoint.Date(LocalDate.of(2025, 6, 15)),
                endPoint = ExperiencePoint.Date(LocalDate.of(2025, 8, 20)),
                details = ExperienceDetails.Intern(company = "카카오", role = "안드로이드", summary = null),
                createdAt = null,
            )

        assertEquals(ExperiencePoint.Date(LocalDate.of(2025, 6, 15)), card.startPoint)
        assertEquals(ExperiencePrecision.Date, card.startPoint?.precision)
    }

    @Test
    fun `기간이 없는 유형은 종료 시점을 가질 수 없다`() {
        assertThrows(IllegalArgumentException::class.java) {
            ExperienceDraft(
                title = "SW 공모전",
                startPoint = ExperiencePoint.Year(2024),
                endPoint = ExperiencePoint.Year(2025),
                details = ExperienceDetails.Award(contestName = "SW 공모전", rank = "대상", organizer = null),
            )
        }
    }

    @Test
    fun `시작 없이 종료만 있는 기간은 없다`() {
        assertThrows(IllegalArgumentException::class.java) {
            ExperienceDraft(
                title = "CareerCompass",
                startPoint = null,
                endPoint = ExperiencePoint.YearMonth(2025, 9),
                details = ExperienceDetails.Activity(organization = "동아리", role = null, summary = null),
            )
        }
    }

    @Test
    fun `서버가 준 카드는 시작 시점이 없어도 열린다`() {
        // 시작 시점 필수는 우리가 만드는 입력([ExperienceDraft])에만 건다 — 서버 값 때문에 목록 전체가
        // 못 열리면 사용자가 할 수 있는 일이 없다.
        val card =
            Experience(
                id = 3L,
                title = "CareerCompass",
                startPoint = null,
                endPoint = null,
                details = ExperienceDetails.Project(role = null, techs = emptyList(), summary = null, link = null),
                createdAt = null,
            )

        assertEquals(ExperienceType.Project, card.type)
    }
}
