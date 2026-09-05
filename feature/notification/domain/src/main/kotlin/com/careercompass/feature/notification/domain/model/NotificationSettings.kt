package com.careercompass.feature.notification.domain.model

import java.time.LocalTime

/**
 * 방해 금지 시간대 — `docs/spec/notification-screens.md` §2 「시간」.
 *
 * **자정을 넘는 구간이 기본 사용례다**(`23:00 → 08:00`). 그래서 [end] 가 [start] 보다 앞서는 것을 막지
 * 않는다. 대신 두 값이 같은 것은 막는다 — 「하루 종일 받지 않음」이라는 뜻이 되는데, 그건 종류 스위치를
 * 모두 끄는 것과 같은 결과이면서 화면에는 「시간대를 골랐다」로 보여 서로 다른 두 사실이 한 값에 겹친다.
 *
 * 저장하려는 값이 이 조건을 어기면 서버로 보내기 전에 걸린다 — 값 자체가 만들어지지 않기 때문이다.
 */
public data class QuietHours(
    val start: LocalTime,
    val end: LocalTime,
) {
    init {
        require(start != end) { "quiet hours must not start and end at the same time" }
    }
}

/** 게시판별 알림 ON/OFF 한 줄 — §8 설정의 `perBoard`. */
public data class BoardNotificationSetting(
    val boardId: Long,
    val enabled: Boolean,
)

/**
 * 알림 설정 — API_SPEC v0.1 §8 `GET /notifications/settings` / `PUT` 의 스키마 그대로다.
 *
 * 서버가 스키마 하나로 조회와 저장을 함께 받으므로 이 값이 **설정의 전부**다. 스위치 하나를 바꿔도
 * 전체를 보낸다(§8 에 부분 수정이 없다). 화면은 낙관적으로 켜고 실패하면 되돌린다.
 *
 * @property quietHours `null` 이면 방해 금지가 꺼진 것이다. 「바꾸지 않음」이라는 뜻이 아니다 — 이 값은
 *   언제나 설정 전체를 나타낸다.
 * @property perBoard 게시판별 ON/OFF. 삭제된 게시판이 남아 있을 수 있어 게시판 목록과 개수가 다를 수 있다
 *   (화면이 「사라진 게시판」으로 보이고 지울 수 있게 한다).
 */
public data class NotificationSettings(
    val newPosting: Boolean,
    val dueSoon: Boolean,
    val boardError: Boolean,
    val weeklyReport: Boolean,
    val quietHours: QuietHours?,
    val weekendOff: Boolean,
    val perBoard: List<BoardNotificationSetting>,
) {
    init {
        require(perBoard.distinctBy(BoardNotificationSetting::boardId).size == perBoard.size) {
            "perBoard must not repeat a boardId"
        }
    }

    /**
     * 종류 스위치 넷의 현재 값.
     *
     * 화면은 종류를 목록으로 그리는데 설정은 종류마다 다른 프로퍼티다. 그 대응을 화면의 `when` 에 두면
     * 종류가 늘 때 화면마다 고쳐야 하므로 여기 한 자리에 모은다.
     */
    public fun isEnabled(type: NotificationType): Boolean =
        when (type) {
            NotificationType.NewPosting -> newPosting
            NotificationType.DueSoon -> dueSoon
            NotificationType.BoardError -> boardError
            NotificationType.WeeklyReport -> weeklyReport
        }

    /** 종류 스위치 하나만 바꾼 새 설정. */
    public fun withTypeEnabled(
        type: NotificationType,
        enabled: Boolean,
    ): NotificationSettings =
        when (type) {
            NotificationType.NewPosting -> copy(newPosting = enabled)
            NotificationType.DueSoon -> copy(dueSoon = enabled)
            NotificationType.BoardError -> copy(boardError = enabled)
            NotificationType.WeeklyReport -> copy(weeklyReport = enabled)
        }

    /** [boardId] 의 현재 값. 설정에 없는 게시판은 켜져 있는 것으로 본다 — 서버가 아직 줄을 만들지 않은 상태다. */
    public fun isBoardEnabled(boardId: Long): Boolean = perBoard.firstOrNull { it.boardId == boardId }?.enabled != false

    /**
     * 게시판 스위치 하나만 바꾼 새 설정.
     *
     * 설정에 없던 게시판이면 줄을 더한다 — 서버가 새 게시판의 줄을 아직 만들지 않았어도 사용자가 끌 수
     * 있어야 하고, 끈 사실을 실어 보내려면 줄이 있어야 한다.
     */
    public fun withBoardEnabled(
        boardId: Long,
        enabled: Boolean,
    ): NotificationSettings {
        val updated =
            if (perBoard.any { it.boardId == boardId }) {
                perBoard.map { if (it.boardId == boardId) it.copy(enabled = enabled) else it }
            } else {
                perBoard + BoardNotificationSetting(boardId = boardId, enabled = enabled)
            }
        return copy(perBoard = updated)
    }
}
