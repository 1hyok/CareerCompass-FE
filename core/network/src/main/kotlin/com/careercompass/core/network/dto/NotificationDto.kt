package com.careercompass.core.network.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * API_SPEC v0.1 §8 `GET /notifications` 항목.
 *
 * **명세에 응답 스키마가 없다.** §8 은 「알림 목록」 한 줄이 전부고, 설정 스키마만 예시가 붙어 있다.
 * 그래서 이 DTO 는 서버가 확정한 계약이 아니라 **FE 가 화면에서 실제로 쓰는 필드를 적어 낸 제안**이다
 * — 근거는 `docs/spec/notification-screens.md` §1 의 「한 줄은 읽음 점·아이콘·제목·본문·받은 시각
 * 다섯 조각이다」와 종류 넷의 딥링크 대상 표다. 같은 제안을 서버 저장소
 * [CareerCompass-BE #45](https://github.com/Team-CareerCompass/CareerCompass-BE/issues/45) 에 적어 두었고,
 * 서버가 다른 모양을 확정하면 여기와 매퍼만 고친다.
 *
 * 딥링크 대상은 URL 문자열이 아니라 [type] + [targetId] 로 받는다. 문자열이면 서버가 만든 URL 을 앱이
 * 다시 파싱해야 하고, 앱이 아는 화면보다 서버가 앞서 나가면 열 수 없는 링크가 목록에 남는다. 종류가
 * 대상의 성격을 정하므로(신규 공고·수집 오류는 게시판, 마감 임박은 공고) id 하나면 충분하다.
 *
 * @property type 종류 넷 중 하나 — `newPosting`·`dueSoon`·`boardError`·`weeklyReport`. 판정은
 *   `docs/spec/canon.md` 의 「알림 유형 개수」 줄이다(설정 스키마에 키가 있으면 서버가 보낸다).
 * @property title 서버가 완성해 보낸 제목. 앱이 종류와 대상 이름으로 조립하지 않는다 — 다국어가 없어
 *   서버가 문구를 갖는 쪽이 단순하고, 발송 푸시와 목록의 문구가 갈리지 않는다.
 * @property receivedAt ISO-8601 시각. 화면은 이 값을 상대 표기(`3분 전`)로 접는다.
 * @property targetId 딥링크 대상의 id — 종류가 게시판이면 boardId, 공고면 postingId. 주간 리포트는
 *   대상이 피드 전체라 `null` 이다. 키 자체는 언제나 실린다(기본값을 두지 않는다).
 */
@Serializable
data class NotificationDto(
    @SerialName("id")
    val id: Long,
    @SerialName("type")
    val type: String,
    @SerialName("title")
    val title: String,
    @SerialName("body")
    val body: String,
    @SerialName("receivedAt")
    val receivedAt: String,
    @SerialName("isRead")
    val isRead: Boolean,
    @SerialName("targetId")
    val targetId: Long?,
)

/** `GET /notifications` 응답 — 커서 페이징은 공통 규약대로 `nextCursor` 다. 목록 키는 `notifications` 로 가정한다. */
@Serializable
data class NotificationListDto(
    @SerialName("notifications")
    val notifications: List<NotificationDto>,
    @SerialName("nextCursor")
    val nextCursor: String? = null,
)

/** §8 설정의 「방해 금지」 시간대. `23:00 → 08:00` 처럼 자정을 넘는 구간이 정상이다. */
@Serializable
data class QuietHoursDto(
    @SerialName("start")
    val start: String,
    @SerialName("end")
    val end: String,
)

/** §8 설정의 게시판별 ON/OFF 한 줄. */
@Serializable
data class BoardNotificationDto(
    @SerialName("boardId")
    val boardId: Long,
    @SerialName("enabled")
    val enabled: Boolean,
)

/**
 * §8 `GET /notifications/settings` 응답이자 `PUT` 요청 본문 — 명세가 두 방향에 같은 스키마 하나를 준다.
 *
 * **어느 프로퍼티에도 기본값을 두지 않는다.** 응답 쪽에서는 키 누락이 조용히 성공하는 것을 막고
 * (`ResponseDtoContractKonsistTest`), 요청 쪽에서는 `encodeDefaults = false` 가 값을 빼먹는 것을 막는다 —
 * 특히 [quietHours] 는 `null` 이 「방해 금지 꺼짐」이라는 뜻이라 키가 사라지면 「바꾸지 않음」과 구별되지
 * 않는다. 기본값이 없으면 kotlinx 는 `null` 을 그대로 실어 보낸다.
 *
 * 이름에 `Request` 를 넣지 않은 것은 의도다 — 이 값은 응답이기도 하고, 요청 DTO 로 분류되면 위의
 * 기본값 가드가 적용되지 않는다.
 */
@Serializable
data class NotificationSettingsDto(
    @SerialName("newPosting")
    val newPosting: Boolean,
    @SerialName("dueSoon")
    val dueSoon: Boolean,
    @SerialName("boardError")
    val boardError: Boolean,
    @SerialName("weeklyReport")
    val weeklyReport: Boolean,
    @SerialName("quietHours")
    val quietHours: QuietHoursDto?,
    @SerialName("weekendOff")
    val weekendOff: Boolean,
    @SerialName("perBoard")
    val perBoard: List<BoardNotificationDto>,
)
