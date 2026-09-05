package com.careercompass.feature.notification.data.mapper

import com.careercompass.core.data.mapper.WireTime
import com.careercompass.core.network.dto.BoardNotificationDto
import com.careercompass.core.network.dto.NotificationDto
import com.careercompass.core.network.dto.NotificationSettingsDto
import com.careercompass.core.network.dto.QuietHoursDto
import com.careercompass.feature.notification.domain.model.BoardNotificationSetting
import com.careercompass.feature.notification.domain.model.NotificationItem
import com.careercompass.feature.notification.domain.model.NotificationSettings
import com.careercompass.feature.notification.domain.model.NotificationType
import com.careercompass.feature.notification.domain.model.QuietHours
import java.time.LocalTime
import java.time.format.DateTimeParseException

internal object NotificationMapper {
    /**
     * 알림 한 줄 — **앱이 그릴 수 없는 줄이면 `null`** 이고, 호출자가 목록에서 뺀다.
     *
     * 그릴 수 없는 경우는 둘이다. 모르는 [NotificationDto.type] 이면 아이콘도 제목 규칙도 갈 곳도 정할 수
     * 없고, 대상 id 가 필요한 종류인데 `targetId` 가 비면 탭했을 때 아무 데도 못 간다.
     *
     * 페이지 전체를 실패시키지 않는 이유 — 서버가 우리보다 앞서 새 종류를 보내기 시작하면 그 순간
     * 알림함 전체가 오류 화면이 된다. 사용자가 받은 나머지 알림은 멀쩡하고, 못 그리는 한 줄 때문에 그것을
     * 못 보게 만드는 쪽이 손해가 크다. 대신 조용히 지나가지는 않게 시각·문구 형식이 어긋나면
     * [WireTime] 이 예외로 세운다 — 그건 **아는 종류의 계약이 깨진** 것이라 성격이 다르다.
     */
    fun toItem(dto: NotificationDto): NotificationItem? {
        val type = NotificationType.fromWireValue(dto.type) ?: return null
        val target = type.targetOf(dto.targetId) ?: return null
        return NotificationItem(
            id = dto.id,
            type = type,
            title = dto.title,
            body = dto.body,
            receivedAt = WireTime.parseInstant(dto.receivedAt),
            isRead = dto.isRead,
            target = target,
        )
    }

    fun toSettings(dto: NotificationSettingsDto): NotificationSettings =
        NotificationSettings(
            newPosting = dto.newPosting,
            dueSoon = dto.dueSoon,
            boardError = dto.boardError,
            weeklyReport = dto.weeklyReport,
            quietHours = dto.quietHours?.let(::toQuietHours),
            weekendOff = dto.weekendOff,
            perBoard = dto.perBoard.map { BoardNotificationSetting(boardId = it.boardId, enabled = it.enabled) },
        )

    fun toSettingsDto(settings: NotificationSettings): NotificationSettingsDto =
        NotificationSettingsDto(
            newPosting = settings.newPosting,
            dueSoon = settings.dueSoon,
            boardError = settings.boardError,
            weeklyReport = settings.weeklyReport,
            quietHours = settings.quietHours?.let { QuietHoursDto(start = format(it.start), end = format(it.end)) },
            weekendOff = settings.weekendOff,
            perBoard = settings.perBoard.map { BoardNotificationDto(boardId = it.boardId, enabled = it.enabled) },
        )

    /** `23:00` — 시·분만 있는 24시간 표기다. 초가 붙어 오면 계약이 깨진 것이라 세운다. */
    private fun toQuietHours(dto: QuietHoursDto): QuietHours = QuietHours(start = parseTime(dto.start), end = parseTime(dto.end))

    private fun parseTime(value: String): LocalTime =
        try {
            LocalTime.parse(value)
        } catch (exception: DateTimeParseException) {
            throw IllegalStateException("시각 형식이 계약과 다릅니다: $value", exception)
        }

    private fun format(value: LocalTime): String = "%02d:%02d".format(value.hour, value.minute)
}
