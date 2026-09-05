package com.careercompass.feature.notification.data

import com.careercompass.core.common.result.runCatchingCancellable
import com.careercompass.core.data.failure.mapDataFailure
import com.careercompass.core.model.paging.CursorPage
import com.careercompass.core.network.model.requireData
import com.careercompass.core.network.model.requireOk
import com.careercompass.core.network.service.NotificationApiService
import com.careercompass.feature.notification.data.mapper.NotificationMapper
import com.careercompass.feature.notification.domain.model.NotificationItem
import com.careercompass.feature.notification.domain.model.NotificationSettings
import com.careercompass.feature.notification.domain.repository.NotificationRepository
import javax.inject.Inject

internal class NotificationRepositoryImpl
    @Inject
    constructor(
        private val notificationApiService: NotificationApiService,
    ) : NotificationRepository {
        override suspend fun getNotifications(
            cursor: String?,
            limit: Int,
        ): Result<CursorPage<NotificationItem>> {
            require(limit > 0) { "limit must be greater than zero" }
            return runCatchingCancellable {
                val page = notificationApiService.getNotifications(cursor = cursor, limit = limit).requireData()
                CursorPage(
                    items = page.notifications.mapNotNull(NotificationMapper::toItem),
                    nextCursor = page.nextCursor,
                )
            }.mapDataFailure()
        }

        override suspend fun markAllRead(): Result<Unit> =
            runCatchingCancellable { notificationApiService.markAllRead().requireOk() }.mapDataFailure()

        override suspend fun getSettings(): Result<NotificationSettings> =
            runCatchingCancellable {
                NotificationMapper.toSettings(notificationApiService.getSettings().requireData())
            }.mapDataFailure()

        override suspend fun updateSettings(settings: NotificationSettings): Result<NotificationSettings> =
            runCatchingCancellable {
                NotificationMapper.toSettings(
                    notificationApiService.updateSettings(NotificationMapper.toSettingsDto(settings)).requireData(),
                )
            }.mapDataFailure()
    }
