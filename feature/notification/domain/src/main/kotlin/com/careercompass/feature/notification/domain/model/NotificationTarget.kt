package com.careercompass.feature.notification.domain.model

/**
 * 알림 한 줄을 탭했을 때 열리는 화면.
 *
 * 서버가 URL 문자열을 만들어 보내는 대신 종류([NotificationType])와 대상 id 로 받아 앱이 여기로 옮긴다.
 * 문자열이면 앱이 서버가 만든 URL 을 다시 파싱해야 하고, 앱이 아는 화면보다 서버가 앞서 나가는 순간
 * 열 수 없는 링크가 목록에 남는다. 값으로 들고 있으면 그 어긋남이 **타입에서** 드러난다.
 *
 * 이 값에서 실제 화면·딥링크로 옮기는 것은 presentation 몫이다(FE #195). 도메인은 「어디로 가는가」까지만
 * 정한다 — `careercompass://postings/{id}` 딥링크는 이미 서 있고(`app` 의 `AppDeepLink`), 게시판으로 좁힌
 * 피드와 게시판 관리에는 아직 딥링크 URI 가 없어 앱 안 이동으로 처리한다.
 */
public sealed interface NotificationTarget {
    /** 그 게시판으로 좁힌 피드 — 신규 공고 알림. */
    public data class BoardFeed(
        val boardId: Long,
    ) : NotificationTarget

    /** 그 공고 상세 — 마감 임박 알림. */
    public data class PostingDetail(
        val postingId: Long,
    ) : NotificationTarget

    /** 게시판 관리의 그 게시판 카드 — 수집 오류 알림. */
    public data class BoardManagement(
        val boardId: Long,
    ) : NotificationTarget

    /** 피드 전체 — 주간 리포트 알림. 가리키는 대상이 하나가 아니라 id 를 갖지 않는다. */
    public data object Feed : NotificationTarget
}
