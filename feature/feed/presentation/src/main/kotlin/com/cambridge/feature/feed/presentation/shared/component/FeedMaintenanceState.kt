package com.cambridge.feature.feed.presentation.shared.component

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.cambridge.core.ui.component.CareerCompassMaintenanceState
import com.cambridge.core.ui.failure.FailureKind
import com.cambridge.core.ui.failure.description
import com.cambridge.core.ui.failure.display
import com.cambridge.core.ui.failure.title
import com.cambridge.feature.feed.presentation.R

/**
 * 서버 점검(503 `LLM_UNAVAILABLE`) 안내 — Figma 09 Edge Cases 「서버 점검」.
 *
 * 피드 홈·게시판 목록·공고 상세가 같은 문구를 쓴다. 세 화면이 각자 문구를 들면 「점검 중」이라는
 * 같은 사실을 서로 다르게 말하게 된다.
 *
 * 저장해 둔 스냅샷이 있는 화면만 [onOfflineClick] 을 넘겨 「오프라인 모드로 보기」를 연다. 문의처는
 * 아직 공개된 창구가 없어 넘기지 않는다 — 없는 주소를 적으면 사용자를 막다른 길로 보낸다.
 *
 * 제목·본문은 실패 표에서 읽는다([FailureKind.ServiceUnavailable], #204) — 503 은 피드만의 사건이
 * 아니라서, 이 모듈이 문구를 들고 있으면 다른 기능이 같은 상태를 다르게 안내하게 된다. 진행 배지만
 * 여기 남는다: 「점검 진행 중」은 이 화면의 구성 요소지 실패의 설명이 아니다.
 */
@Composable
internal fun FeedMaintenanceState(
    onRetryClick: () -> Unit,
    onOfflineClick: (() -> Unit)?,
    modifier: Modifier = Modifier,
) {
    val display = FailureKind.ServiceUnavailable.display()

    CareerCompassMaintenanceState(
        title = display.title(),
        description = display.description(),
        statusLabel = stringResource(R.string.feed_maintenance_status),
        onRefreshClick = onRetryClick,
        onOfflineClick = onOfflineClick,
        contactLabel = null,
        modifier = modifier,
    )
}
