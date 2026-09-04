package com.careercompass.core.data.mapper

import java.time.Instant
import java.time.LocalDate
import java.time.OffsetDateTime
import java.time.format.DateTimeParseException

/** 서버 날짜/시각 문자열 — `2026-05-25`(날짜)·`2026-05-18T07:00:00+09:00`(오프셋 시각). */
internal object WireTime {
    fun parseDate(value: String): LocalDate =
        try {
            LocalDate.parse(value)
        } catch (exception: DateTimeParseException) {
            throw IllegalStateException("날짜 형식이 계약과 다릅니다: $value", exception)
        }

    fun parseInstant(value: String): Instant =
        try {
            OffsetDateTime.parse(value).toInstant()
        } catch (exception: DateTimeParseException) {
            try {
                Instant.parse(value)
            } catch (_: DateTimeParseException) {
                throw IllegalStateException("시각 형식이 계약과 다릅니다: $value", exception)
            }
        }

    fun formatDate(value: LocalDate): String = value.toString()
}
