package com.cambridge.careercompass_fe.navigation

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.cambridge.careercompass_fe.R
import com.cambridge.core.ui.component.CareerCompassBottomTab
import com.cambridge.core.ui.component.CareerCompassEmptyState

/**
 * 다른 담당 모듈(foryou·editor·profile)이 진입점을 제공하기 전까지 탭이 비어 보이지 않게 하는 자리표시자.
 * 모듈이 붙으면 이 컴포저블과 [Route] 의 해당 항목을 지운다.
 */
@Composable
internal fun PlaceholderTabScreen(
    tab: CareerCompassBottomTab,
    modifier: Modifier = Modifier,
) {
    val title =
        when (tab) {
            CareerCompassBottomTab.Feed -> stringResource(R.string.placeholder_feed_title)
            CareerCompassBottomTab.Analysis -> stringResource(R.string.placeholder_analysis_title)
            CareerCompassBottomTab.Applications -> stringResource(R.string.placeholder_applications_title)
            CareerCompassBottomTab.My -> stringResource(R.string.placeholder_my_title)
        }
    CareerCompassEmptyState(
        title = title,
        description = stringResource(R.string.placeholder_description),
        actionText = null,
        onActionClick = null,
        modifier = modifier.fillMaxSize(),
    )
}
