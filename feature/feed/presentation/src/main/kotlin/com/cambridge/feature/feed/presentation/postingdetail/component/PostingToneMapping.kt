package com.cambridge.feature.feed.presentation.postingdetail.component

import com.cambridge.core.ui.component.CareerCompassBadgeTone
import com.cambridge.core.ui.component.CareerCompassScoreLevel
import com.cambridge.feature.feed.presentation.FeedListingCategory

/** Badge tone per listing category, matching the feed listing card. */
internal fun FeedListingCategory.badgeTone(): CareerCompassBadgeTone =
    when (this) {
        FeedListingCategory.All,
        FeedListingCategory.Employment,
        -> CareerCompassBadgeTone.Brand

        FeedListingCategory.Scholarship -> CareerCompassBadgeTone.Info

        FeedListingCategory.Contest -> CareerCompassBadgeTone.Warning

        FeedListingCategory.ExternalActivity -> CareerCompassBadgeTone.Neutral
    }

/** Badge tone for the suitability level label. */
internal fun CareerCompassScoreLevel.badgeTone(): CareerCompassBadgeTone =
    when (this) {
        CareerCompassScoreLevel.High -> CareerCompassBadgeTone.Brand
        CareerCompassScoreLevel.Mid -> CareerCompassBadgeTone.Info
        CareerCompassScoreLevel.Low -> CareerCompassBadgeTone.Neutral
    }

/** Score level thresholds shared with the feed listing card (spec F3-2). */
internal fun Int.suitabilityLevel(): CareerCompassScoreLevel =
    when {
        this >= HIGH_SCORE_THRESHOLD -> CareerCompassScoreLevel.High
        this >= MID_SCORE_THRESHOLD -> CareerCompassScoreLevel.Mid
        else -> CareerCompassScoreLevel.Low
    }

internal const val HIGH_SCORE_THRESHOLD: Int = 80
internal const val MID_SCORE_THRESHOLD: Int = 60
