package com.careercompass.feature.feed.presentation.postingdetail.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.careercompass.core.model.posting.SuitabilityLabel
import com.careercompass.core.ui.component.CareerCompassBadge
import com.careercompass.core.ui.theme.CareerCompassTheme
import com.careercompass.feature.feed.presentation.R

/**
 * Large score readout with a level badge and a determinate progress bar (spec F3-3).
 *
 * 막대 색은 [level] 의 구간을 따른다([gaugeColor]) — 색이 갈리는 지점이 곧 F3-2 가 이름을 바꾸는
 * 지점이고, 그 이름은 바로 옆 배지에 글자로 적힌다(이슈 #200).
 *
 * [level] 은 서버가 준 레이블이다. 점수에서 다시 계산하지 않는 이유 — 배지 문구([levelLabel])도 같은
 * 레이블에서 나오므로, 서버 점수와 서버 레이블이 어쩌다 어긋나더라도 **한 화면 안에서는 색과 글자가
 * 늘 같은 말을 한다.**
 */
@Composable
internal fun SuitabilityGauge(
    score: Int,
    levelLabel: String,
    level: SuitabilityLabel,
    modifier: Modifier = Modifier,
) {
    val colors = CareerCompassTheme.colors
    val gaugeColor = level.gaugeColor(colors)
    val gaugeDescription =
        stringResource(
            R.string.feed_posting_detail_suitability_content_description,
            score,
            levelLabel,
        )

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(CareerCompassTheme.spacing.medium),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = score.toString(),
                modifier = Modifier.alignByBaseline(),
                // 구간 색은 막대와 배지가 진다. 숫자까지 물들이면 「보통」의 주황(#F59E0B)이 흰 바탕에서
                // 2:1 대비로 떨어져 이 화면에서 가장 중요한 값이 가장 안 읽히게 된다.
                color = colors.onSurface,
                style =
                    CareerCompassTheme.typography.displayLarge.copy(
                        fontSize = 44.sp,
                        lineHeight = 52.sp,
                    ),
            )
            Text(
                text = stringResource(R.string.feed_posting_detail_score_max),
                modifier =
                    Modifier
                        .alignByBaseline()
                        .padding(start = 4.dp),
                color = colors.mutedContent,
                style = CareerCompassTheme.typography.bodyLarge,
            )
            Spacer(modifier = Modifier.weight(1f))
            CareerCompassBadge(
                label = levelLabel,
                tone = level.badgeTone(),
            )
        }
        LinearProgressIndicator(
            progress = { score / MAX_SCORE },
            modifier =
                Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(CareerCompassTheme.shapes.pill)
                    .semantics { contentDescription = gaugeDescription },
            color = gaugeColor,
            trackColor = colors.surfaceVariant,
            strokeCap = StrokeCap.Round,
            gapSize = 0.dp,
            drawStopIndicator = {},
        )
    }
}

private const val MAX_SCORE = 100f
