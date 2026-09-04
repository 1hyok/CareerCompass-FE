package com.careercompass.core.domain.repository

import com.careercompass.core.model.board.Board
import com.careercompass.core.model.board.BoardDetection
import com.careercompass.core.model.board.BoardRegistration
import com.careercompass.core.model.board.BoardUpdate

/** 게시판 계약 — API_SPEC v0.1 §5 `/boards`. */
public interface BoardRepository {
    /** `POST /boards/detect` — 등록 전 구조 감지·미리보기. */
    public suspend fun detect(url: String): Result<BoardDetection>

    /** `POST /boards` — 등록 확정. 완료 시 서버가 즉시 1회 수집한다. */
    public suspend fun register(registration: BoardRegistration): Result<Board>

    public suspend fun getBoards(): Result<List<Board>>

    /** `PATCH /boards/{id}` — 빈 수정은 요청하지 않는다. */
    public suspend fun update(
        id: Long,
        update: BoardUpdate,
    ): Result<Board>

    public suspend fun delete(id: Long): Result<Unit>

    /** `POST /boards/{id}/retry` — 수집 실패 게시판 재시도. */
    public suspend fun retry(id: Long): Result<Unit>
}
