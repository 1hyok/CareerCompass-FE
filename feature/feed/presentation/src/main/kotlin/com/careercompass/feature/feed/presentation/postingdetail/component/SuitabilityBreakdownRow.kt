package com.careercompass.feature.feed.presentation.postingdetail.component

import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.careercompass.core.ui.theme.CareerCompassTheme
import com.careercompass.feature.feed.presentation.R
import com.careercompass.feature.feed.presentation.postingdetail.SuitabilityAxisFulfillment
import com.careercompass.feature.feed.presentation.postingdetail.SuitabilityAxisUiModel

/**
 * One analysis axis: label, weight caption, score, a thin score bar, and a fulfilled/unfulfilled
 * badge (spec F3-2·F3-3).
 *
 * 충족 여부는 **글자와 형태**로 말한다 — 배지 문구(「충족」·「미충족」)와 그 앞의 점(찬 원 vs 빈 원)이
 * 정보를 지고, 색은 거들기만 한다. 예전에는 막대 색 하나가 유일한 구분이어서 색각 이상·흑백
 * 환경에서는 아무것도 전달하지 못했다.
 *
 * 배지를 위쪽 제목 줄이 아니라 **막대와 같은 줄**에 둔 이유는 폰트 배율이다. 제목 줄은 이미
 * 축 이름·가중치·점수로 꽉 차 있어 fontScale 2.0 에서 배지를 더하면 점수가 잘린다. 막대는
 * `weight` 로 줄어들 수 있으므로 배지가 커져도 잘리는 것이 없다.
 */
@Composable
internal fun SuitabilityBreakdownRow(
    axis: SuitabilityAxisUiModel,
    modifier: Modifier = Modifier,
) {
    val colors = CareerCompassTheme.colors
    val spacing = CareerCompassTheme.spacing
    val fulfillmentLabel = stringResource(axis.fulfillment.labelRes())
    val axisDescription =
        stringResource(
            R.string.feed_posting_detail_axis_content_description,
            axis.label,
            axis.weightLabel,
            axis.score,
            fulfillmentLabel,
        )
    val barColor =
        when (axis.fulfillment) {
            SuitabilityAxisFulfillment.Fulfilled -> colors.primary
            SuitabilityAxisFulfillment.Unfulfilled -> colors.inverseSurface
        }

    Column(
        modifier =
            modifier
                .fillMaxWidth()
                .semantics(mergeDescendants = true) { contentDescription = axisDescription },
        verticalArrangement = Arrangement.spacedBy(spacing.xSmall),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = axis.label,
                color = colors.onSurface,
                style = CareerCompassTheme.typography.bodyMedium,
            )
            Spacer(modifier = Modifier.width(spacing.xxSmall))
            Text(
                text = axis.weightLabel,
                color = colors.mutedContent,
                style = CareerCompassTheme.typography.caption,
            )
            Spacer(modifier = Modifier.weight(1f))
            Text(
                text = axis.score.toString(),
                color = barColor,
                style =
                    CareerCompassTheme.typography.labelMedium.copy(
                        fontWeight = FontWeight.Bold,
                    ),
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(spacing.small),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier =
                    Modifier
                        .weight(1f)
                        .height(6.dp)
                        .clip(CareerCompassTheme.shapes.pill)
                        .background(colors.surfaceVariant),
            ) {
                Box(
                    modifier =
                        Modifier
                            .fillMaxWidth(fraction = axis.score / MAX_SCORE)
                            .fillMaxHeight()
                            .background(barColor, CareerCompassTheme.shapes.pill),
                )
            }
            SuitabilityAxisFulfillmentBadge(
                fulfillment = axis.fulfillment,
                label = fulfillmentLabel,
            )
        }
    }
}

/**
 * 충족 여부 배지. 점의 **형태**(찬 원 = 충족, 빈 원 = 미충족)와 문구가 각각 홀로도 뜻을 지므로
 * 색을 못 보아도, 색이 없어도 읽힌다. 미충족은 오류가 아니라 「경계 아래」라서 경고색이 아닌
 * 중립색을 쓴다.
 */
@Composable
private fun SuitabilityAxisFulfillmentBadge(
    fulfillment: SuitabilityAxisFulfillment,
    label: String,
) {
    val colors = CareerCompassTheme.colors
    val container: Color
    val content: Color
    when (fulfillment) {
        SuitabilityAxisFulfillment.Fulfilled -> {
            container = colors.successContainer
            content = colors.onSuccessContainer
        }

        SuitabilityAxisFulfillment.Unfulfilled -> {
            container = colors.surfaceVariant
            content = colors.onSurfaceVariant
        }
    }

    val markerModifier =
        when (fulfillment) {
            SuitabilityAxisFulfillment.Fulfilled -> Modifier.background(content, CircleShape)
            SuitabilityAxisFulfillment.Unfulfilled -> Modifier.border(MARKER_BORDER_WIDTH, content, CircleShape)
        }

    Surface(
        shape = CareerCompassTheme.shapes.pill,
        color = container,
        contentColor = content,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(modifier = Modifier.size(MARKER_SIZE).then(markerModifier))
            Text(
                text = label,
                maxLines = 1,
                style =
                    CareerCompassTheme.typography.caption.copy(
                        lineHeight = 16.5.sp,
                        fontWeight = FontWeight.SemiBold,
                    ),
            )
        }
    }
}

@StringRes
private fun SuitabilityAxisFulfillment.labelRes(): Int =
    when (this) {
        SuitabilityAxisFulfillment.Fulfilled -> R.string.feed_posting_detail_axis_fulfilled
        SuitabilityAxisFulfillment.Unfulfilled -> R.string.feed_posting_detail_axis_unfulfilled
    }

private const val MAX_SCORE = 100f
private val MARKER_SIZE = 8.dp
private val MARKER_BORDER_WIDTH = 1.5.dp
