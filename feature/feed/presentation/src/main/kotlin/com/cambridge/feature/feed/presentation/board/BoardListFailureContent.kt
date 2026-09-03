package com.cambridge.feature.feed.presentation.board

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.cambridge.core.ui.component.CareerCompassEmptyState
import com.cambridge.core.ui.component.CareerCompassNetworkErrorState
import com.cambridge.feature.feed.presentation.R
import com.cambridge.feature.feed.presentation.shared.component.FeedMaintenanceState
import com.cambridge.feature.feed.presentation.shared.model.FeedFailureReason

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
            CareerCompassEmptyState(
                title = stringResource(R.string.feed_board_list_error_title),
                description = stringResource(R.string.feed_board_list_error_description),
                actionText = stringResource(R.string.feed_board_list_error_retry),
                onActionClick = onRetryClick,
                modifier = modifier,
            )
        }
    }
}
