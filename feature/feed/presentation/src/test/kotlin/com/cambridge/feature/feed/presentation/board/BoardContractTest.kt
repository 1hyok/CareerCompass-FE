package com.cambridge.feature.feed.presentation.board

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class BoardContractTest {
    @Test
    fun detectionSuccess_requiresOneToFivePreviewItems() {
        assertThrows(IllegalArgumentException::class.java) {
            BoardDetectionState.Success(preview = emptyList(), dateDetected = true)
        }
        assertThrows(IllegalArgumentException::class.java) {
            BoardDetectionState.Success(
                preview = List(6) { index -> samplePreviewItem(title = "게시글 $index") },
                dateDetected = true,
            )
        }
        assertEquals(
            5,
            BoardDetectionState
                .Success(
                    preview = List(5) { index -> samplePreviewItem(title = "게시글 $index") },
                    dateDetected = true,
                ).preview.size,
        )
    }

    @Test
    fun previewItem_rejectsBlankStringsButAllowsNullDate() {
        assertThrows(IllegalArgumentException::class.java) { samplePreviewItem(title = " ") }
        assertThrows(IllegalArgumentException::class.java) { samplePreviewItem().copy(url = " ") }
        assertThrows(IllegalArgumentException::class.java) { samplePreviewItem().copy(dateLabel = " ") }
        assertEquals(null, samplePreviewItem().copy(dateLabel = null).dateLabel)
    }

    @Test
    fun registerState_rejectsBlankUrlError() {
        assertThrows(IllegalArgumentException::class.java) {
            sampleRegisterState().copy(urlError = " ")
        }
    }

    @Test
    fun isDetectEnabled_requiresUrlAndNoInFlightWork() {
        assertTrue(sampleRegisterState().isDetectEnabled)
        assertFalse(sampleRegisterState().copy(url = " ").isDetectEnabled)
        assertFalse(sampleRegisterState().copy(detection = BoardDetectionState.Detecting).isDetectEnabled)
        assertFalse(sampleRegisterState().copy(isSubmitting = true).isDetectEnabled)
        assertTrue(
            sampleRegisterState()
                .copy(detection = BoardDetectionState.Failed(BoardDetectionFailure.Blocked))
                .isDetectEnabled,
        )
    }

    @Test
    fun isRegisterEnabled_requiresSuccessNameTypeAndNoSubmission() {
        val ready =
            sampleRegisterState().copy(
                detection = sampleSuccess(),
                name = "건국대 공지사항",
                type = BoardType.Employment,
            )

        assertTrue(ready.isRegisterEnabled)
        assertFalse(sampleRegisterState().isRegisterEnabled)
        assertFalse(ready.copy(detection = BoardDetectionState.Detecting).isRegisterEnabled)
        assertFalse(ready.copy(name = " ").isRegisterEnabled)
        assertFalse(ready.copy(type = null).isRegisterEnabled)
        assertFalse(ready.copy(isSubmitting = true).isRegisterEnabled)
    }

    @Test
    fun boardModel_rejectsBlankStringsAndNegativeCounts() {
        assertThrows(IllegalArgumentException::class.java) { sampleBoard().copy(id = " ") }
        assertThrows(IllegalArgumentException::class.java) { sampleBoard().copy(name = " ") }
        assertThrows(IllegalArgumentException::class.java) { sampleBoard().copy(url = " ") }
        assertThrows(IllegalArgumentException::class.java) { sampleBoard().copy(typeLabel = " ") }
        assertThrows(IllegalArgumentException::class.java) { sampleBoard().copy(failCount = -1) }
        assertThrows(IllegalArgumentException::class.java) { sampleBoard().copy(postingCount = -1) }
        assertThrows(IllegalArgumentException::class.java) { sampleBoard().copy(lastCollectedLabel = " ") }
        assertEquals(null, sampleBoard().copy(lastCollectedLabel = null).lastCollectedLabel)
        assertEquals(null, sampleBoard().copy(postingCount = null).postingCount)
    }

    @Test
    fun loadedList_rejectsEmptyAndDuplicateBoards() {
        assertThrows(IllegalArgumentException::class.java) {
            BoardListContentState.Loaded(emptyList())
        }
        assertThrows(IllegalArgumentException::class.java) {
            BoardListContentState.Loaded(listOf(sampleBoard(id = "dup"), sampleBoard(id = "dup")))
        }
    }

    @Test
    fun listState_enforcesMaxBoardCount() {
        assertThrows(IllegalArgumentException::class.java) {
            BoardListUiState(content = BoardListContentState.Empty, maxBoardCount = 0)
        }
        assertThrows(IllegalArgumentException::class.java) {
            BoardListUiState(
                content =
                    BoardListContentState.Loaded(
                        List(3) { index -> sampleBoard(id = "board-$index") },
                    ),
                maxBoardCount = 2,
            )
        }
        assertEquals(
            BOARD_DEFAULT_MAX_COUNT,
            BoardListUiState(content = BoardListContentState.Empty).maxBoardCount,
        )
    }

    @Test
    fun collectCycle_exposesHours() {
        assertEquals(24, BoardCollectCycle.Daily.hours)
        assertEquals(12, BoardCollectCycle.TwiceDaily.hours)
        assertEquals(168, BoardCollectCycle.Weekly.hours)
    }
}

private fun samplePreviewItem(title: String = "2026 SW 인턴 모집 안내"): BoardPreviewItemUiModel =
    BoardPreviewItemUiModel(
        title = title,
        url = "https://konkuk.ac.kr/board/notice/1",
        dateLabel = "2026-05-10",
    )

private fun sampleSuccess(): BoardDetectionState.Success =
    BoardDetectionState.Success(preview = listOf(samplePreviewItem()), dateDetected = true)

private fun sampleRegisterState(): BoardRegisterUiState =
    BoardRegisterUiState(
        url = "https://konkuk.ac.kr/board/notice",
        urlError = null,
        detection = BoardDetectionState.Idle,
        name = "",
        type = null,
        cycle = BoardCollectCycle.Daily,
        isSubmitting = false,
    )

private fun sampleBoard(id: String = "konkuk"): BoardUiModel =
    BoardUiModel(
        id = id,
        name = "건국대 공지사항",
        url = "https://konkuk.ac.kr/board/notice",
        type = BoardType.Employment,
        typeLabel = "채용",
        status = BoardStatus.Active,
        isActive = true,
        failCount = 0,
        lastCollectedLabel = "방금",
        postingCount = 12,
    )
