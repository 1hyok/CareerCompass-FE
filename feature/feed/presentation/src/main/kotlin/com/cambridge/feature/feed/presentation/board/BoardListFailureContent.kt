package com.cambridge.feature.feed.presentation.board

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.cambridge.core.ui.component.CareerCompassEmptyState
import com.cambridge.core.ui.component.CareerCompassNetworkErrorState
import com.cambridge.core.ui.failure.FailureSurface
import com.cambridge.core.ui.failure.actionLabel
import com.cambridge.core.ui.failure.description
import com.cambridge.core.ui.failure.display
import com.cambridge.core.ui.failure.title
import com.cambridge.feature.feed.presentation.shared.component.FeedMaintenanceState
import com.cambridge.feature.feed.presentation.shared.model.FeedFailureReason
import com.cambridge.feature.feed.presentation.shared.model.failureKind

/**
 * 게시판 목록을 못 받았을 때 사유별로 갈리는 화면 — [BoardListEntry] 가 상태 없이 그리는 부분만 떼어 냈다.
 *
 * 게시판 목록은 스냅샷을 저장하지 않으므로 「오프라인 모드로 보기」는 어느 사유에서도 열지 않는다.
 */
@Composable
internal fun BoardListFailureContent(
    reason: FeedFailureReason,
    onRetryClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    when (reason) {
        FeedFailureReason.NetworkUnavailable -> {
            CareerCompassNetworkErrorState(
                onRetryClick = onRetryClick,
                onOfflineClick = null,
                modifier = modifier,
            )
        }

        FeedFailureReason.Maintenance -> {
            FeedMaintenanceState(
                onRetryClick = onRetryClick,
                onOfflineClick = null,
                modifier = modifier,
            )
        }

        FeedFailureReason.Generic -> {
            // 문구는 실패 표에서 읽는다(#204). 같은 사유라도 「게시판」이라는 명사는 문맥이 채운다.
            val display = reason.failureKind.display(FailureSurface.Board)
            val actionText = display.actionLabel()
            CareerCompassEmptyState(
                title = display.title(),
                description = display.description(),
                actionText = actionText,
                onActionClick = onRetryClick.takeIf { actionText != null },
                modifier = modifier,
            )
        }
    }
}
