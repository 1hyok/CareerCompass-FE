package com.cambridge.feature.feed.presentation.shared.util

import com.cambridge.feature.feed.presentation.FIXED_CLOCK
import com.cambridge.feature.feed.presentation.NOON_TODAY
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Duration

class RelativeTimeFormatterTest {
    private fun relative(elapsed: Duration): RelativeTime = RelativeTimeFormatter.relativeTime(NOON_TODAY.minus(elapsed), FIXED_CLOCK)

    @Test
    fun `1분 미만과 미래 시각은 방금이다`() {
        assertEquals(RelativeTime.JustNow, relative(Duration.ZERO))
        assertEquals(RelativeTime.JustNow, relative(Duration.ofSeconds(59)))
        assertEquals(RelativeTime.JustNow, relative(Duration.ofMinutes(-5)))
    }

    @Test
    fun `1시간 미만은 분 단위다`() {
        assertEquals(RelativeTime.MinutesAgo(1), relative(Duration.ofSeconds(60)))
        assertEquals(RelativeTime.MinutesAgo(59), relative(Duration.ofMinutes(59).plusSeconds(59)))
    }

    @Test
    fun `하루 미만은 시간 단위다`() {
        assertEquals(RelativeTime.HoursAgo(1), relative(Duration.ofHours(1)))
        assertEquals(RelativeTime.HoursAgo(23), relative(Duration.ofHours(23).plusMinutes(59)))
    }

    @Test
    fun `하루 이상은 일 단위로 내림한다`() {
        assertEquals(RelativeTime.DaysAgo(1), relative(Duration.ofHours(24)))
        assertEquals(RelativeTime.DaysAgo(2), relative(Duration.ofDays(2).plusHours(23)))
    }
}
