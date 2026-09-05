package com.careercompass.feature.notification.data

import com.careercompass.core.domain.error.CoreDataFailure
import com.careercompass.core.network.dto.BoardNotificationDto
import com.careercompass.core.network.dto.NotificationDto
import com.careercompass.core.network.dto.NotificationListDto
import com.careercompass.core.network.dto.NotificationSettingsDto
import com.careercompass.core.network.dto.QuietHoursDto
import com.careercompass.core.network.model.ApiException
import com.careercompass.core.network.model.BaseResponse
import com.careercompass.core.network.service.NotificationApiService
import com.careercompass.feature.notification.domain.model.BoardNotificationSetting
import com.careercompass.feature.notification.domain.model.NotificationTarget
import com.careercompass.feature.notification.domain.model.NotificationType
import com.careercompass.feature.notification.domain.model.QuietHours
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException
import java.net.UnknownHostException
import java.time.Instant
import java.time.LocalTime

class NotificationRepositoryImplTest {
    private class FakeNotificationApi(
        var items: List<NotificationDto> = emptyList(),
        var nextCursor: String? = null,
        var settings: NotificationSettingsDto = settingsDto(),
    ) : NotificationApiService {
        val listCalls = mutableListOf<Pair<String?, Int>>()
        val savedSettings = mutableListOf<NotificationSettingsDto>()
        var markAllReadCount: Int = 0
        var listThrows: Throwable? = null
        var settingsThrows: Throwable? = null

        override suspend fun getNotifications(
            cursor: String?,
            limit: Int,
        ): BaseResponse<NotificationListDto> {
            listCalls += cursor to limit
            listThrows?.let { throw it }
            return BaseResponse(ok = true, data = NotificationListDto(notifications = items, nextCursor = nextCursor))
        }

        override suspend fun markAllRead(): BaseResponse<Unit> {
            markAllReadCount += 1
            return BaseResponse(ok = true)
        }

        override suspend fun getSettings(): BaseResponse<NotificationSettingsDto> {
            settingsThrows?.let { throw it }
            return BaseResponse(ok = true, data = settings)
        }

        override suspend fun updateSettings(body: NotificationSettingsDto): BaseResponse<NotificationSettingsDto> {
            savedSettings += body
            return BaseResponse(ok = true, data = body)
        }
    }

    @Test
    fun `목록은 커서와 크기를 그대로 실어 보낸다`() =
        runTest {
            val api = FakeNotificationApi(items = listOf(dto()), nextCursor = "eyJ")

            val page = NotificationRepositoryImpl(api).getNotifications(cursor = "prev", limit = 20).getOrThrow()

            assertEquals(listOf("prev" to 20), api.listCalls)
            assertEquals("eyJ", page.nextCursor)
            assertEquals(1, page.items.size)
        }

    @Test
    fun `항목은 종류와 딥링크 대상으로 옮겨진다`() =
        runTest {
            val api =
                FakeNotificationApi(
                    items =
                        listOf(
                            dto(id = 1, type = "newPosting", targetId = 3),
                            dto(id = 2, type = "dueSoon", targetId = 101),
                            dto(id = 3, type = "boardError", targetId = 3),
                            dto(id = 4, type = "weeklyReport", targetId = null),
                        ),
                )

            val items = NotificationRepositoryImpl(api).getNotifications(cursor = null, limit = 20).getOrThrow().items

            assertEquals(
                listOf(
                    NotificationTarget.BoardFeed(3),
                    NotificationTarget.PostingDetail(101),
                    NotificationTarget.BoardManagement(3),
                    NotificationTarget.Feed,
                ),
                items.map { it.target },
            )
            assertEquals(NotificationType.NewPosting, items.first().type)
            assertEquals(Instant.parse("2026-05-18T07:00:00Z"), items.first().receivedAt)
        }

    @Test
    fun `모르는 종류와 갈 곳 없는 항목은 목록에서 빠지고 나머지는 남는다`() =
        runTest {
            val api =
                FakeNotificationApi(
                    items =
                        listOf(
                            dto(id = 1, type = "boardCollected", targetId = 3),
                            dto(id = 2, type = "dueSoon", targetId = null),
                            dto(id = 3, type = "dueSoon", targetId = 101),
                        ),
                    nextCursor = "eyJ",
                )

            val page = NotificationRepositoryImpl(api).getNotifications(cursor = null, limit = 20).getOrThrow()

            assertEquals(listOf(3L), page.items.map { it.id })
            assertEquals("eyJ", page.nextCursor)
        }

    @Test
    fun `시각 형식이 계약과 다르면 페이지가 실패한다`() =
        runTest {
            val api = FakeNotificationApi(items = listOf(dto(receivedAt = "2026-05-18 07:00")))

            val result = NotificationRepositoryImpl(api).getNotifications(cursor = null, limit = 20)

            assertTrue(result.exceptionOrNull() is IllegalStateException)
        }

    @Test
    fun `크기가 0 이하면 요청하지 않는다`() =
        runTest {
            val api = FakeNotificationApi()

            val failure = runCatching { NotificationRepositoryImpl(api).getNotifications(cursor = null, limit = 0) }.exceptionOrNull()

            assertTrue(failure is IllegalArgumentException)
            assertTrue(api.listCalls.isEmpty())
        }

    @Test
    fun `모두 읽음은 한 번 나가고 성공한다`() =
        runTest {
            val api = FakeNotificationApi()

            assertTrue(NotificationRepositoryImpl(api).markAllRead().isSuccess)
            assertEquals(1, api.markAllReadCount)
        }

    @Test
    fun `설정은 방해 금지 시각까지 옮겨진다`() =
        runTest {
            val api = FakeNotificationApi()

            val settings = NotificationRepositoryImpl(api).getSettings().getOrThrow()

            assertEquals(QuietHours(start = LocalTime.of(23, 0), end = LocalTime.of(8, 0)), settings.quietHours)
            assertEquals(false, settings.weeklyReport)
            assertEquals(listOf(BoardNotificationSetting(boardId = 3, enabled = true)), settings.perBoard)
        }

    @Test
    fun `방해 금지가 없으면 꺼진 것으로 옮겨지고 그대로 되돌아간다`() =
        runTest {
            val api = FakeNotificationApi(settings = settingsDto(quietHours = null))
            val repository = NotificationRepositoryImpl(api)

            val settings = repository.getSettings().getOrThrow()
            repository.updateSettings(settings).getOrThrow()

            assertNull(settings.quietHours)
            assertNull(api.savedSettings.single().quietHours)
        }

    @Test
    fun `저장은 설정 전체를 명세 형식으로 실어 보낸다`() =
        runTest {
            val api = FakeNotificationApi()
            val repository = NotificationRepositoryImpl(api)

            val stored = repository.getSettings().getOrThrow()
            val updated = repository.updateSettings(stored.withTypeEnabled(NotificationType.WeeklyReport, enabled = true)).getOrThrow()

            assertEquals(
                settingsDto(weeklyReport = true),
                api.savedSettings.single(),
            )
            assertTrue(updated.weeklyReport)
        }

    @Test
    fun `서버 실패는 도메인 사유로 번역된다`() =
        runTest {
            val api = FakeNotificationApi()
            api.settingsThrows = ApiException(code = "AUTH_REQUIRED", serverMessage = null, fallbackMessage = "f", status = 401)

            val failure = NotificationRepositoryImpl(api).getSettings().exceptionOrNull()

            assertTrue(failure is CoreDataFailure.Unauthorized)
        }

    @Test
    fun `전송 실패는 네트워크 사유로 번역된다`() =
        runTest {
            val api = FakeNotificationApi()
            api.listThrows = UnknownHostException("offline")

            val failure = NotificationRepositoryImpl(api).getNotifications(cursor = null, limit = 20).exceptionOrNull()

            assertTrue(failure is CoreDataFailure.NetworkUnavailable)
            assertTrue((failure as CoreDataFailure.NetworkUnavailable).transportCause is IOException)
        }

    private companion object {
        fun dto(
            id: Long = 9,
            type: String = "dueSoon",
            targetId: Long? = 101,
            receivedAt: String = "2026-05-18T16:00:00+09:00",
        ): NotificationDto =
            NotificationDto(
                id = id,
                type = type,
                title = "마감 D-1",
                body = "마감이 하루 남았어요",
                receivedAt = receivedAt,
                isRead = false,
                targetId = targetId,
            )

        fun settingsDto(
            weeklyReport: Boolean = false,
            quietHours: QuietHoursDto? = QuietHoursDto(start = "23:00", end = "08:00"),
        ): NotificationSettingsDto =
            NotificationSettingsDto(
                newPosting = true,
                dueSoon = true,
                boardError = true,
                weeklyReport = weeklyReport,
                quietHours = quietHours,
                weekendOff = false,
                perBoard = listOf(BoardNotificationDto(boardId = 3, enabled = true)),
            )
    }
}
