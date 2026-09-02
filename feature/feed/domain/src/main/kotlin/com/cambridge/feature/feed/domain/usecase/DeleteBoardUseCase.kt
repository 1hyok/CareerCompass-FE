package com.cambridge.feature.feed.domain.usecase

import com.cambridge.core.domain.repository.BoardRepository
import javax.inject.Inject

/** 게시판 삭제 — `DELETE /boards/{id}`. */
public class DeleteBoardUseCase
    @Inject
    constructor(
        private val boardRepository: BoardRepository,
    ) {
        public suspend operator fun invoke(id: Long): Result<Unit> = boardRepository.delete(id)
    }
