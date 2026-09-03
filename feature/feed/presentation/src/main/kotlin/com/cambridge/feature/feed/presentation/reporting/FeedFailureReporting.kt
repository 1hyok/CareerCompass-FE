package com.cambridge.feature.feed.presentation.reporting

import com.cambridge.core.common.reporting.ErrorReporter
import com.cambridge.core.domain.error.CoreDataFailure

/** 리포팅 속성 키 — 피드 기능 안에서 어느 단계가 실패했는지. */
public const val FEED_REPORT_KEY_STAGE: String = "feed_stage"

/** 피드 기능의 실패 단계. 값은 리포팅 콘솔 필터용 안정 식별자다. */
public enum class FeedFailureStage(
    public val key: String,
) {
    FeedLoad("feed_load"),
    FeedRefresh("feed_refresh"),
    FeedLoadMore("feed_load_more"),
    FeedSnapshotSave("feed_snapshot_save"),
    FeedSnapshotLoad("feed_snapshot_load"),
    TodayCount("today_count"),
    FilterBoards("filter_boards"),
    Bookmark("bookmark"),
    PostingDetail("posting_detail"),
    PostingRaw("posting_raw"),
    BoardDetect("board_detect"),
    BoardRegister("board_register"),
    BoardList("board_list"),
    BoardToggle("board_toggle"),
    BoardRetry("board_retry"),
    BoardDelete("board_delete"),
    BoardUpdate("board_update"),
}

/**
 * [ErrorReporter.recordFailure] 에 피드 단계 속성을 붙여 기록한다. 취소 필터링은 인터페이스가 한다.
 *
 * 예상된 상태([isExpectedEnvironmentState])는 기록하지 않는다 — 코루틴 취소를 인터페이스가 거르는
 * 것과 같은 이유다. 지하철에서 앱을 켠 횟수와 서버 점검 창 하나가 콘솔을 채우면, 정작 고쳐야 할
 * 결함이 그 잡음에 묻힌다.
 */
public fun ErrorReporter.recordFeedFailure(
    stage: FeedFailureStage,
    throwable: Throwable,
) {
    if (throwable.isExpectedEnvironmentState()) return
    recordFailure(throwable, mapOf(FEED_REPORT_KEY_STAGE to stage.key))
}

/**
 * 화면이 사유를 그대로 안내하는 환경 상태 — 우리가 고칠 결함이 아니다.
 *
 * 네트워크 단절은 사용자의 연결 문제이고, 서버 점검(503 `LLM_UNAVAILABLE`)은 서버가 스스로 알린
 * 계획된 상태다. 둘 다 앱은 이미 전용 화면으로 안내하고 있으므로 리포팅까지 할 것이 없다.
 */
private fun Throwable.isExpectedEnvironmentState(): Boolean =
    this is CoreDataFailure.NetworkUnavailable || this is CoreDataFailure.ServiceUnavailable
