package com.careercompass.feature.notification.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class NotificationTypeTest {
    @Test
    fun `종류는 넷이고 설정 스키마의 키와 같다`() {
        assertEquals(
            listOf("newPosting", "dueSoon", "boardError", "weeklyReport"),
            NotificationType.entries.map(NotificationType::wireValue),
        )
    }

    @Test
    fun `서버 값에서 종류로 돌아온다`() {
        NotificationType.entries.forEach { type ->
            assertEquals(type, NotificationType.fromWireValue(type.wireValue))
        }
    }

    @Test
    fun `모르는 값은 종류가 아니다`() {
        assertNull(NotificationType.fromWireValue("boardCollected"))
        assertNull(NotificationType.fromWireValue(""))
    }

    @Test
    fun `종류마다 가는 화면이 다르다`() {
        assertEquals(NotificationTarget.BoardFeed(3), NotificationType.NewPosting.targetOf(3))
        assertEquals(NotificationTarget.PostingDetail(101), NotificationType.DueSoon.targetOf(101))
        assertEquals(NotificationTarget.BoardManagement(3), NotificationType.BoardError.targetOf(3))
    }

    @Test
    fun `주간 리포트는 대상 id 없이 피드로 간다`() {
        assertEquals(NotificationTarget.Feed, NotificationType.WeeklyReport.targetOf(null))
        assertEquals(NotificationTarget.Feed, NotificationType.WeeklyReport.targetOf(7))
    }

    @Test
    fun `대상 id 가 필요한 종류는 id 가 없으면 갈 곳이 없다`() {
        assertNull(NotificationType.NewPosting.targetOf(null))
        assertNull(NotificationType.DueSoon.targetOf(null))
        assertNull(NotificationType.BoardError.targetOf(null))
    }
}
