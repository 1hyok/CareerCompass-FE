package com.cambridge.feature.feed.presentation

import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.android.tools.screenshot.PreviewTest
import com.cambridge.core.ui.theme.CareerCompassTheme
import com.cambridge.feature.feed.presentation.board.BoardListContentState
import com.cambridge.feature.feed.presentation.board.BoardListScreen
import com.cambridge.feature.feed.presentation.board.BoardListUiState
import com.cambridge.feature.feed.presentation.board.BoardStatus
import com.cambridge.feature.feed.presentation.board.BoardType
import com.cambridge.feature.feed.presentation.board.BoardUiModel

@PreviewTest
@Preview(name = "Board list loaded", widthDp = 360, heightDp = 772)
@Composable
public fun BoardListLoadedPreview() {
    BoardListPreviewSurface(
        state = BoardListUiState(content = BoardListContentState.Loaded(boardListPreviewBoards())),
    )
}

@PreviewTest
@Preview(name = "Board list empty", widthDp = 360, heightDp = 772)
@Composable
public fun BoardListEmptyPreview() {
    BoardListPreviewSurface(state = BoardListUiState(content = BoardListContentState.Empty))
}

@Composable
private fun BoardListPreviewSurface(state: BoardListUiState) {
    CareerCompassTheme {
        Surface(color = CareerCompassTheme.colors.subtleSurface) {
            BoardListScreen(state = state, onEvent = {})
        }
    }
}

private fun boardListPreviewBoards(): List<BoardUiModel> =
    listOf(
        BoardUiModel(
            id = "konkuk",
            name = "건국대 공지사항",
            url = "https://konkuk.ac.kr/board/notice",
            type = BoardType.Employment,
            typeLabel = "채용",
            status = BoardStatus.Active,
            isActive = true,
            failCount = 0,
            lastCollectedLabel = "방금",
            postingCount = 12,
        ),
        BoardUiModel(
            id = "cs-scholarship",
            name = "정보과학대학 장학 안내",
            url = "https://konkuk.ac.kr/cs/scholarship",
            type = BoardType.Scholarship,
            typeLabel = "장학금",
            status = BoardStatus.Active,
            isActive = true,
            failCount = 0,
            lastCollectedLabel = "2시간 전",
            postingCount = 5,
        ),
        BoardUiModel(
            id = "wettheidol",
            name = "공모전 사이트",
            url = "https://wettheidol.com/contest/list",
            type = BoardType.Contest,
            typeLabel = "공모전",
            status = BoardStatus.Failing,
            isActive = false,
            failCount = 3,
            lastCollectedLabel = "6시간 전",
            postingCount = 4,
        ),
        BoardUiModel(
            id = "naver",
            name = "네이버 채용",
            url = "https://recruit.navercorp.com/rcrt/list.do",
            type = BoardType.Employment,
            typeLabel = "채용",
            status = BoardStatus.Paused,
            isActive = false,
            failCount = 0,
            lastCollectedLabel = null,
            postingCount = 0,
        ),
    )
