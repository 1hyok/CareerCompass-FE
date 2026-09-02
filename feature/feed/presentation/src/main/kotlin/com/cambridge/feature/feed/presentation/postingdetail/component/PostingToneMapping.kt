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

        FeedListingCategory.ExternalActivity,
        FeedListingCategory.Other,
        -> CareerCompassBadgeTone.Neutral
    }

/** Badge tone for the suitability level label. */
internal fun CareerCompassScoreLevel.badgeTone(): CareerCompassBadgeTone =
    when (this) {
        CareerCompassScoreLevel.High -> CareerCompassBadgeTone.Brand
        CareerCompassScoreLevel.Mid -> CareerCompassBadgeTone.Info
        CareerCompassScoreLevel.Low -> CareerCompassBadgeTone.Neutral
    }
