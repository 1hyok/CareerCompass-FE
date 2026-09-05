package com.careercompass.feature.notification.domain.repository

import com.careercompass.core.model.paging.CursorPage
import com.careercompass.feature.notification.domain.model.NotificationItem
import com.careercompass.feature.notification.domain.model.NotificationSettings

/**
 * 알림 계약 — API_SPEC v0.1 §8 `/notifications`.
 *
 * 명세가 준 엔드포인트 넷이 전부다. 화면 명세가 요구하는 **개별 읽음**은 계약에 없어 여기에도 없다 —
 * 서버 저장소 [CareerCompass-BE #45](https://github.com/Team-CareerCompass/CareerCompass-BE/issues/45)
 * 에 필요하다고 적어 두었고, 확정되면 이 계약에 더한다.
 *
 * FCM 토큰의 등록·해제는 이 계약이 아니라 인증 계약(`AuthRepository`)에 실려 있다 — 등록은 소셜 로그인
 * 요청의 `fcmToken`, 해제는 `POST /auth/logout` 이다. 갱신은 계약 자체가 없다.
 * `docs/spec/fcm-token-lifecycle.md` 를 보라.
 *
 * 실패는 [Result] 로 돌려주고, 사유가 확인된 것은
 * [CoreDataFailure][com.careercompass.core.domain.error.CoreDataFailure] 로 번역돼 있다.
 */
public interface NotificationRepository {
    /**
     * `GET /notifications` — 커서 페이징. [cursor] 가 `null` 이면 첫 페이지다.
     *
     * 앱이 해석할 수 없는 항목(모르는 종류, 갈 곳을 알 수 없는 대상)은 페이지에서 빠진다. 그래서 항목이
     * 비어도 마지막 페이지라는 뜻은 아니다 — 끝은 `nextCursor` 로 판단한다.
     */
    public suspend fun getNotifications(
        cursor: String?,
        limit: Int,
    ): Result<CursorPage<NotificationItem>>

    /** `POST /notifications/read-all` — 되돌릴 수 없다. */
    public suspend fun markAllRead(): Result<Unit>

    public suspend fun getSettings(): Result<NotificationSettings>

    /** `PUT /notifications/settings` — 설정 전체를 보내고 서버가 확정한 값을 돌려받는다. */
    public suspend fun updateSettings(settings: NotificationSettings): Result<NotificationSettings>
}
