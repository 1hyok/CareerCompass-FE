package com.cambridge.feature.feed.presentation.postingdetail.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.cambridge.feature.feed.presentation.FeedListingUiModel
import com.cambridge.feature.feed.presentation.R
import com.cambridge.feature.feed.presentation.shared.component.FeedCard
import com.cambridge.feature.feed.presentation.shared.component.FeedReadBadge
import com.cambridge.feature.feed.presentation.shared.component.FeedSuitabilityChip
import com.cambridge.feature.feed.presentation.shared.component.feedMetaTextStyle
import com.careercompass.core.ui.component.CareerCompassBadge
import com.careercompass.core.ui.component.CareerCompassBadgeTone
import com.careercompass.core.ui.theme.CareerCompassTheme

/**
 * Compact, single-action listing card for the "similar postings" section.
 *
 * Unlike the feed's [com.cambridge.feature.feed.presentation.FeedListingCard] it exposes no bookmark
 * toggle, because the detail contract has no per-similar-posting bookmark intent.
 *
 * ### 읽음은 싣고 수집일은 뺐다 (#165)
 *
 * 목록 카드(#140)는 마감일·수집일·읽음 셋을 다 지지만 여기서는 **마감일과 읽음만** 싣는다.
 *
 * 1. **이 자리에서 답해야 하는 질문이 다르다.** 유사 공고 줄은 「다음에 뭘 열까」를 고르는 자리다.
 *    클릭을 가르는 것은 아직 지원할 수 있는지(마감일)와 이미 봤는지(읽음)이고, 얼마나 최근에
 *    모아 온 것인지는 그 판단에 끼지 않는다 — 이 목록은 수집 시각이 아니라 **유사도**로 뽑혀 온다.
 * 2. **수집일이 목록 카드에 들어간 이유가 여기엔 없다.** 목록 카드에서 수집일 문구는 「오늘 수집」을
 *    색으로만 말하던 초록 점의 말을 넘겨받은 것이다([FeedListingUiModel.isNew]). 유사 공고 카드는
 *    그 점을 애초에 그리지 않으므로 문구가 되찾아 올 정보가 없다.
 * 3. **자리값.** 상세 화면 맨 아래에서 카드가 한 줄씩 길어지면 그만큼 스크롤이 늘어난다. 셋을 다
 *    넣으면 fontScale 2.0 에서 메타 줄이 두 줄로 접혀, 정작 급한 마감일이 읽음 배지와 갈라선다.
 *
 * 읽음 표시 규칙은 목록 카드와 **같다** — 문구와 형태가 정보를 지고([FeedReadBadge]) 흐린 제목 색은
 * 훑어볼 때만 거든다. 스크린 리더에는 카드의 `stateDescription` 으로 읽음·읽지 않음을 **둘 다** 실어,
 * 표시가 없는 쪽(읽지 않음)도 침묵하지 않게 한다.
 */
@Composable
internal fun SimilarPostingCard(
    listing: FeedListingUiModel,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = CareerCompassTheme.colors
    val readStateDescription =
        stringResource(
            if (listing.isRead) R.string.feed_listing_read_state else R.string.feed_listing_unread_state,
        )

    FeedCard(
        onClick = onClick,
        modifier = modifier.semantics { stateDescription = readStateDescription },
    ) {
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
            // 읽은 공고는 한 단계 흐리다 — 「읽음」 배지가 정보를 지고 이 색은 훑어볼 때만 거든다.
            color = if (listing.isRead) colors.onSurfaceVariant else colors.onSurface,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            style = CareerCompassTheme.typography.headline4,
        )
        // 마감일과 읽음 배지가 한 줄에 서지만, 큰 글꼴에서는 잘리는 대신 접히게 [FlowRow] 로 둔다 —
        // 카드가 세로로 길어질 뿐 마감일이 말줄임으로 사라지지 않는다.
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
            itemVerticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = listing.deadlineLabel,
                color = if (listing.isDeadlineUrgent) colors.actionDanger else colors.mutedContent,
                style = feedMetaTextStyle,
            )
            if (listing.isRead) {
                FeedReadBadge()
            }
        }
    }
}
