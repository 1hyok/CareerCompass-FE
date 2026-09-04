package com.cambridge.feature.feed.presentation.shared.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import com.cambridge.core.ui.component.CareerCompassMaintenanceState
import com.cambridge.core.ui.theme.CareerCompassTheme
import com.cambridge.feature.feed.presentation.R

/*
 * 서버 점검(503 `LLM_UNAVAILABLE`)을 말하는 자리는 이 파일이 전부다.
 *
 * 화면 한 장을 쓸 수 있으면 [FeedMaintenanceState], 폼·입력칸이 살아 있어야 하면 [FeedMaintenanceNotice] 다.
 * 둘 다 `feed_maintenance_*` 리소스만 읽는다 — 문구를 새로 짓는 자리를 남기지 않는다. 에러 코드별 문구 표가
 * 생기면(#204) 바꿀 곳도 이 파일 하나다.
 */

/**
 * 서버 점검(503 `LLM_UNAVAILABLE`) 안내 — Figma 09 Edge Cases 「서버 점검」.
 *
 * 피드 홈·게시판 목록·공고 상세·원문 보기가 같은 문구를 쓴다. 네 화면이 각자 문구를 들면 「점검 중」이라는
 * 같은 사실을 서로 다르게 말하게 된다 — 상세에서 「원문 보기」를 눌렀을 때 바로 앞 화면과 다른 말을 하던
 * 것이 그 예다(#212).
 *
 * 저장해 둔 스냅샷이 있는 화면만 [onOfflineClick] 을 넘겨 「오프라인 모드로 보기」를 연다. 문의처는
 * 아직 공개된 창구가 없어 넘기지 않는다 — 없는 주소를 적으면 사용자를 막다른 길로 보낸다.
 *
 * 행동 버튼은 부품이 고정한 「새로고침」 하나다(`core_ui_state_refresh`). 네트워크 실패의 「다시 시도」와
 * 문구가 다른 것은 우연이 아니다 — 점검은 사용자가 연결을 살려 놓고 누르는 실패가 아니라 서버가 돌아오기를
 * 기다리는 실패라, 같은 말로 재시도를 권하지 않는다.
 */
@Composable
internal fun FeedMaintenanceState(
    onRetryClick: () -> Unit,
    onOfflineClick: (() -> Unit)?,
    modifier: Modifier = Modifier,
) {
    CareerCompassMaintenanceState(
        title = stringResource(R.string.feed_maintenance_title),
        description = stringResource(R.string.feed_maintenance_description),
        statusLabel = stringResource(R.string.feed_maintenance_status),
        onRefreshClick = onRetryClick,
        onOfflineClick = onOfflineClick,
        contactLabel = null,
        modifier = modifier,
    )
}

/**
 * 점검 안내를 **폼 안에 끼워 넣는** 변형 — 화면 한 장을 쓸 수 없는 자리를 위한 것이다.
 *
 * [FeedMaintenanceState] 의 뼈대(`CareerCompassStateLayout`)는 `fillMaxSize` 라, 게시판 등록처럼 URL 입력칸과
 * 「구조 분석하기」가 그대로 살아 있어야 하는 화면에는 들어갈 수 없다. 그렇다고 문구를 새로 지으면 같은 503 을
 * 화면마다 다르게 말하게 되므로, 자리만 바꾸고 문구는 같은 리소스를 읽는다.
 *
 * **행동 버튼을 그리지 않는다.** 점검은 사용자가 되돌릴 수 있는 조건이 아니라서 「다시 시도」를 붙이면 눌러도
 * 아무 일 없는 버튼이 된다(엣지 상태 §3 — 「할 수 있는 일이 있는 상태에만 행동 버튼」). 감지 타임아웃은 사이트가
 * 느렸을 뿐이라 다시 시도할 뜻이 있어 버튼을 주지만, 점검은 서버가 돌아와야 답이 달라진다. 다시 눌러 볼 길이
 * 아예 막히는 것은 아니다 — 위의 「구조 분석하기」는 그대로 눌린다.
 *
 * 경고(노랑)가 아니라 정보(파랑) 톤인 것도 같은 이유다. 사용자가 넣은 URL 이나 그 사이트를 의심하게 만들 일이
 * 아니다.
 */
@Composable
internal fun FeedMaintenanceNotice(modifier: Modifier = Modifier) {
    val colors = CareerCompassTheme.colors
    val spacing = CareerCompassTheme.spacing

    Column(
        modifier =
            modifier
                .fillMaxWidth()
                .background(colors.infoContainer, CareerCompassTheme.shapes.largeControl)
                .padding(spacing.large)
                .semantics(mergeDescendants = true) { liveRegion = LiveRegionMode.Polite },
        verticalArrangement = Arrangement.spacedBy(spacing.xxSmall),
    ) {
        Text(
            text = stringResource(R.string.feed_maintenance_title),
            color = colors.onInfoContainer,
            style = CareerCompassTheme.typography.headline4,
        )
        Text(
            text = stringResource(R.string.feed_maintenance_description),
            color = colors.onInfoContainer,
            style = CareerCompassTheme.typography.bodyMedium,
        )
    }
}
