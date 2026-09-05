package com.careercompass.feature.notification.domain.usecase

import com.careercompass.feature.notification.domain.model.NotificationSettings
import com.careercompass.feature.notification.domain.model.NotificationType
import com.careercompass.feature.notification.domain.repository.NotificationRepository
import javax.inject.Inject

/**
 * 종류 스위치 하나를 바꿔 설정 전체를 저장한다.
 *
 * §8 에 부분 수정이 없어 스위치 하나에도 설정 전부가 실린다. 그 조립을 화면이 하면 종류가 늘 때마다
 * 화면의 `when` 이 늘어나므로 도메인에 둔다.
 *
 * 이미 그 값이면 서버를 부르지 않는다 — 목록을 스크롤하다 같은 스위치를 두 번 스치는 것으로 같은 PUT 이
 * 두 번 나가지 않게 한다.
 */
public class SetNotificationTypeEnabledUseCase
    @Inject
    constructor(
        private val notificationRepository: NotificationRepository,
    ) {
        public suspend operator fun invoke(
            current: NotificationSettings,
            type: NotificationType,
            enabled: Boolean,
        ): Result<NotificationSettings> {
            if (current.isEnabled(type) == enabled) return Result.success(current)
            return notificationRepository.updateSettings(current.withTypeEnabled(type, enabled))
        }
    }
