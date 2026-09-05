package com.careercompass.feature.notification.domain.model

/**
 * 알림 종류 — API_SPEC v0.1 §8 설정 스키마의 키, 기능 스펙 F2-4.
 *
 * **넷이다.** 명세서 F2-4 표는 셋(신규 공고·마감 임박·수집 오류)만 적었지만 설정 스키마에
 * `weeklyReport` 키가 있다 — 키가 있으면 서버가 그 알림을 보낸다는 뜻이고, 명세서 표는 「발송 조건」만
 * 적어 조건이 정해지지 않은 주간 리포트가 빠진 것으로 본다. 판정과 근거는 `docs/spec/canon.md` 의
 * 「알림 유형 개수」 줄이다(FE #202).
 *
 * [WeeklyReport] 의 발송 조건은 아직 정해져 있지 않다. 설정 화면에는 넣되, 서버가 실제로 보내기 전까지
 * 목록에는 나타나지 않는다.
 *
 * @property wireValue 서버가 쓰는 값 — 설정 스키마의 키이자 목록 항목의 `type` 이다.
 */
public enum class NotificationType(
    public val wireValue: String,
) {
    /** 새 공고 — 등록한 게시판에 공고가 올라왔다. */
    NewPosting("newPosting"),

    /** 마감 임박 — 서버가 D-3·D-1 두 시점에 보낸다(BE #47). 앱은 두 시점을 가르지 않는다. */
    DueSoon("dueSoon"),

    /** 수집 오류 — 게시판 수집이 연속 실패했다. */
    BoardError("boardError"),

    /** 주간 리포트 — 한 주의 새 공고·마감 임박 수. 발송 조건 미정. */
    WeeklyReport("weeklyReport"),
    ;

    /**
     * 이 종류의 알림이 [targetId] 로 가리키는 화면.
     *
     * 종류가 대상의 성격을 정한다 — 신규 공고·수집 오류는 게시판 id, 마감 임박은 공고 id, 주간 리포트는
     * 대상이 피드 전체라 id 를 쓰지 않는다. 그래서 id 하나면 충분하고, 종류와 대상이 어긋난 조합
     * (「마감 임박인데 게시판으로 간다」)은 만들 수 없다.
     *
     * id 가 필요한 종류인데 [targetId] 가 없으면 `null` 이다 — 갈 곳을 모르는 줄은 화면에 세울 수 없다.
     */
    public fun targetOf(targetId: Long?): NotificationTarget? =
        when (this) {
            NewPosting -> targetId?.let(NotificationTarget::BoardFeed)
            DueSoon -> targetId?.let(NotificationTarget::PostingDetail)
            BoardError -> targetId?.let(NotificationTarget::BoardManagement)
            WeeklyReport -> NotificationTarget.Feed
        }

    public companion object {
        /** 서버가 보낸 `type` 을 종류로 옮긴다. 모르는 값이면 `null` — 앱이 그릴 수 없는 줄이다. */
        public fun fromWireValue(value: String): NotificationType? = entries.firstOrNull { it.wireValue == value }
    }
}
