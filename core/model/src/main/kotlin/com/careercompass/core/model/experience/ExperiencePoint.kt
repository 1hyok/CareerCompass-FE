package com.careercompass.core.model.experience

import java.time.LocalDate

/** 시점의 연도가 담을 수 있는 범위 — 와이어(API_SPEC v0.1 §3)의 `YYYY` 와 같은 네 자리다. */
public val EXPERIENCE_YEAR_RANGE: IntRange = 1000..9999

/**
 * 시점 정밀도 — **굵은 것에서 자세한 것 순**이라 선언 순서가 곧 비교 순서다(`Year < YearMonth < Date`).
 *
 * 순서에 의미를 실은 이유는 [ExperienceType.minPointPrecision]·[ExperienceType.maxPointPrecision] 이
 * 「이 유형이 담을 수 있는 정밀도의 범위」를 이 열거형의 대소로 적기 때문이다. 항목을 사이에 끼워 넣으면
 * 그 범위 판정이 함께 바뀐다.
 */
public enum class ExperiencePrecision {
    /** 연도만 안다 — `2025`. */
    Year,

    /** 연월까지 안다 — `2025-06`. */
    YearMonth,

    /** 일까지 안다 — `2025-06-15`. */
    Date,
}

/**
 * 「그 경험이 언제인가」를 **아는 만큼만** 담는 시점.
 *
 * ### 왜 `LocalDate` 가 아닌가 (#166 · #171)
 * 경험 카드의 시점은 유형마다 정밀도가 다르다 — 수상은 연도만, 자격증은 연월만, 프로젝트·인턴은 날짜다(F1-3).
 * 그런데 모델이 `LocalDate` 하나로 담으면 **정밀도가 값에서 사라진다.** 사라진 정밀도는 화면이 그때그때
 * 되살려야 하고, 그 왕복에서 값이 두 방향으로 조용히 바뀌었다.
 *
 * - **넓히기(날조)** — 연도만 아는 수상 카드를 열었다 저장만 해도 `2025-01-01` 이 생겼다(#166).
 *   사용자가 준 적 없는 월·일이 사실로 굳어 정렬·표시가 그걸 근거로 삼는다.
 * - **좁히기(손실)** — 시트가 담지 못하는 일(day)이 열었다 저장하는 것만으로 깎였다(#171).
 *
 * 그래서 정밀도를 값에 붙였다. 좁히기는 **도출**이라 [toYear]·[toYearMonth] 처럼 이름이 드러나는 호출로만
 * 하고, 넓히기는 **아예 길이 없다** — [Year] 에서 [YearMonth] 를 얻는 함수가 타입에 없고, 자세한 시점은
 * `LocalDate` 나 월 값을 손에 든 호출자만 만들 수 있다.
 *
 * ### 「연월 이상」이 왜 별도 타입([WithMonth])인가
 * `toYearMonth()` 를 이 인터페이스에 두면 [Year] 도 부를 수 있게 되고, 그 구현은 없는 달을 지어내는 수밖에
 * 없다. 월을 아는 시점만 [WithMonth] 를 구현하게 해 **넓히기를 컴파일 단계에서 막는다.**
 */
public sealed interface ExperiencePoint {
    public val year: Int

    /** 이 시점이 아는 정밀도. */
    public val precision: ExperiencePrecision

    /** 연 정밀도로 좁힌다 — 좁히기는 도출이라 어떤 시점에서도 할 수 있다. */
    public fun toYear(): Year = Year(year)

    /**
     * [ceiling] 보다 자세하면 거기까지 좁히고, 이미 그만큼 굵으면 **그대로 둔다.**
     *
     * 「최대 이만큼까지만」이라 넓히는 경우가 없다 — 서버가 준 값을 유형의 상한에 맞출 때
     * (`ExperienceMapper`) 쓰는 자리라, 굵은 값이 들어와도 없는 정밀도를 만들지 않아야 한다.
     */
    public fun narrowedTo(ceiling: ExperiencePrecision): ExperiencePoint

    /**
     * 두 시점을 **더 굵은 쪽 정밀도로 맞춰** 비교한다.
     *
     * 「2025-06-20 시작, 2025-06 종료」는 어긋난 기간이 아니다 — 종료가 말한 것은 달까지뿐이라 그 달의
     * 어느 날인지 우리는 모른다. 자세한 쪽에 맞춰 비교하면 사용자가 하지 않은 말을 근거로 기간을 거부하게 된다.
     */
    public fun isBefore(other: ExperiencePoint): Boolean {
        val common = minOf(precision, other.precision)
        return narrowedTo(common).sortKey() < other.narrowedTo(common).sortKey()
    }

    /** 연월 이상을 아는 시점. [Year] 는 여기 없어서 [toYearMonth] 를 부를 수 없다. */
    public sealed interface WithMonth : ExperiencePoint {
        public val month: Int

        /** 연월 정밀도로 좁힌다. */
        public fun toYearMonth(): YearMonth = YearMonth(year, month)
    }

    /** 연도만 아는 시점 — 수상(F1-3)의 정밀도다. */
    public data class Year(
        override val year: Int,
    ) : ExperiencePoint {
        init {
            require(year in EXPERIENCE_YEAR_RANGE) { "year must be in $EXPERIENCE_YEAR_RANGE" }
        }

        override val precision: ExperiencePrecision get() = ExperiencePrecision.Year

        override fun narrowedTo(ceiling: ExperiencePrecision): ExperiencePoint = this
    }

    /** 연월까지 아는 시점 — 자격증(F1-3)의 정밀도이자, 시트의 `YYYY.MM` 칸이 담는 정밀도다. */
    public data class YearMonth(
        override val year: Int,
        override val month: Int,
    ) : WithMonth {
        init {
            require(year in EXPERIENCE_YEAR_RANGE) { "year must be in $EXPERIENCE_YEAR_RANGE" }
            require(month in 1..12) { "month must be in 1..12" }
        }

        override val precision: ExperiencePrecision get() = ExperiencePrecision.YearMonth

        override fun narrowedTo(ceiling: ExperiencePrecision): ExperiencePoint = if (ceiling == ExperiencePrecision.Year) toYear() else this
    }

    /** 일까지 아는 시점 — 서버가 `YYYY-MM-DD` 로 주는 프로젝트·인턴의 정밀도다. */
    public data class Date(
        val date: LocalDate,
    ) : WithMonth {
        init {
            require(date.year in EXPERIENCE_YEAR_RANGE) { "year must be in $EXPERIENCE_YEAR_RANGE" }
        }

        override val year: Int get() = date.year
        override val month: Int get() = date.monthValue

        /** 일(day). */
        public val day: Int get() = date.dayOfMonth

        override val precision: ExperiencePrecision get() = ExperiencePrecision.Date

        override fun narrowedTo(ceiling: ExperiencePrecision): ExperiencePoint =
            when (ceiling) {
                ExperiencePrecision.Year -> toYear()
                ExperiencePrecision.YearMonth -> toYearMonth()
                ExperiencePrecision.Date -> this
            }
    }
}

/** 정밀도가 같은 두 시점을 견주기 위한 정렬 키. 없는 자리는 0 이라 같은 정밀도끼리만 뜻이 있다. */
private fun ExperiencePoint.sortKey(): Int =
    when (this) {
        is ExperiencePoint.Year -> year * 10_000
        is ExperiencePoint.YearMonth -> year * 10_000 + month * 100
        is ExperiencePoint.Date -> year * 10_000 + month * 100 + day
    }
