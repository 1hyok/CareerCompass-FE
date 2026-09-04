package com.careercompass.core.model.board

import java.time.Instant
import java.time.LocalDate

/** 게시판 등록 상한 — 기능 스펙 F2-1 (최대 20개). */
public const val MAX_BOARDS: Int = 20

/** 기본 수집 주기 — 기능 스펙 F2-1 (1일 1회). */
public const val DEFAULT_BOARD_CYCLE_HOURS: Int = 24

/** 게시판 공고 유형 — API_SPEC v0.1 §5 `POST /boards` `type`. */
public enum class BoardType(
    public val wireValue: String,
) {
    Scholarship("scholarship"),
    Recruit("recruit"),
    Contest("contest"),
    Activity("activity"),
    Other("other"),
    ;

    public companion object {
        public fun fromWireValue(value: String): BoardType? = entries.firstOrNull { it.wireValue == value }
    }
}

/**
 * 게시판 수집 상태. 명세는 `status` 필드만 언급하고 값을 열거하지 않아 알려진 값 외는 [Unknown] 으로 받는다 —
 * 서버가 값을 늘려도 목록이 깨지지 않게 하되, 새 값은 그대로 드러난다.
 */
public enum class BoardStatus(
    public val wireValue: String,
) {
    Active("active"),
    Paused("paused"),
    Failed("failed"),
    Unknown(""),
    ;

    public companion object {
        public fun fromWireValue(value: String): BoardStatus = entries.firstOrNull { it != Unknown && it.wireValue == value } ?: Unknown
    }
}

/** 등록된 게시판 — `GET /boards`. */
public data class Board(
    val id: Long,
    val url: String,
    val name: String,
    val type: BoardType,
    val cycleHours: Int,
    val isActive: Boolean,
    val status: BoardStatus,
    val failCount: Int,
    val lastCollectedAt: Instant?,
) {
    init {
        require(url.isNotBlank()) { "url must not be blank" }
        require(name.isNotBlank()) { "name must not be blank" }
        require(cycleHours > 0) { "cycleHours must be positive" }
        require(failCount >= 0) { "failCount must not be negative" }
    }
}

/** 구조 감지 결과 상태 — API_SPEC v0.1 §5 `detectStatus`. */
public enum class BoardDetectionStatus(
    public val wireValue: String,
) {
    Success("success"),
    LoginRequired("login_required"),
    Spa("spa"),
    Blocked("blocked"),
    Failed("failed"),
    ;

    public companion object {
        public fun fromWireValue(value: String): BoardDetectionStatus? = entries.firstOrNull { it.wireValue == value }
    }
}

/** 감지 미리보기 게시글 한 건. */
public data class BoardPreviewItem(
    val title: String,
    val url: String,
    val date: LocalDate?,
) {
    init {
        require(title.isNotBlank()) { "title must not be blank" }
        require(url.isNotBlank()) { "url must not be blank" }
    }
}

/**
 * `POST /boards/detect` 결과.
 *
 * @property hasDateSelector 날짜 셀렉터 추출 여부 — 실패해도 등록은 가능하나 마감일 파싱 정확도가 떨어진다(F2-1).
 */
public data class BoardDetection(
    val status: BoardDetectionStatus,
    val preview: List<BoardPreviewItem>,
    val hasDateSelector: Boolean,
) {
    init {
        require(status != BoardDetectionStatus.Success || preview.isNotEmpty()) { "successful detection must include a preview" }
    }

    public val isRegistrable: Boolean get() = status == BoardDetectionStatus.Success
}

/** `POST /boards` 등록 확정 입력. */
public data class BoardRegistration(
    val url: String,
    val name: String,
    val type: BoardType,
    val cycleHours: Int = DEFAULT_BOARD_CYCLE_HOURS,
) {
    init {
        require(url.isNotBlank()) { "url must not be blank" }
        require(name.isNotBlank()) { "name must not be blank" }
        require(cycleHours > 0) { "cycleHours must be positive" }
    }
}

/** `PATCH /boards/{id}` 부분 수정 — null 인 필드는 보내지 않는다. */
public data class BoardUpdate(
    val name: String? = null,
    val type: BoardType? = null,
    val cycleHours: Int? = null,
    val isActive: Boolean? = null,
) {
    init {
        require(name == null || name.isNotBlank()) { "name must be null or non-blank" }
        require(cycleHours == null || cycleHours > 0) { "cycleHours must be null or positive" }
    }

    public val isEmpty: Boolean
        get() = name == null && type == null && cycleHours == null && isActive == null
}
