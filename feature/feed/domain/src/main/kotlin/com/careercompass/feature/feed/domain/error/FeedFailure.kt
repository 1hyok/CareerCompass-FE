package com.careercompass.feature.feed.domain.error

import com.careercompass.core.model.board.MAX_BOARDS

/**
 * 피드 도메인이 **요청 전에** 확정하는 실패. 서버가 돌려준 실패는
 * [CoreDataFailure][com.careercompass.core.domain.error.CoreDataFailure] 로 따로 흐른다.
 */
public sealed class FeedFailure(
    message: String,
) : Exception(message) {
    /** 게시판 URL 형태가 아니다 — 빈 값, `http(s)` 외 스킴, 호스트 없음, 내부 공백. [input] 은 사용자 입력 원문. */
    public class InvalidBoardUrl(
        public val input: String,
    ) : FeedFailure("invalid board url")

    /** 등록 게시판이 이미 상한([limit], 기능 스펙 F2-1 최대 20개)에 닿았다. */
    public class BoardLimitReached(
        public val limit: Int = MAX_BOARDS,
    ) : FeedFailure("board limit reached ($limit)")
}
