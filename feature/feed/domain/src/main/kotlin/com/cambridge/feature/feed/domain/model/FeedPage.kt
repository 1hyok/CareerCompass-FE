package com.cambridge.feature.feed.domain.model

import com.careercompass.core.model.posting.Posting

/**
 * 피드 한 페이지 — 서버 페이지에 클라이언트 필터(마감일·검색어)를 적용한 결과.
 *
 * 클라이언트 필터로 항목이 줄어들 수 있어 [postings] 가 비어 있어도 [nextCursor] 가 남아 있을 수 있다.
 * 호출자는 목록이 비었다고 끝으로 보지 말고 [hasNext] 로 판단해야 한다.
 */
public data class FeedPage(
    val postings: List<Posting>,
    val nextCursor: String?,
) {
    init {
        require(nextCursor == null || nextCursor.isNotBlank()) { "nextCursor must be null or non-blank" }
    }

    public val hasNext: Boolean get() = nextCursor != null
}
