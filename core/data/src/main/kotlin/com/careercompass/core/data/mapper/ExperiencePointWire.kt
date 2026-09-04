package com.careercompass.core.data.mapper

import com.careercompass.core.model.experience.EXPERIENCE_YEAR_RANGE
import com.careercompass.core.model.experience.ExperiencePoint
import com.careercompass.core.model.experience.ExperienceType
import kotlinx.serialization.json.JsonObject

/**
 * 경험 카드의 시점을 와이어(API_SPEC v0.1 §3)와 [ExperiencePoint] 사이에서 옮긴다.
 *
 * ## 서버 계약에는 정밀도 칸이 없다 — 그래서 어디서 알아내는가 (#207)
 * 공통 컬럼 `startDate`·`endDate` 는 **언제나 `YYYY-MM-DD`** 다. 즉 그 칸만 보면 「6월 1일에 시작했다」와
 * 「6월에 시작했다」를 구분할 수단이 없다. 정밀도를 알려 주는 것은 유형별 `data` 안의 전용 키뿐이다.
 *
 * | 유형 | 정밀도의 출처 | 없을 때 |
 * |---|---|---|
 * | 수상 | `data.year` (Int) → 연 | `startDate` 를 **연으로 좁혀** 읽는다 |
 * | 자격증 | `data.acquiredYearMonth` (`YYYY-MM`) → 연월 | `startDate` 를 **연월로 좁혀** 읽는다 |
 * | 프로젝트·인턴·대외활동 | `startDate` → 날짜 | 시점 없음 |
 *
 * 판정을 이 한 곳에만 두는 이유는, 같은 규칙이 매퍼·시트·카드 목록에 흩어졌던 것이 #166·#171 의 원인이기
 * 때문이다. 좁히기(날짜 → 연·연월)는 도출이라 안전하고, 반대 방향은 [ExperiencePoint] 가 타입으로 막는다.
 *
 * ## 우리가 못 지키는 것 — 프로젝트·인턴의 연월 정밀도는 와이어를 건너면 굳는다
 * 시트는 `YYYY.MM` 을 받으므로 새로 만든 프로젝트 카드의 시점은 연월 정밀도다. 그런데 보낼 칸이
 * `YYYY-MM-DD` 뿐이라 **그 달 1일로 적어 보낼 수밖에 없다.** 다시 읽으면 「1일」로 돌아온다.
 *
 * 대안을 둘 다 버렸다 — `data` 에 `startPrecision` 같은 키를 우리 마음대로 더하는 것은 서버가 모르는
 * 계약을 클라이언트가 지어내는 것이고, 정밀도를 로컬에 따로 저장하는 것은 기기를 바꾸면 사라지는 두 번째
 * 정본이다. 그래서 **아는 만큼만 지키고 모르는 것은 지어내지 않는다**는 원칙을 이렇게 나눴다.
 *
 * - 서버가 정밀도를 알려 주는 수상·자격증은 **왕복해도 그대로다**(전용 키가 정본).
 * - 프로젝트·인턴·대외활동은 서버를 한 번 다녀오면 일이 `01` 로 굳는다. 다만 앱 안에서는 사용자가 그 칸을
 *   손대지 않는 한 원래 시점을 그대로 들고 다니므로(#171), 열었다 저장하는 것만으로 값이 바뀌지는 않는다.
 *
 * 서버가 `startDate` 에 정밀도를 실을 방법이 생기면 고칠 자리는 이 파일 하나다.
 */
internal object ExperiencePointWire {
    private val YEAR_MONTH = Regex("""^(\d{4})-(0[1-9]|1[0-2])$""")

    /** 시작 시점을 읽는다 — 유형별 전용 키를 먼저 보고, 없으면 공통 `startDate` 를 그 유형의 정밀도로 좁힌다. */
    fun readStart(
        type: ExperienceType,
        data: JsonObject,
        startDate: String?,
    ): ExperiencePoint? = normalize(type, readDetailPoint(type, data) ?: startDate?.let(::readWireDate))

    /** 종료 시점을 읽는다. 기간이 없는 유형(수상·자격증)의 종료는 뜻이 없어 버린다 — 모델도 받지 않는다. */
    fun readEnd(
        type: ExperienceType,
        endDate: String?,
    ): ExperiencePoint? = if (type.hasPeriod) normalize(type, endDate?.let(::readWireDate)) else null

    /**
     * 공통 `startDate`·`endDate` 칸에 적을 문자열.
     *
     * 수상·자격증은 **비운다** — 그 유형의 시점은 `data` 의 전용 키가 정본이고, 공통 칸에 적으려면 없는
     * 월·일을 지어내야 한다(#166). 나머지 유형의 연월 정밀도는 그 달 1일로 굳는다(이 파일 KDoc 참고).
     */
    fun writeWireDate(
        type: ExperienceType,
        point: ExperiencePoint?,
    ): String? {
        if (point == null || !type.hasPeriod) return null
        val day = (point as? ExperiencePoint.Date)?.day ?: 1
        return "%04d-%02d-%02d".format(point.year, (point as? ExperiencePoint.WithMonth)?.month ?: 1, day)
    }

    /** 수상의 `data.year` 값. 다른 유형이면 null. */
    fun writeAwardYear(point: ExperiencePoint?): Int? = point?.year

    /** 자격증의 `data.acquiredYearMonth` 값(`YYYY-MM`). 모델이 연월 이상을 보장한다. */
    fun writeAcquiredYearMonth(point: ExperiencePoint?): String? =
        (point as? ExperiencePoint.WithMonth)?.let { "%04d-%02d".format(it.year, it.month) }

    private fun readDetailPoint(
        type: ExperienceType,
        data: JsonObject,
    ): ExperiencePoint? =
        when (type) {
            ExperienceType.Award -> data.awardYear()?.let(ExperiencePoint::Year)
            ExperienceType.Certificate -> data.acquiredYearMonth()
            else -> null
        }

    /**
     * 유형이 담을 수 있는 정밀도로 맞춘다. 자세하면 좁히고, **너무 굵으면 버린다.**
     *
     * 서버 값으로 `require` 를 깨뜨려 목록 전체를 못 열게 만들지 않는다 — 우리가 못 고치는 값이다.
     */
    private fun normalize(
        type: ExperienceType,
        point: ExperiencePoint?,
    ): ExperiencePoint? = point?.narrowedTo(type.maxPointPrecision)?.takeIf { it.precision >= type.minPointPrecision }

    private fun readWireDate(value: String): ExperiencePoint = ExperiencePoint.Date(WireTime.parseDate(value))

    /** 형식이 어긋난 선택 필드는 실패시키지 않고 못 본 것으로 둔다 — 공통 `startDate` 가 대신 읽힌다. */
    private fun JsonObject.acquiredYearMonth(): ExperiencePoint.YearMonth? {
        val match = YEAR_MONTH.matchEntire(stringOrNull("acquiredYearMonth") ?: return null) ?: return null
        val (year, month) = match.destructured
        return year.toInt().takeIf { it in EXPERIENCE_YEAR_RANGE }?.let { ExperiencePoint.YearMonth(it, month.toInt()) }
    }

    private fun JsonObject.awardYear(): Int? = intOrNull("year")?.takeIf { it in EXPERIENCE_YEAR_RANGE }
}
