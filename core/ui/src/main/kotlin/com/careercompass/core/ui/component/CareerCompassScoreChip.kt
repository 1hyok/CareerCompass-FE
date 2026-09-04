package com.careercompass.core.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.careercompass.core.ui.theme.CareerCompassTheme

/** Visual emphasis levels for [CareerCompassScoreChip]. */
public enum class CareerCompassScoreLevel {
    High,
    Mid,
    Low,
}

/**
 * 단계를 채운 눈금 수로 옮긴다 — High 3칸, Mid 2칸, Low 1칸.
 *
 * 칩의 세 단계는 컨테이너 색으로만 갈렸다. 라이트에서 그 세 색(brand/50 · neutral/100 · neutral/50)은
 * 서로 1.01~1.04:1 이라 색을 못 보면 **정상 시야에서도** 구별되지 않는다. 그래서 색이 아니라 개수로
 * 다시 말한다(이슈 #205).
 */
internal fun CareerCompassScoreLevel.filledSteps(): Int =
    when (this) {
        CareerCompassScoreLevel.High -> 3
        CareerCompassScoreLevel.Mid -> 2
        CareerCompassScoreLevel.Low -> 1
    }

/**
 * Displays a score from 0 through 100 with a single accessibility description.
 *
 * 단계([level])는 컨테이너 색과 함께 **채운 눈금 개수**로도 나간다 — 색만으로는 세 단계가 갈리지 않는다
 * ([filledSteps]). 칩 전체가 하나의 접근성 노드라 눈금은 따로 읽히지 않고 [contentDescription] 이 대신한다.
 */
@Composable
public fun CareerCompassScoreChip(
    label: String,
    score: Int,
    level: CareerCompassScoreLevel,
    modifier: Modifier = Modifier,
    contentDescription: String = "$label $score",
) {
    require(score in SCORE_RANGE) {
        "score must be between ${SCORE_RANGE.first} and ${SCORE_RANGE.last}: $score"
    }

    val colors = CareerCompassTheme.colors
    val scoreColors =
        when (level) {
            CareerCompassScoreLevel.High -> {
                // 라벨은 mutedContent 였다. neutral/500·400 은 중립 바탕에서만 4.5:1 을 지키는 값이라
                // 브랜드 컨테이너 위에서는 라이트 4.50:1(경계값)·다크 3.85:1 로 무너졌다 — 중립 컨테이너의
                // 짝인 onSurfaceVariant 로 바꾸면 라이트 7.42:1 · 다크 6.56:1 이다.
                ScoreColors(
                    container = colors.primaryContainer,
                    label = colors.onSurfaceVariant,
                    score = colors.onSuccessContainer,
                )
            }

            CareerCompassScoreLevel.Mid -> {
                ScoreColors(
                    container = colors.surfaceVariant,
                    label = colors.onSurfaceVariant,
                    score = colors.onSurface,
                )
            }

            CareerCompassScoreLevel.Low -> {
                ScoreColors(
                    container = colors.subtleSurface,
                    label = colors.mutedContent,
                    score = colors.mutedContent,
                )
            }
        }

    Surface(
        modifier =
            modifier.semantics(mergeDescendants = true) {
                this.contentDescription = contentDescription
            },
        shape = CareerCompassTheme.shapes.pill,
        color = scoreColors.container,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = label,
                modifier = Modifier.clearAndSetSemantics {},
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                color = scoreColors.label,
                style =
                    CareerCompassTheme.typography.caption.copy(
                        lineHeight = 16.5.sp,
                        fontWeight = FontWeight.Medium,
                    ),
            )
            Text(
                text = score.toString(),
                modifier = Modifier.clearAndSetSemantics {},
                maxLines = 1,
                color = scoreColors.score,
                style =
                    CareerCompassTheme.typography.labelMedium.copy(
                        fontSize = 13.sp,
                        lineHeight = 19.5.sp,
                        fontWeight = FontWeight.Bold,
                    ),
            )
            ScoreLevelMeter(
                filledSteps = level.filledSteps(),
                filledColor = scoreColors.score,
                emptyColor = scoreColors.label,
            )
        }
    }
}

/**
 * 단계 눈금. 찬 점과 빈 점의 **모양**이 개수를 지므로 색을 못 보아도 세 단계가 갈린다
 * ([CareerCompassScoreLevel.filledSteps] 참고).
 *
 * 점은 `dp` 로 그려 글꼴 배율을 타지 않는다 — 배율 2.0 에서 숫자만 커지고 눈금은 그대로라
 * 칩이 가로로 더 늘어나지 않는다. 칩 전체가 하나의 접근성 노드이므로 눈금은 읽히지 않는다.
 */
@Composable
private fun ScoreLevelMeter(
    filledSteps: Int,
    filledColor: Color,
    emptyColor: Color,
) {
    Row(
        modifier = Modifier.clearAndSetSemantics {},
        horizontalArrangement = Arrangement.spacedBy(METER_DOT_GAP),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        repeat(METER_STEPS) { index ->
            val filled = index < filledSteps
            Box(
                modifier =
                    Modifier
                        .size(METER_DOT_SIZE)
                        .then(
                            if (filled) {
                                Modifier.background(filledColor, CircleShape)
                            } else {
                                Modifier.border(METER_DOT_BORDER, emptyColor, CircleShape)
                            },
                        ),
            )
        }
    }
}

private data class ScoreColors(
    val container: Color,
    val label: Color,
    val score: Color,
)

private val SCORE_RANGE = 0..100

private const val METER_STEPS = 3
private val METER_DOT_SIZE = 5.dp
private val METER_DOT_GAP = 3.dp
private val METER_DOT_BORDER = 1.dp
