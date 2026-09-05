package com.careercompass.feature.notification.domain.usecase

import com.careercompass.core.model.paging.CursorPage
import com.careercompass.feature.notification.domain.model.DEFAULT_NOTIFICATION_PAGE_SIZE
import com.careercompass.feature.notification.domain.model.NotificationItem
import com.careercompass.feature.notification.domain.repository.NotificationRepository
import javax.inject.Inject

/** 알림 목록 한 페이지 — 커서를 넘기지 않으면 첫 페이지다. */
public class GetNotificationsUseCase
    @Inject
    constructor(
        private val notificationRepository: NotificationRepository,
    ) {
        public suspend operator fun invoke(
            cursor: String? = null,
            limit: Int = DEFAULT_NOTIFICATION_PAGE_SIZE,
        ): Result<CursorPage<NotificationItem>> {
            require(limit > 0) { "limit must be greater than zero" }
            return notificationRepository.getNotifications(cursor = cursor, limit = limit)
        }
    }
