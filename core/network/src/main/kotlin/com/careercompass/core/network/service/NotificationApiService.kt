package com.careercompass.core.network.service

import com.careercompass.core.network.dto.NotificationListDto
import com.careercompass.core.network.dto.NotificationSettingsDto
import com.careercompass.core.network.model.BaseResponse
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Query

/**
 * API_SPEC v0.1 §8 — `/notifications`.
 *
 * 명세가 준 엔드포인트 넷이 전부다. 화면 명세(`docs/spec/notification-screens.md` §1)는 줄을 탭했을 때의
 * **개별 읽음**도 요구하지만 `POST /notifications/{id}/read` 는 계약에 없다 — 없는 라우트를 앱이 먼저
 * 선언하면 서버가 다른 이름을 고르는 순간 죽은 선언이 남는다. 서버 저장소
 * [CareerCompass-BE #45](https://github.com/Team-CareerCompass/CareerCompass-BE/issues/45) 에 필요하다고
 * 적어 두었고, 확정되면 여기에 더한다.
 *
 * FCM 토큰 등록·갱신은 이 서비스가 아니다. 등록은 `POST /auth/social/{provider}` 의 `fcmToken`(§1),
 * 해제는 `POST /auth/logout` 이 함께 처리한다. 갱신 엔드포인트는 계약에 없다 —
 * `docs/spec/fcm-token-lifecycle.md` 와 [CareerCompass-BE #44](https://github.com/Team-CareerCompass/CareerCompass-BE/issues/44).
 */
public interface NotificationApiService {
    /** `GET /notifications` — 커서 페이징. null 인 커서는 Retrofit 이 생략해 첫 페이지가 된다. */
    @GET("notifications")
    public suspend fun getNotifications(
        @Query("cursor") cursor: String?,
        @Query("limit") limit: Int,
    ): BaseResponse<NotificationListDto>

    /** `POST /notifications/read-all` — 모두 읽음. 되돌릴 수 없다. */
    @POST("notifications/read-all")
    public suspend fun markAllRead(): BaseResponse<Unit>

    @GET("notifications/settings")
    public suspend fun getSettings(): BaseResponse<NotificationSettingsDto>

    /** `PUT /notifications/settings` — 부분 수정이 아니라 설정 전체를 실어 보낸다(§8 이 스키마 하나만 준다). */
    @PUT("notifications/settings")
    public suspend fun updateSettings(
        @Body body: NotificationSettingsDto,
    ): BaseResponse<NotificationSettingsDto>
}
