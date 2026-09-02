package com.cambridge.core.model.paging

/** 커서 페이징 결과 — API_SPEC v0.1 「페이징」. [nextCursor] 가 null 이면 마지막 페이지다. */
public data class CursorPage<T>(
    val items: List<T>,
    val nextCursor: String?,
) {
    init {
        require(nextCursor == null || nextCursor.isNotBlank()) { "nextCursor must be null or non-blank" }
    }

    public val hasNext: Boolean get() = nextCursor != null

    public companion object {
        public fun <T> empty(): CursorPage<T> = CursorPage(items = emptyList(), nextCursor = null)
    }
}
