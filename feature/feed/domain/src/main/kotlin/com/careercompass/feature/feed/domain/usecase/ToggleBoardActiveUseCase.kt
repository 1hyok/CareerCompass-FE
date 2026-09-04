package com.careercompass.feature.feed.domain.usecase

import com.careercompass.core.domain.repository.BoardRepository
import com.careercompass.core.model.board.Board
import com.careercompass.core.model.board.BoardUpdate
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
