package com.cambridge.feature.feed.presentation.reporting

import com.cambridge.core.common.reporting.ErrorReporter

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

/** [ErrorReporter.recordFailure] 에 피드 단계 속성을 붙여 기록한다. 취소 필터링은 인터페이스가 한다. */
public fun ErrorReporter.recordFeedFailure(
    stage: FeedFailureStage,
    throwable: Throwable,
) {
    recordFailure(throwable, mapOf(FEED_REPORT_KEY_STAGE to stage.key))
}
