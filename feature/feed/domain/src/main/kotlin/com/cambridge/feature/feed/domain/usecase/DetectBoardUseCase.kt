package com.cambridge.feature.feed.domain.usecase

import com.cambridge.feature.feed.domain.model.BoardUrl
import com.careercompass.core.domain.repository.BoardRepository
import com.careercompass.core.model.board.BoardDetection
import javax.inject.Inject

/**
 * 감지 결과와 함께 실제로 감지에 쓴 정규화 URL 을 돌려준다 — 등록 확정([RegisterBoardUseCase]) 때
 * 같은 URL 을 보내야 하므로 호출자가 다시 정규화하지 않아도 되게 한다.
 */
public data class BoardDetectionOutcome(
    val url: String,
    val detection: BoardDetection,
)

/**
 * 게시판 URL 을 정규화·검증한 뒤 구조를 감지한다 — `POST /boards/detect`.
 *
 * 형태가 잘못된 입력은 요청 없이 [FeedFailure.InvalidBoardUrl][com.cambridge.feature.feed.domain.error.FeedFailure.InvalidBoardUrl]
 * 로 실패한다(규칙은 [BoardUrl]).
 */
public class DetectBoardUseCase
    @Inject
    constructor(
        private val boardRepository: BoardRepository,
    ) {
        public suspend operator fun invoke(rawUrl: String): Result<BoardDetectionOutcome> {
            val url = BoardUrl.normalize(rawUrl).getOrElse { return Result.failure(it) }
            return boardRepository.detect(url).map { BoardDetectionOutcome(url = url, detection = it) }
        }
    }
