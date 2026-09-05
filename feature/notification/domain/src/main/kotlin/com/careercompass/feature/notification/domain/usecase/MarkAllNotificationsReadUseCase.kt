package com.careercompass.feature.notification.domain.usecase

import com.careercompass.feature.notification.domain.repository.NotificationRepository
import javax.inject.Inject

/**
 * 모두 읽음 — `POST /notifications/read-all`.
 *
 * 되돌릴 수 없으므로 화면은 읽지 않은 것이 하나라도 있을 때만 버튼을 열고, 누른 뒤 스낵바로 알린다
 * (`docs/spec/notification-screens.md` §1).
 */
public class MarkAllNotificationsReadUseCase
    @Inject
    constructor(
        private val notificationRepository: NotificationRepository,
    ) {
        public suspend operator fun invoke(): Result<Unit> = notificationRepository.markAllRead()
    }
