package com.cambridge.feature.feed.presentation

import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.android.tools.screenshot.PreviewTest
import com.cambridge.feature.feed.presentation.board.BoardCollectCycle
import com.cambridge.feature.feed.presentation.board.BoardEditSheetContent
import com.cambridge.feature.feed.presentation.board.BoardEditUiState
import com.cambridge.feature.feed.presentation.board.BoardType
import com.careercompass.core.ui.theme.CareerCompassTheme

@PreviewTest
@Preview(name = "Board edit sheet", widthDp = 360, heightDp = 772)
@Composable
public fun BoardEditSheetPreview() {
    BoardEditPreviewSurface(state = boardEditPreviewState())
}

@PreviewTest
@Preview(name = "Board edit sheet saving", widthDp = 360, heightDp = 772)
@Composable
public fun BoardEditSheetSavingPreview() {
    BoardEditPreviewSurface(state = boardEditPreviewState().copy(isSaving = true))
}

@PreviewTest
@Preview(name = "Board edit sheet name error", widthDp = 360, heightDp = 772)
@Composable
public fun BoardEditSheetNameErrorPreview() {
    BoardEditPreviewSurface(
        state =
            boardEditPreviewState().copy(
                name = "",
                nameError = "게시판 이름을 입력해 주세요",
                hasChanges = false,
            ),
    )
}

@Composable
private fun BoardEditPreviewSurface(state: BoardEditUiState) {
    CareerCompassTheme {
        Surface(color = CareerCompassTheme.colors.surface) {
            BoardEditSheetContent(state = state, onEvent = {})
        }
    }
}

/** 이름·주기를 바꾼 상태 — 저장 버튼이 켜진 모습을 담는다. */
private fun boardEditPreviewState(): BoardEditUiState =
    BoardEditUiState(
        boardName = "건국대 공지사항",
        url = "https://konkuk.ac.kr/board/notice",
        name = "건국대 취업 공지",
        nameError = null,
        type = BoardType.Employment,
        cycle = BoardCollectCycle.TwiceDaily,
        isSaving = false,
        hasChanges = true,
    )
