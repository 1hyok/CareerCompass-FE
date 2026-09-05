package com.careercompass.feature.notification.domain.usecase

import com.careercompass.feature.notification.domain.model.NotificationSettings
import com.careercompass.feature.notification.domain.repository.NotificationRepository
import javax.inject.Inject

/**
 * 게시판 스위치 하나를 바꿔 설정 전체를 저장한다.
 *
 * 설정에 아직 줄이 없는 게시판이면 줄을 만들어 실어 보낸다
 * ([NotificationSettings.withBoardEnabled]). 이미 그 값이면 서버를 부르지 않는다.
 */
public class SetBoardNotificationEnabledUseCase
    @Inject
    constructor(
        private val notificationRepository: NotificationRepository,
    ) {
        public suspend operator fun invoke(
            current: NotificationSettings,
            boardId: Long,
            enabled: Boolean,
        ): Result<NotificationSettings> {
            if (current.isBoardEnabled(boardId) == enabled) return Result.success(current)
            return notificationRepository.updateSettings(current.withBoardEnabled(boardId, enabled))
        }
    }
