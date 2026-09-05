package com.careercompass.feature.foryou.domain.model

/** 강점 Export 형식 — API_SPEC v0.1 §7 `POST /export` 의 `format`. */
public enum class ExportFormat(
    public val wireValue: String,
) {
    Markdown("markdown"),
    Notion("notion"),
    Html("html"),
    Plain("plain"),
    ;

    public companion object {
        public fun fromWireValue(value: String): ExportFormat? = entries.firstOrNull { it.wireValue == value }
    }
}

/** 내보낼 구획 — §7 `sections`. 고른 순서가 문서에 그대로 실린다. */
public enum class ExportSection(
    public val wireValue: String,
) {
    Basic("basic"),
    Skills("skills"),
    Projects("projects"),
    Awards("awards"),
    Summary("summary"),
    ;

    public companion object {
        public fun fromWireValue(value: String): ExportSection? = entries.firstOrNull { it.wireValue == value }
    }
}

/**
 * 내보내기 결과 — §7 `POST /export`.
 *
 * [format] 은 **서버가 실제로 만든** 형식이다. 요청한 형식을 그대로 들고 있으면 서버가 다른 형식으로
 * 내려보냈을 때 화면이 잘못된 확장자로 저장한다.
 */
public data class StrengthExport(
    public val format: ExportFormat,
    public val content: String,
)
