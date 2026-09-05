package com.careercompass.feature.notification.domain.usecase

import com.careercompass.core.model.paging.CursorPage
import com.careercompass.feature.notification.domain.model.BoardNotificationSetting
import com.careercompass.feature.notification.domain.model.DEFAULT_NOTIFICATION_PAGE_SIZE
import com.careercompass.feature.notification.domain.model.NotificationItem
import com.careercompass.feature.notification.domain.model.NotificationSettings
import com.careercompass.feature.notification.domain.model.NotificationTarget
import com.careercompass.feature.notification.domain.model.NotificationType
import com.careercompass.feature.notification.domain.model.QuietHours
import com.careercompass.feature.notification.domain.testing.FakeNotificationRepository
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException
import java.time.Instant
import java.time.LocalTime

class NotificationUseCasesTest {
    private val repository = FakeNotificationRepository()

    @Test
    fun `첫 페이지는 커서 없이 기본 크기로 조회한다`() =
        runTest {
            GetNotificationsUseCase(repository)()

            assertEquals(listOf(null to DEFAULT_NOTIFICATION_PAGE_SIZE), repository.requestedPages)
        }

    @Test
    fun `다음 페이지는 받은 커서를 그대로 넘긴다`() =
        runTest {
            repository.page = CursorPage(items = listOf(item()), nextCursor = "eyJ")

            val page = GetNotificationsUseCase(repository)().getOrThrow()
            GetNotificationsUseCase(repository)(cursor = page.nextCursor, limit = 5)

            assertEquals("eyJ" to 5, repository.requestedPages.last())
        }

    @Test
    fun `크기가 0 이하인 조회는 요청하지 않는다`() =
        runTest {
            val failure = runCatching { GetNotificationsUseCase(repository)(limit = 0) }.exceptionOrNull()

            assertTrue(failure is IllegalArgumentException)
            assertTrue(repository.requestedPages.isEmpty())
        }

    @Test
    fun `모두 읽음은 한 번만 나간다`() =
        runTest {
            repository.page = CursorPage(items = listOf(item(isRead = false)), nextCursor = null)

            val result = MarkAllNotificationsReadUseCase(repository)()

            assertTrue(result.isSuccess)
            assertEquals(1, repository.markAllReadCount)
            assertTrue(
                repository.page.items
                    .single()
                    .isRead,
            )
        }

    @Test
    fun `설정 조회 실패는 그대로 전달된다`() =
        runTest {
            val failure = IOException("offline")
            repository.onGetSettings = { Result.failure(failure) }

            val result = GetNotificationSettingsUseCase(repository)()

            assertEquals(failure, result.exceptionOrNull())
        }

    @Test
    fun `설정 저장은 전체를 실어 보낸다`() =
        runTest {
            val settings = settings(quietHours = QuietHours(start = LocalTime.of(23, 0), end = LocalTime.of(8, 0)))

            UpdateNotificationSettingsUseCase(repository)(settings)

            assertEquals(settings, repository.savedSettings.single())
        }

    @Test
    fun `종류 스위치는 바뀐 값만 담아 저장한다`() =
        runTest {
            val current = settings()

            val updated = SetNotificationTypeEnabledUseCase(repository)(current, NotificationType.DueSoon, enabled = false).getOrThrow()

            assertEquals(false, updated.dueSoon)
            assertEquals(true, updated.newPosting)
            assertEquals(updated, repository.savedSettings.single())
        }

    @Test
    fun `이미 그 값인 종류 스위치는 서버를 부르지 않는다`() =
        runTest {
            val current = settings()

            val result = SetNotificationTypeEnabledUseCase(repository)(current, NotificationType.NewPosting, enabled = true)

            assertEquals(current, result.getOrThrow())
            assertTrue(repository.savedSettings.isEmpty())
        }

    @Test
    fun `게시판 스위치는 없던 줄을 만들어 저장한다`() =
        runTest {
            val updated = SetBoardNotificationEnabledUseCase(repository)(settings(), boardId = 3, enabled = false).getOrThrow()

            assertEquals(listOf(BoardNotificationSetting(boardId = 3, enabled = false)), updated.perBoard)
            assertEquals(updated, repository.savedSettings.single())
        }

    @Test
    fun `이미 꺼진 게시판 스위치는 서버를 부르지 않는다`() =
        runTest {
            val current = settings(perBoard = listOf(BoardNotificationSetting(boardId = 3, enabled = false)))

            val result = SetBoardNotificationEnabledUseCase(repository)(current, boardId = 3, enabled = false)

            assertEquals(current, result.getOrThrow())
            assertTrue(repository.savedSettings.isEmpty())
        }

    private fun item(isRead: Boolean = false): NotificationItem =
        NotificationItem(
            id = 9,
            type = NotificationType.DueSoon,
            title = "마감 D-1",
            body = "마감이 하루 남았어요",
            receivedAt = Instant.parse("2026-05-18T07:00:00Z"),
            isRead = isRead,
            target = NotificationTarget.PostingDetail(101),
        )

    private fun settings(
        quietHours: QuietHours? = null,
        perBoard: List<BoardNotificationSetting> = emptyList(),
    ): NotificationSettings =
        NotificationSettings(
            newPosting = true,
            dueSoon = true,
            boardError = true,
            weeklyReport = false,
            quietHours = quietHours,
            weekendOff = false,
            perBoard = perBoard,
        )
}
