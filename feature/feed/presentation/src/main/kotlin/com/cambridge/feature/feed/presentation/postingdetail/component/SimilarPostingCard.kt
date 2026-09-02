package com.cambridge.feature.feed.presentation.postingdetail.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.sp
import com.cambridge.core.ui.component.CareerCompassBadge
import com.cambridge.core.ui.component.CareerCompassBadgeTone
import com.cambridge.core.ui.theme.CareerCompassTheme
import com.cambridge.feature.feed.presentation.FeedListingUiModel
import com.cambridge.feature.feed.presentation.shared.component.FeedCard
import com.cambridge.feature.feed.presentation.shared.component.FeedSuitabilityChip

/**
 * Compact, single-action listing card for the "similar postings" section.
 *
 * Unlike the feed's [com.cambridge.feature.feed.presentation.FeedListingCard] it exposes no bookmark
 * toggle, because the detail contract has no per-similar-posting bookmark intent.
 */
@Composable
internal fun SimilarPostingCard(
    listing: FeedListingUiModel,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = CareerCompassTheme.colors

    FeedCard(onClick = onClick, modifier = modifier) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(
                modifier = Modifier.weight(1f),
                horizontalArrangement = Arrangement.spacedBy(CareerCompassTheme.spacing.xSmall),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                CareerCompassBadge(
                    label = listing.categoryLabel,
                    tone = listing.category.badgeTone(),
                )
                CareerCompassBadge(
                    label = listing.sourceLabel,
                    tone = CareerCompassBadgeTone.Neutral,
                )
            }
            Spacer(modifier = Modifier.width(CareerCompassTheme.spacing.xSmall))
            FeedSuitabilityChip(state = listing.suitability)
        }
        Text(
            text = listing.title,
            color = colors.onSurface,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            style = CareerCompassTheme.typography.headline4,
        )
        Text(
            text = listing.deadlineLabel,
            color = if (listing.isDeadlineUrgent) colors.actionDanger else colors.mutedContent,
            style =
                CareerCompassTheme.typography.caption.copy(
                    fontSize = 12.sp,
                    lineHeight = 18.sp,
                    fontWeight = FontWeight.SemiBold,
                ),
        )
    }
}
