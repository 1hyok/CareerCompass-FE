package com.cambridge.feature.feed.domain.usecase

import com.cambridge.core.domain.repository.BoardRepository
import com.cambridge.core.model.board.Board
import com.cambridge.core.model.board.BoardUpdate
import javax.inject.Inject

/** 게시판 수집 ON/OFF — `PATCH /boards/{id}` 의 `isActive`. */
public class ToggleBoardActiveUseCase
    @Inject
    constructor(
        private val boardRepository: BoardRepository,
    ) {
        public suspend operator fun invoke(
            id: Long,
            isActive: Boolean,
        ): Result<Board> = boardRepository.update(id, BoardUpdate(isActive = isActive))
    }
