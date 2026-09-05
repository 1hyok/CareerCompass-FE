package com.careercompass.feature.notification.domain.testing

import com.careercompass.core.model.paging.CursorPage
import com.careercompass.feature.notification.domain.model.NotificationItem
import com.careercompass.feature.notification.domain.model.NotificationSettings
import com.careercompass.feature.notification.domain.repository.NotificationRepository
import java.util.concurrent.CopyOnWriteArrayList

/**
 * [NotificationRepository] fake 정본 — 메모리에 목록 한 페이지와 설정 하나를 든다.
 *
 * `onX` 훅이 있으면 기본 동작 대신 그 결과를 돌려준다(실패 시나리오). 호출 기록은 [requestedPages]·
 * [markAllReadCount]·[savedSettings] 로 검증한다.
 */
public class FakeNotificationRepository(
    public var page: CursorPage<NotificationItem> = CursorPage.empty(),
    public var settings: NotificationSettings =
        NotificationSettings(
            newPosting = true,
            dueSoon = true,
            boardError = true,
            weeklyReport = false,
            quietHours = null,
            weekendOff = false,
            perBoard = emptyList(),
        ),
    public var onGetNotifications: (suspend (String?, Int) -> Result<CursorPage<NotificationItem>>)? = null,
    public var onMarkAllRead: (suspend () -> Result<Unit>)? = null,
    public var onGetSettings: (suspend () -> Result<NotificationSettings>)? = null,
    public var onUpdateSettings: (suspend (NotificationSettings) -> Result<NotificationSettings>)? = null,
) : NotificationRepository {
    /** [getNotifications] 로 들어온 `cursor`·`limit` 순서대로. 훅이 가로챈 호출도 기록한다. */
    public val requestedPages: CopyOnWriteArrayList<Pair<String?, Int>> = CopyOnWriteArrayList()

    /** [updateSettings] 로 들어온 설정 순서대로. */
    public val savedSettings: CopyOnWriteArrayList<NotificationSettings> = CopyOnWriteArrayList()
    public var markAllReadCount: Int = 0
        private set

    override suspend fun getNotifications(
        cursor: String?,
        limit: Int,
    ): Result<CursorPage<NotificationItem>> {
        requestedPages += cursor to limit
        onGetNotifications?.let { return it(cursor, limit) }
        return Result.success(page)
    }

    override suspend fun markAllRead(): Result<Unit> {
        markAllReadCount += 1
        onMarkAllRead?.let { return it() }
        page = page.copy(items = page.items.map { it.copy(isRead = true) })
        return Result.success(Unit)
    }

    override suspend fun getSettings(): Result<NotificationSettings> {
        onGetSettings?.let { return it() }
        return Result.success(settings)
    }

    override suspend fun updateSettings(settings: NotificationSettings): Result<NotificationSettings> {
        savedSettings += settings
        onUpdateSettings?.let { return it(settings) }
        this.settings = settings
        return Result.success(settings)
    }
}
