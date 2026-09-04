package com.cambridge.feature.feed.domain.usecase

import com.careercompass.core.domain.repository.BoardRepository
import javax.inject.Inject

/** 수집 실패 게시판 재시도 — `POST /boards/{id}/retry`. */
public class RetryBoardUseCase
    @Inject
    constructor(
        private val boardRepository: BoardRepository,
    ) {
        public suspend operator fun invoke(id: Long): Result<Unit> = boardRepository.retry(id)
    }
