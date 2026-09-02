package com.cambridge.feature.feed.domain.usecase

import com.cambridge.core.domain.repository.BoardRepository
import com.cambridge.core.model.board.Board
import javax.inject.Inject

/** 내 게시판 목록 — `GET /boards`. */
public class GetBoardsUseCase
    @Inject
    constructor(
        private val boardRepository: BoardRepository,
    ) {
        public suspend operator fun invoke(): Result<List<Board>> = boardRepository.getBoards()
    }
