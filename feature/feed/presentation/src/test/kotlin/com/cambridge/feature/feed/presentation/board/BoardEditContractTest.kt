package com.cambridge.feature.feed.presentation.board

import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class BoardEditContractTest {
    @Test
    fun editState_rejectsBlankBoardNameUrlAndError() {
        assertThrows(IllegalArgumentException::class.java) { sampleEditState().copy(boardName = " ") }
        assertThrows(IllegalArgumentException::class.java) { sampleEditState().copy(url = " ") }
        assertThrows(IllegalArgumentException::class.java) { sampleEditState().copy(nameError = " ") }
    }

    @Test
    fun isSaveEnabled_requiresNameChangesAndNoInFlightSave() {
        assertTrue(sampleEditState().isSaveEnabled)
        assertFalse(sampleEditState().copy(name = " ").isSaveEnabled)
        assertFalse(sampleEditState().copy(isSaving = true).isSaveEnabled)
        assertFalse(sampleEditState().copy(hasChanges = false).isSaveEnabled)
    }

    private fun sampleEditState(): BoardEditUiState =
        BoardEditUiState(
            boardName = "건국대 공지사항",
            url = "https://konkuk.ac.kr/board/notice",
            name = "건국대 취업 공지",
            nameError = null,
            type = BoardType.Employment,
            cycle = BoardCollectCycle.Daily,
            isSaving = false,
            hasChanges = true,
        )
}
