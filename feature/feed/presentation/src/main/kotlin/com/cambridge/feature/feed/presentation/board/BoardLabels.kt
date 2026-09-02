package com.cambridge.feature.feed.presentation.board

import androidx.annotation.StringRes
import com.cambridge.core.ui.component.CareerCompassBadgeTone
import com.cambridge.feature.feed.presentation.R

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
