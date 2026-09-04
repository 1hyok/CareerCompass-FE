package com.cambridge.feature.feed.presentation.shared.component

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cambridge.core.model.posting.SuitabilityLabel
import com.cambridge.core.ui.component.CareerCompassScoreChip
import com.cambridge.core.ui.component.CareerCompassScoreLevel
import com.cambridge.core.ui.theme.CareerCompassTheme
import com.cambridge.feature.feed.presentation.FeedSuitabilityState
import com.cambridge.feature.feed.presentation.R
import com.cambridge.feature.feed.presentation.shared.util.toScoreLevel

/**
 * Suitability readout of a listing card: the score chip when a score exists, otherwise a pill of the
 * same height so cards with and without a score line up (spec F2-3·F3-1).
 *
 * 점수가 없을 때의 문구는 사유를 그대로 말한다 — 프로필이 비어 산출을 못 하는 카드에 「분석 중」이라고
 * 적으면 기다리면 나올 것처럼 읽힌다.
 */
@Composable
internal fun FeedSuitabilityChip(
    state: FeedSuitabilityState,
    modifier: Modifier = Modifier,
) {
    when (state) {
        is FeedSuitabilityState.Scored -> {
            CareerCompassScoreChip(
                label = stringResource(R.string.feed_suitability_label),
                score = state.score,
                level = state.score.suitabilityLevel(),
                modifier = modifier,
                contentDescription = stringResource(R.string.feed_suitability_content_description, state.score),
            )
        }

        FeedSuitabilityState.Analyzing -> {
            FeedSuitabilityPlaceholderChip(
                label = stringResource(R.string.feed_suitability_analyzing),
                contentDescription = stringResource(R.string.feed_suitability_analyzing_content_description),
                modifier = modifier,
            )
        }

        FeedSuitabilityState.ProfileIncomplete -> {
            FeedSuitabilityPlaceholderChip(
                label = stringResource(R.string.feed_suitability_profile_incomplete),
                contentDescription = stringResource(R.string.feed_suitability_profile_incomplete_content_description),
                modifier = modifier,
            )
        }
    }
}

@Composable
private fun FeedSuitabilityPlaceholderChip(
    label: String,
    contentDescription: String,
    modifier: Modifier = Modifier,
) {
    val colors = CareerCompassTheme.colors

    Surface(
        modifier =
            modifier.semantics(mergeDescendants = true) {
                this.contentDescription = contentDescription
            },
        shape = CareerCompassTheme.shapes.pill,
        color = colors.subtleSurface,
    ) {
        Text(
            text = label,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
            maxLines = 1,
            color = colors.mutedContent,
            style =
                CareerCompassTheme.typography.caption.copy(
                    lineHeight = 16.5.sp,
                    fontWeight = FontWeight.Medium,
                ),
        )
    }
}

/**
 * 점수 → 칩 강조. 경계는 **도메인의 F3-2 표 하나**([SuitabilityLabel])에서만 나온다(이슈 #200).
 *
 * 예전에는 이 파일이 80·60 을 상수로 다시 적어 두고, 그 둘이 도메인 값과 같은지는 테스트가 감시했다.
 * 감시로 지킬 일이 아니라 애초에 한 벌만 두면 되는 일이다 — 이 이슈가 고치는 사고(같은 경계가 두 곳에서
 * 갈라짐)가 바로 그 모양이었다.
 *
 * 서버가 [com.cambridge.core.model.posting.Posting.scoreLabel] 을 함께 주지만 여기서는 쓰지 않는다.
 * 목록 카드는 점수만 싣고 레이블 글자를 그리지 않아, 레이블을 받아도 화면에 드러나는 것이 없다. 서버
 * 점수와 서버 레이블이 어긋나는 경우의 처분은 `docs/spec/suitability-score-boundary.md` 에 적어 뒀다.
 */
internal fun Int.suitabilityLevel(): CareerCompassScoreLevel = SuitabilityLabel.fromScore(this).toScoreLevel()
