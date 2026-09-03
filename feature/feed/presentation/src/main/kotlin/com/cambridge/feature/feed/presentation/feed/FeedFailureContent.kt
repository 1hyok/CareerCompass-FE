package com.cambridge.feature.feed.presentation.feed

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.cambridge.core.ui.component.CareerCompassEmptyState
import com.cambridge.core.ui.component.CareerCompassNetworkErrorState
import com.cambridge.feature.feed.presentation.R
import com.cambridge.feature.feed.presentation.shared.component.FeedMaintenanceState
import com.cambridge.feature.feed.presentation.shared.model.FeedFailureReason

/**
 * 목록을 못 받았을 때 사유별로 갈리는 화면 — [FeedEntry] 가 상태 없이 그리는 부분만 떼어 냈다.
 *
 * @param onOfflineClick 저장해 둔 스냅샷이 있을 때만 넘긴다. `null` 이면 눌러도 보여 줄 것이 없으므로
 *  「오프라인 모드로 보기」를 아예 그리지 않는다. 점검 중에도 스냅샷은 유효하니 같은 길을 열어 둔다.
 */
@Composable
internal fun FeedFailureContent(
    reason: FeedFailureReason,
    onRetryClick: () -> Unit,
    onOfflineClick: (() -> Unit)?,
    modifier: Modifier = Modifier,
) {
    when (reason) {
        FeedFailureReason.NetworkUnavailable -> {
            CareerCompassNetworkErrorState(
                onRetryClick = onRetryClick,
                onOfflineClick = onOfflineClick,
                modifier = modifier,
            )
        }

        FeedFailureReason.Maintenance -> {
            FeedMaintenanceState(
                onRetryClick = onRetryClick,
                onOfflineClick = onOfflineClick,
                modifier = modifier,
            )
        }

        FeedFailureReason.Generic -> {
            CareerCompassEmptyState(
                title = stringResource(R.string.feed_error_title),
                description = stringResource(R.string.feed_error_description),
                actionText = stringResource(R.string.feed_error_retry),
                onActionClick = onRetryClick,
                modifier = modifier,
            )
        }
    }
}
