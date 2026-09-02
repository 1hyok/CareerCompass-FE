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
@Preview(name = "Board register success", widthDp = 360, heightDp = 772)
@Composable
public fun BoardRegisterSuccessPreview() {
    BoardRegisterPreviewSurface(
        state =
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
            ),
    )
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

@Composable
private fun BoardRegisterPreviewSurface(state: BoardRegisterUiState) {
    CareerCompassTheme {
        Surface(color = CareerCompassTheme.colors.subtleSurface) {
            BoardRegisterScreen(state = state, onEvent = {})
        }
    }
}

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
