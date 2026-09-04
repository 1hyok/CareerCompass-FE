package com.careercompass.core.model.application

import java.io.InputStream
import java.time.Instant

/** 과거 지원서 저장 상한 — 기능 스펙 F1-4 (최대 10개). */
public const val MAX_PAST_APPLICATIONS: Int = 10

/** 업로드 파일 크기 상한 — API_SPEC v0.1 §4 (최대 10MB). */
public const val MAX_PAST_APPLICATION_FILE_BYTES: Long = 10L * 1024L * 1024L

/**
 * 지원서 항목 분류 — 기능 스펙 F1-4 「지원 동기 / 성장 배경 / 경험 기술 / 직무 역량 / 입사 후 포부 / 기타」.
 *
 * wire 값 중 명세에 적힌 것은 `motivation`·`aspiration`·`other` 뿐이다. 나머지 세 값은 명세의 한글 항목을
 * 그대로 영문화한 가정이며, 서버가 확정되면 이 한 곳만 고친다.
 */
public enum class PastApplicationCategory(
    public val wireValue: String,
) {
    Motivation("motivation"),
    Growth("growth"),
    Experience("experience"),
    Competency("competency"),
    Aspiration("aspiration"),
    Other("other"),
    ;

    public companion object {
        public fun fromWireValue(value: String): PastApplicationCategory? = entries.firstOrNull { it.wireValue == value }
    }
}

/** 지원서에서 분리된 항목 하나. [confident] 가 false 면 분류가 불확실해 사용자가 조정할 수 있다. */
public data class PastApplicationItem(
    val id: Long,
    val category: PastApplicationCategory,
    val content: String,
    val confident: Boolean,
) {
    init {
        require(content.isNotBlank()) { "content must not be blank" }
    }
}

/** 업로드·분류가 끝난 과거 지원서. */
public data class PastApplication(
    val id: Long,
    val label: String,
    val items: List<PastApplicationItem>,
    val createdAt: Instant?,
) {
    init {
        require(label.isNotBlank()) { "label must not be blank" }
        require(items.map(PastApplicationItem::id).distinct().size == items.size) { "item ids must be unique" }
    }
}

/** 지원하는 업로드 형식 — API_SPEC v0.1 §4 (PDF/DOCX/TXT). */
public enum class PastApplicationFileFormat(
    public val extension: String,
    public val mimeType: String,
) {
    Pdf("pdf", "application/pdf"),
    Docx("docx", "application/vnd.openxmlformats-officedocument.wordprocessingml.document"),
    Txt("txt", "text/plain"),
    ;

    public companion object {
        public fun fromFileName(fileName: String): PastApplicationFileFormat? {
            val extension = fileName.substringAfterLast('.', missingDelimiterValue = "")
            return entries.firstOrNull { it.extension.equals(extension, ignoreCase = true) }
        }
    }
}

/**
 * 업로드할 파일. 플랫폼 `Uri` 를 domain 에 들이지 않으려고 스트림 공급자로 받는다.
 *
 * @property openStream 호출마다 새 스트림을 연다 — 재시도 시 같은 파일을 다시 읽는다.
 */
public class UploadFile(
    public val fileName: String,
    public val sizeBytes: Long,
    public val openStream: () -> InputStream,
) {
    public val format: PastApplicationFileFormat =
        requireNotNull(PastApplicationFileFormat.fromFileName(fileName)) {
            "Unsupported application document format: $fileName"
        }

    init {
        require(fileName.isNotBlank()) { "fileName must not be blank" }
        require(sizeBytes in 1..MAX_PAST_APPLICATION_FILE_BYTES) {
            "sizeBytes must be within 1..$MAX_PAST_APPLICATION_FILE_BYTES"
        }
    }

    override fun toString(): String = "UploadFile(fileName=$fileName, sizeBytes=$sizeBytes, format=$format)"
}
