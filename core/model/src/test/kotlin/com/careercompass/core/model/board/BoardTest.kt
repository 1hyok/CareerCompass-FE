package com.careercompass.core.model.board

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class BoardTest {
    @Test
    fun `알 수 없는 수집 상태는 Unknown 으로 받는다`() {
        assertEquals(BoardStatus.Active, BoardStatus.fromWireValue("active"))
        assertEquals(BoardStatus.Unknown, BoardStatus.fromWireValue("throttled"))
        assertEquals(BoardStatus.Unknown, BoardStatus.fromWireValue(""))
    }

    @Test
    fun `감지 성공은 미리보기가 있어야 하고 성공만 등록 가능하다`() {
        assertThrows(IllegalArgumentException::class.java) {
            BoardDetection(status = BoardDetectionStatus.Success, preview = emptyList(), hasDateSelector = true)
        }
        val blocked = BoardDetection(status = BoardDetectionStatus.Blocked, preview = emptyList(), hasDateSelector = false)
        assertFalse(blocked.isRegistrable)
        val success =
            BoardDetection(
                status = BoardDetectionStatus.Success,
                preview = listOf(BoardPreviewItem(title = "장학금 공지", url = "https://konkuk.ac.kr/1", date = null)),
                hasDateSelector = false,
            )
        assertTrue(success.isRegistrable)
    }

    @Test
    fun `등록 입력은 기본 주기 24시간이고 빈 값을 거부한다`() {
        assertEquals(24, BoardRegistration(url = "https://konkuk.ac.kr/board", name = "학교 공지", type = BoardType.Scholarship).cycleHours)
        assertThrows(IllegalArgumentException::class.java) {
            BoardRegistration(url = " ", name = "학교 공지", type = BoardType.Scholarship)
        }
        assertTrue(BoardUpdate().isEmpty)
    }
}
