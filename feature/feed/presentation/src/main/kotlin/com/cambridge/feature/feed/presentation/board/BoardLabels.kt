package com.cambridge.feature.feed.presentation.board

import androidx.annotation.StringRes
import com.cambridge.feature.feed.presentation.R
import com.careercompass.core.ui.component.CareerCompassBadgeTone

@StringRes
internal fun BoardType.labelRes(): Int =
    when (this) {
        BoardType.Scholarship -> R.string.feed_board_type_scholarship
        BoardType.Employment -> R.string.feed_board_type_employment
        BoardType.Contest -> R.string.feed_board_type_contest
        BoardType.ExternalActivity -> R.string.feed_board_type_external_activity
        BoardType.Other -> R.string.feed_board_type_other
    }

/** Badge tone per board type, aligned with the feed listing category tones. */
internal fun BoardType.badgeTone(): CareerCompassBadgeTone =
    when (this) {
        BoardType.Employment -> CareerCompassBadgeTone.Brand

        BoardType.Scholarship -> CareerCompassBadgeTone.Info

        BoardType.Contest -> CareerCompassBadgeTone.Warning

        BoardType.ExternalActivity,
        BoardType.Other,
        -> CareerCompassBadgeTone.Neutral
    }

@StringRes
internal fun BoardCollectCycle.labelRes(): Int =
    when (this) {
        BoardCollectCycle.Daily -> R.string.feed_board_cycle_daily
        BoardCollectCycle.TwiceDaily -> R.string.feed_board_cycle_twice_daily
        BoardCollectCycle.Weekly -> R.string.feed_board_cycle_weekly
    }

@StringRes
internal fun BoardDetectionFailure.messageRes(): Int =
    when (this) {
        BoardDetectionFailure.LoginRequired -> R.string.feed_board_failure_login_required
        BoardDetectionFailure.Spa -> R.string.feed_board_failure_spa
        BoardDetectionFailure.Blocked -> R.string.feed_board_failure_blocked
        BoardDetectionFailure.Failed -> R.string.feed_board_failure_failed
    }

/**
 * **같은 주소로 다시 감지하면 답이 갈릴 여지가 있는가** — 실패 상자에 재시도를 그릴지의 근거다(#204).
 *
 * 로그인이 필요한 게시판·JavaScript 로 그려지는 게시판·수집이 막힌 사이트는 사이트 쪽 사정이라 몇 번을
 * 다시 보내도 같은 답이 온다. 버튼을 주면 사용자는 누르고 같은 상자를 다시 만난다 — 실패 표가
 * `BOARD_BLOCKED` 에 재시도를 주지 않는 것과 같은 규칙이다
 * ([FailureDisplay][com.careercompass.core.ui.failure.FailureDisplay]).
 *
 * [BoardDetectionFailure.Failed] 만 예외다. 「목록 페이지 주소인지 확인해 주세요」는 사용자가 URL 을
 * 고쳐 다시 누르는 길이고, 그 자리에서 실제로 답이 갈린다.
 *
 * 재시도를 지워도 막다른 길이 되지 않는다 — URL 입력란과 「구조 분석하기」는 실패 상자 **위에** 그대로
 * 남아 있어, 주소를 고쳐 다시 감지하는 길이 화면에 계속 있다.
 */
internal val BoardDetectionFailure.isRetryable: Boolean
    get() =
        when (this) {
            BoardDetectionFailure.LoginRequired,
            BoardDetectionFailure.Spa,
            BoardDetectionFailure.Blocked,
            -> false

            BoardDetectionFailure.Failed -> true
        }
