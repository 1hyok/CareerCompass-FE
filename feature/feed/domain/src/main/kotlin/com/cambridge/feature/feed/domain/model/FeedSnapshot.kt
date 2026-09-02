package com.cambridge.feature.feed.domain.model

import com.cambridge.core.model.posting.Posting
import com.cambridge.core.model.posting.PostingQuery
import java.time.Instant

/**
 * 마지막으로 성공한 **기본 조건([FeedQuery.isDefault]) 첫 페이지**의 사본 — 오프라인 모드가 보여 주는 목록.
 *
 * 다른 조건·다음 페이지는 저장하지 않는다. 조건이 걸린 결과를 저장하면 오프라인에서 「전체」로 보이는 목록이
 * 사실은 필터된 부분집합이 되어, 마감 임박 공고가 빠진 줄도 모른 채 읽게 된다.
 *
 * @property postings 비어 있지 않고 id 가 유일하다. 빈 스냅샷은 「스냅샷 없음」과 구분할 이유가 없어 만들 수 없다.
 * @property savedAt 저장 시각. 화면 배너의 「n월 n일 hh:mm 기준」 근거다.
 */
public data class FeedSnapshot(
    val postings: List<Posting>,
    val savedAt: Instant,
) {
    init {
        require(postings.isNotEmpty()) { "postings must not be empty" }
        require(postings.map(Posting::id).distinct().size == postings.size) { "posting ids must be unique" }
    }

    public companion object {
        /** 저장 상한 — 첫 페이지 한 장. 그 이상은 data 계층이 잘라 저장한다. */
        public const val MAX_POSTINGS: Int = PostingQuery.DEFAULT_LIMIT
    }
}
