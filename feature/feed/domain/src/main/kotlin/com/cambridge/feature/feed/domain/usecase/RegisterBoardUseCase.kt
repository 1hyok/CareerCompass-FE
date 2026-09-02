package com.cambridge.feature.feed.domain.usecase

import com.cambridge.core.domain.repository.BoardRepository
import com.cambridge.core.model.board.Board
import com.cambridge.core.model.board.BoardRegistration
import com.cambridge.core.model.board.MAX_BOARDS
import com.cambridge.feature.feed.domain.error.FeedFailure
import com.cambridge.feature.feed.domain.model.BoardUrl
import javax.inject.Inject

/**
 * 게시판 등록을 확정한다 — `POST /boards`.
 *
 * 등록 전에 목록을 조회해 상한([MAX_BOARDS], 기능 스펙 F2-1)을 확인하고 닿았으면 요청 없이
 * [FeedFailure.BoardLimitReached] 로 실패한다. 목록 조회 자체가 실패하면 그 실패를 그대로 돌려준다 —
 * 같은 원인으로 등록도 실패할 가능성이 커 서버 상한 검사에 기대지 않는다.
 * URL 은 [BoardUrl] 로 다시 정규화해 감지 때와 같은 값이 실리게 한다.
 */
public class RegisterBoardUseCase
    @Inject
    constructor(
        private val boardRepository: BoardRepository,
    ) {
        public suspend operator fun invoke(registration: BoardRegistration): Result<Board> {
            val url = BoardUrl.normalize(registration.url).getOrElse { return Result.failure(it) }
            val boards = boardRepository.getBoards().getOrElse { return Result.failure(it) }
            if (boards.size >= MAX_BOARDS) return Result.failure(FeedFailure.BoardLimitReached(MAX_BOARDS))
            return boardRepository.register(registration.copy(url = url))
        }
    }
