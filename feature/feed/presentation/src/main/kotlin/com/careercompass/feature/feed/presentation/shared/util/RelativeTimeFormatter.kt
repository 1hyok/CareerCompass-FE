package com.careercompass.feature.feed.presentation.shared.util

import android.content.res.Resources
import com.careercompass.feature.feed.presentation.R
import java.time.Clock
import java.time.Duration
import java.time.Instant

/** 상대 시각의 단위 — 문구는 [toLabel] 이 리소스로 만든다. */
public sealed interface RelativeTime {
    public data object JustNow : RelativeTime

    public data class MinutesAgo(
        val minutes: Long,
    ) : RelativeTime

    public data class HoursAgo(
        val hours: Long,
    ) : RelativeTime

    public data class DaysAgo(
        val days: Long,
    ) : RelativeTime
}

/**
 * 「방금」·「n분 전」·「n시간 전」·「n일 전」 — 게시판 마지막 수집·공고 수집 시각 표기.
 *
 * 「지금」은 주입된 [Clock] 으로 읽는다. 시계보다 미래인 시각(서버·기기 시계 어긋남)은 「방금」으로 본다.
 */
public object RelativeTimeFormatter {
    public fun relativeTime(
        instant: Instant,
        clock: Clock,
    ): RelativeTime {
        val elapsed = Duration.between(instant, clock.instant())
        return when {
            elapsed < Duration.ofMinutes(1) -> RelativeTime.JustNow
            elapsed < Duration.ofHours(1) -> RelativeTime.MinutesAgo(elapsed.toMinutes())
            elapsed < Duration.ofDays(1) -> RelativeTime.HoursAgo(elapsed.toHours())
            else -> RelativeTime.DaysAgo(elapsed.toDays())
        }
    }

    public fun format(
        resources: Resources,
        instant: Instant,
        clock: Clock,
    ): String = relativeTime(instant, clock).toLabel(resources)
}

public fun RelativeTime.toLabel(resources: Resources): String =
    when (this) {
        RelativeTime.JustNow -> resources.getString(R.string.feed_relative_time_just_now)
        is RelativeTime.MinutesAgo -> resources.getString(R.string.feed_relative_time_minutes_ago, minutes)
        is RelativeTime.HoursAgo -> resources.getString(R.string.feed_relative_time_hours_ago, hours)
        is RelativeTime.DaysAgo -> resources.getString(R.string.feed_relative_time_days_ago, days)
    }
