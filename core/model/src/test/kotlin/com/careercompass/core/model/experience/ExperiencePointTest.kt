package com.careercompass.core.model.experience

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class ExperiencePointTest {
    @Test
    fun `좁히기는 이름이 드러나는 호출로만 하고 값은 그대로 남는다`() {
        val date = ExperiencePoint.Date(LocalDate.of(2025, 6, 15))

        assertEquals(ExperiencePoint.YearMonth(2025, 6), date.toYearMonth())
        assertEquals(ExperiencePoint.Year(2025), date.toYear())
        // 좁히기는 도출이라 원본을 바꾸지 않는다 — 자세한 값은 그대로 남아 다시 쓸 수 있다.
        assertEquals(15, date.day)
        assertEquals(ExperiencePrecision.Date, date.precision)
    }

    @Test
    fun `넓히기는 길이 없다 — 굵은 정밀도를 상한으로 줘도 자세해지지 않는다`() {
        // 「연도 → 연월」로 가는 함수는 타입에 없다(`WithMonth` 만 `toYearMonth()` 를 갖는다).
        // 남은 길인 narrowedTo 도 자세해지는 방향으로는 아무 일도 하지 않는다.
        val year = ExperiencePoint.Year(2025)

        assertEquals(year, year.narrowedTo(ExperiencePrecision.Date))
        assertEquals(year, year.narrowedTo(ExperiencePrecision.YearMonth))
        assertEquals(ExperiencePrecision.Year, year.narrowedTo(ExperiencePrecision.Date).precision)
    }

    @Test
    fun `상한보다 자세한 시점만 그 상한까지 좁힌다`() {
        val date = ExperiencePoint.Date(LocalDate.of(2025, 6, 15))

        assertEquals(ExperiencePoint.YearMonth(2025, 6), date.narrowedTo(ExperiencePrecision.YearMonth))
        assertEquals(ExperiencePoint.Year(2025), date.narrowedTo(ExperiencePrecision.Year))
        assertEquals(date, date.narrowedTo(ExperiencePrecision.Date))
    }

    @Test
    fun `두 시점은 더 굵은 쪽 정밀도로 견준다`() {
        val start = ExperiencePoint.Date(LocalDate.of(2025, 6, 20))

        // 「6월에 끝났다」는 20일보다 앞선다고 단정할 근거가 아니다 — 종료가 말한 것은 달까지뿐이다.
        assertFalse(ExperiencePoint.YearMonth(2025, 6).isBefore(start))
        assertTrue(ExperiencePoint.YearMonth(2025, 5).isBefore(start))
        assertTrue(ExperiencePoint.Date(LocalDate.of(2025, 6, 19)).isBefore(start))
        assertFalse(ExperiencePoint.Year(2025).isBefore(start))
        assertTrue(ExperiencePoint.Year(2024).isBefore(start))
    }

    @Test
    fun `연도는 네 자리만 받는다`() {
        assertThrows(IllegalArgumentException::class.java) { ExperiencePoint.Year(25) }
        assertThrows(IllegalArgumentException::class.java) { ExperiencePoint.YearMonth(999, 6) }
        assertThrows(IllegalArgumentException::class.java) { ExperiencePoint.YearMonth(2025, 13) }
    }
}
