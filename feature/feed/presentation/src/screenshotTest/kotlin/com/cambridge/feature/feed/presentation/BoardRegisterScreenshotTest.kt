package com.cambridge.feature.feed.presentation

import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.android.tools.screenshot.PreviewTest
import com.cambridge.core.ui.theme.CareerCompassTheme
import com.cambridge.feature.feed.presentation.board.BoardCollectCycle
import com.cambridge.feature.feed.presentation.board.BoardDetectionFailure
import com.cambridge.feature.feed.presentation.board.BoardDetectionState
import com.cambridge.feature.feed.presentation.board.BoardPreviewItemUiModel
import com.cambridge.feature.feed.presentation.board.BoardRegisterScreen
import com.cambridge.feature.feed.presentation.board.BoardRegisterUiState
import com.cambridge.feature.feed.presentation.board.BoardType

@PreviewTest
@Preview(name = "Board register idle", widthDp = 360, heightDp = 772)
@Composable
public fun BoardRegisterIdlePreview() {
    BoardRegisterPreviewSurface(state = boardRegisterPreviewState())
}

@PreviewTest
@Preview(name = "Board register detecting", widthDp = 360, heightDp = 772)
@Composable
public fun BoardRegisterDetectingPreview() {
    BoardRegisterPreviewSurface(
        state = boardRegisterPreviewState().copy(detection = BoardDetectionState.Detecting),
    )
}

@PreviewTest
@Preview(name = "Board register timed out", widthDp = 360, heightDp = 772)
@Composable
public fun BoardRegisterTimedOutPreview() {
    BoardRegisterPreviewSurface(
        state =
            boardRegisterPreviewState().copy(
                url = "https://slow-portal.ac.kr/notice",
                detection = BoardDetectionState.TimedOut,
            ),
    )
}

@PreviewTest
@Preview(name = "Board register success", widthDp = 360, heightDp = 772)
@Composable
public fun BoardRegisterSuccessPreview() {
    BoardRegisterPreviewSurface(state = boardRegisterSuccessPreviewState())
}

/** 제출 중 — 진행 표시가 「등록하기」 바로 위, 스크롤되지 않는 하단에 서는지 본다(#146). */
@PreviewTest
@Preview(name = "Board register submitting", widthDp = 360, heightDp = 772)
@Composable
public fun BoardRegisterSubmittingPreview() {
    BoardRegisterPreviewSurface(state = boardRegisterSuccessPreviewState().copy(isSubmitting = true))
}

@PreviewTest
@Preview(name = "Board register failed", widthDp = 360, heightDp = 772)
@Composable
public fun BoardRegisterFailedPreview() {
    BoardRegisterPreviewSurface(
        state =
            boardRegisterPreviewState().copy(
                url = "https://example-portal.kr/notice",
                detection = BoardDetectionState.Failed(BoardDetectionFailure.LoginRequired),
            ),
    )
}

/** 점검(503) — 타임아웃(경고 톤 + 「다시 시도」)과 한눈에 갈리는지 본다. 여기에는 행동 버튼이 없다. */
@PreviewTest
@Preview(name = "Board register maintenance", widthDp = 360, heightDp = 772)
@Composable
public fun BoardRegisterMaintenancePreview() {
    BoardRegisterPreviewSurface(
        state =
            boardRegisterPreviewState().copy(
                url = "https://konkuk.ac.kr/notice",
                detection = BoardDetectionState.Maintenance,
            ),
    )
}

@Composable
private fun BoardRegisterPreviewSurface(state: BoardRegisterUiState) {
    CareerCompassTheme {
        Surface(color = CareerCompassTheme.colors.subtleSurface) {
            BoardRegisterScreen(state = state, onEvent = {})
        }
    }
}

private fun boardRegisterSuccessPreviewState(): BoardRegisterUiState =
    boardRegisterPreviewState().copy(
        detection =
            BoardDetectionState.Success(
                preview =
                    listOf(
                        BoardPreviewItemUiModel(
                            title = "2026 SW 인턴 모집 안내",
                            url = "https://konkuk.ac.kr/board/notice/1",
                            dateLabel = "2026-05-10",
                        ),
                        BoardPreviewItemUiModel(
                            title = "1학기 우수학생 장학금 추가 모집",
                            url = "https://konkuk.ac.kr/board/notice/2",
                            dateLabel = "2026-05-08",
                        ),
                        BoardPreviewItemUiModel(
                            title = "대학 IT 페스티벌 참가자 모집",
                            url = "https://konkuk.ac.kr/board/notice/3",
                            dateLabel = "2026-05-07",
                        ),
                    ),
                dateDetected = true,
            ),
        name = "건국대 학교 공지사항",
        type = BoardType.Employment,
    )

private fun boardRegisterPreviewState(): BoardRegisterUiState =
    BoardRegisterUiState(
        url = "https://konkuk.ac.kr/board/notice",
        urlError = null,
        detection = BoardDetectionState.Idle,
        name = "",
        type = null,
        cycle = BoardCollectCycle.Daily,
        isSubmitting = false,
    )
