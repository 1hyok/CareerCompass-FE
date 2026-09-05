package com.careercompass.feature.notification.domain.model

import java.time.Instant

/** 알림 목록의 기본 페이지 크기 — 피드와 같은 값을 쓴다(API_SPEC 「페이징」의 `limit`). */
public const val DEFAULT_NOTIFICATION_PAGE_SIZE: Int = 20

/**
 * 받은 알림 한 줄 — `docs/spec/notification-screens.md` §1 의 「읽음 점 · 아이콘 · 제목 · 본문 · 받은 시각」.
 *
 * 아이콘은 [type] 이 정하고, 상대 시각(`3분 전`)은 [receivedAt] 을 화면이 접은 결과다 — 둘 다 여기 두지
 * 않는다. 도메인이 들고 있는 것은 서버가 준 사실과 갈 곳([target])뿐이다.
 *
 * [title] 과 [body] 는 서버가 완성해 보낸 문구를 그대로 쓴다. 앱이 종류와 대상 이름으로 조립하지 않는
 * 이유는 다국어가 없어 서버가 문구를 갖는 쪽이 단순하고, 그래야 푸시로 뜬 문구와 목록의 문구가 갈리지
 * 않기 때문이다(BE #45 에 같은 내용을 적어 두었다).
 */
public data class NotificationItem(
    val id: Long,
    val type: NotificationType,
    val title: String,
    val body: String,
    val receivedAt: Instant,
    val isRead: Boolean,
    val target: NotificationTarget,
)
