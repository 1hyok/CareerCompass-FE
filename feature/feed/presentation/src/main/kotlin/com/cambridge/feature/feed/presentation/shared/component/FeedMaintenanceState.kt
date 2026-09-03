package com.cambridge.feature.feed.presentation.shared.component

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.cambridge.core.ui.component.CareerCompassMaintenanceState
import com.cambridge.feature.feed.presentation.R

/**
 * 서버 점검(503 `LLM_UNAVAILABLE`) 안내 — Figma 09 Edge Cases 「서버 점검」.
 *
 * 피드 홈·게시판 목록·공고 상세가 같은 문구를 쓴다. 세 화면이 각자 문구를 들면 「점검 중」이라는
 * 같은 사실을 서로 다르게 말하게 된다.
 *
 * 저장해 둔 스냅샷이 있는 화면만 [onOfflineClick] 을 넘겨 「오프라인 모드로 보기」를 연다. 문의처는
 * 아직 공개된 창구가 없어 넘기지 않는다 — 없는 주소를 적으면 사용자를 막다른 길로 보낸다.
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
