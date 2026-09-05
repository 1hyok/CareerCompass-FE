package com.careercompass.feature.notification.domain.usecase

import com.careercompass.feature.notification.domain.model.NotificationSettings
import com.careercompass.feature.notification.domain.repository.NotificationRepository
import javax.inject.Inject

/**
 * 알림 설정 저장 — `PUT /notifications/settings`.
 *
 * 방해 금지 시간대·주말 끄기처럼 스위치 하나로 접히지 않는 변경이 여기로 온다. 종류·게시판 스위치는
 * 눌린 값만 넘기는 전용 use case 가 따로 있다
 * ([SetNotificationTypeEnabledUseCase]·[SetBoardNotificationEnabledUseCase]).
 *
 * 저장할 수 없는 값(시작·종료가 같은 방해 금지 시간대)은
 * [QuietHours][com.careercompass.feature.notification.domain.model.QuietHours] 가 애초에 만들지 못하게
 * 막으므로 여기서 다시 검사하지 않는다.
 */
public class UpdateNotificationSettingsUseCase
    @Inject
    constructor(
        private val notificationRepository: NotificationRepository,
    ) {
        public suspend operator fun invoke(settings: NotificationSettings): Result<NotificationSettings> =
            notificationRepository.updateSettings(settings)
    }
