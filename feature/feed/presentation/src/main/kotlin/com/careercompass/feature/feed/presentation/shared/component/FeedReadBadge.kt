package com.careercompass.feature.feed.presentation.shared.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.careercompass.core.ui.icon.CareerCompassIcons
import com.careercompass.core.ui.theme.CareerCompassTheme
import com.careercompass.feature.feed.presentation.R

/**
 * 「✓ 읽음」 배지 — 이미 열어 본 공고임을 카드 위에서 말한다(#140 이 목록 카드에 세운 규칙).
 *
 * 목록 카드(`FeedListingCard`)와 유사 공고 카드(`SimilarPostingCard`)가 **이 하나를 함께 쓴다.**
 * #165 가 유사 공고용으로 이 자리를 만들 때 `FeedScreen.kt` 는 #144 가 고치는 중이라 손대지 못해 같은
 * 배지가 두 벌로 갈려 있었고, #170 에서 이쪽으로 합쳤다 — 아래 세 가지가 이 배지의 계약인데, 두 벌로
 * 두면 한쪽만 고쳐져 두 화면이 다른 말을 하게 된다.
 *
 * 정보를 지는 것은 **문구(「읽음」)와 형태(체크 표시)** 이고 회색 알약은 거들기만 한다 — 색각 이상·
 * 흑백 환경에서 색만 남으면 아무것도 아니게 되기 때문이다(#141 의 충족 배지와 같은 원칙).
 *
 * 체크 크기를 dp 가 아니라 **sp 에서 뽑는 이유**는 폰트 배율이다. dp 로 못 박으면 글꼴이 2배가 될 때
 * 체크만 그대로라 문구 옆 점처럼 남고, 「형태로도 말한다」는 약속이 큰 글꼴에서만 깨진다.
 *
 * 스크린 리더에서는 스스로를 지운다([clearAndSetSemantics]). 배지를 다는 카드가 이미
 * `stateDescription` 으로 읽음·읽지 않음을 말하므로, 여기까지 읽히면 읽은 카드만 「읽음」을 두 번 듣는다.
 */
@Composable
internal fun FeedReadBadge(modifier: Modifier = Modifier) {
    val colors = CareerCompassTheme.colors
    val markerSize = with(LocalDensity.current) { FEED_READ_MARKER_SIZE.toDp() }

    Row(
        modifier =
            modifier
                .clearAndSetSemantics {}
                .background(colors.surfaceVariant, CareerCompassTheme.shapes.pill)
                .padding(horizontal = 8.dp, vertical = 2.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = CareerCompassIcons.Check,
            contentDescription = null,
            modifier = Modifier.size(markerSize),
            tint = colors.onSurfaceVariant,
        )
        Text(
            text = stringResource(R.string.feed_listing_read_state),
            color = colors.onSurfaceVariant,
            style = feedMetaTextStyle,
        )
    }
}

/**
 * 카드 메타 줄(마감일·수집일·읽음) 공통 글꼴 — 조각들이 같은 크기로 서야 줄이 접혀도 한 덩어리로 읽힌다.
 *
 * 배지와 같은 이유로 두 카드가 이 하나를 함께 쓴다(#170) — 글꼴이 갈리면 같은 줄에 선 조각들의
 * 기준선이 화면마다 달라진다.
 */
internal val feedMetaTextStyle: TextStyle
    @Composable get() =
        CareerCompassTheme.typography.caption.copy(
            fontSize = 12.sp,
            lineHeight = 18.sp,
            fontWeight = FontWeight.SemiBold,
        )

/** 「읽음」 배지의 체크 표시 크기. dp 가 아니라 sp 라서 폰트 배율을 따라 함께 커진다. */
private val FEED_READ_MARKER_SIZE = 14.sp
