package com.careercompass.feature.notification.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalTime

class NotificationSettingsTest {
    @Test
    fun `방해 금지는 자정을 넘을 수 있다`() {
        val quietHours = QuietHours(start = LocalTime.of(23, 0), end = LocalTime.of(8, 0))

        assertEquals(LocalTime.of(23, 0), quietHours.start)
        assertEquals(LocalTime.of(8, 0), quietHours.end)
    }

    @Test
    fun `시작과 종료가 같은 방해 금지는 만들 수 없다`() {
        assertThrows(IllegalArgumentException::class.java) {
            QuietHours(start = LocalTime.of(9, 0), end = LocalTime.of(9, 0))
        }
    }

    @Test
    fun `같은 게시판이 두 줄이면 설정이 아니다`() {
        assertThrows(IllegalArgumentException::class.java) {
            settings(
                perBoard =
                    listOf(
                        BoardNotificationSetting(boardId = 3, enabled = true),
                        BoardNotificationSetting(boardId = 3, enabled = false),
                    ),
            )
        }
    }

    @Test
    fun `종류별 현재 값을 한 자리에서 읽는다`() {
        val settings = settings(weeklyReport = false)

        assertTrue(settings.isEnabled(NotificationType.NewPosting))
        assertFalse(settings.isEnabled(NotificationType.WeeklyReport))
    }

    @Test
    fun `종류 스위치 하나만 바뀐다`() {
        val updated = settings().withTypeEnabled(NotificationType.DueSoon, enabled = false)

        assertFalse(updated.dueSoon)
        assertTrue(updated.newPosting)
        assertTrue(updated.boardError)
    }

    @Test
    fun `설정에 없는 게시판은 켜진 것으로 보고 끄면 줄이 생긴다`() {
        val before = settings()
        assertTrue(before.isBoardEnabled(boardId = 3))

        val updated = before.withBoardEnabled(boardId = 3, enabled = false)

        assertEquals(listOf(BoardNotificationSetting(boardId = 3, enabled = false)), updated.perBoard)
        assertFalse(updated.isBoardEnabled(boardId = 3))
    }

    @Test
    fun `이미 있는 게시판 줄은 늘어나지 않고 값만 바뀐다`() {
        val before =
            settings(
                perBoard =
                    listOf(
                        BoardNotificationSetting(boardId = 3, enabled = true),
                        BoardNotificationSetting(boardId = 4, enabled = true),
                    ),
            )

        val updated = before.withBoardEnabled(boardId = 3, enabled = false)

        assertEquals(2, updated.perBoard.size)
        assertFalse(updated.isBoardEnabled(boardId = 3))
        assertTrue(updated.isBoardEnabled(boardId = 4))
    }

    private fun settings(
        weeklyReport: Boolean = false,
        perBoard: List<BoardNotificationSetting> = emptyList(),
    ): NotificationSettings =
        NotificationSettings(
            newPosting = true,
            dueSoon = true,
            boardError = true,
            weeklyReport = weeklyReport,
            quietHours = null,
            weekendOff = false,
            perBoard = perBoard,
        )
}
