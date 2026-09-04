package com.cambridge.feature.feed.domain.usecase

import com.careercompass.core.domain.repository.BoardRepository
import com.careercompass.core.model.board.Board
import com.careercompass.core.model.board.BoardUpdate
import javax.inject.Inject

/** 게시판 부분 수정 — `PATCH /boards/{id}`. 바꿀 필드가 하나도 없는 수정은 프로그래밍 오류로 본다. */
public class UpdateBoardUseCase
    @Inject
    constructor(
        private val boardRepository: BoardRepository,
    ) {
        public suspend operator fun invoke(
            id: Long,
            update: BoardUpdate,
        ): Result<Board> {
            require(!update.isEmpty) { "update must change at least one field" }
            return boardRepository.update(id, update)
        }
    }
