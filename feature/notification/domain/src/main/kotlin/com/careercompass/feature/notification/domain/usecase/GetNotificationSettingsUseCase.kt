package com.careercompass.feature.notification.domain.usecase

import com.careercompass.feature.notification.domain.model.NotificationSettings
import com.careercompass.feature.notification.domain.repository.NotificationRepository
import javax.inject.Inject

/** 알림 설정 조회 — `GET /notifications/settings`. */
public class GetNotificationSettingsUseCase
    @Inject
    constructor(
        private val notificationRepository: NotificationRepository,
    ) {
        public suspend operator fun invoke(): Result<NotificationSettings> = notificationRepository.getSettings()
    }
