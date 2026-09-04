package com.careercompass.careercompass_fe.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.navigation.NavHostController
import com.careercompass.feature.feed.presentation.navigation.FeedNavActions
import com.careercompass.feature.feed.presentation.navigation.FeedRoute

/**
 * 피드 그래프가 앱 셸에 요청하는 이동의 구현.
 *
 * @param navigateToMyTab 프로필 입력 안내 — 마이 탭(profile 모듈 진입점이 생기기 전까지 자리표시자).
 * @param onSessionEnded 401 로 세션이 끝났다 — 시작 목적지를 다시 계산해 로그인으로 보낸다.
 */
@Composable
internal fun rememberFeedNavActions(
    navController: NavHostController,
    navigateToMyTab: () -> Unit,
    onSessionEnded: () -> Unit,
): FeedNavActions {
    val navigateToMyTabState by rememberUpdatedState(navigateToMyTab)
    val onSessionEndedState by rememberUpdatedState(onSessionEnded)
    return remember(navController) {
        object : FeedNavActions {
            override fun navigateToPostingDetail(postingId: Long) {
                navController.navigate(FeedRoute.PostingDetail(postingId))
            }

            override fun navigateToPostingRaw(postingId: Long) {
                navController.navigate(FeedRoute.PostingRaw(postingId))
            }

            override fun navigateToBoardRegister() {
                navController.navigate(FeedRoute.BoardRegister) { launchSingleTop = true }
            }

            override fun navigateToBoardList() {
                navController.navigate(FeedRoute.BoardList) { launchSingleTop = true }
            }

            override fun navigateToNotifications() {
                // notification 모듈이 진입점을 제공할 때까지 자리표시자.
                navController.navigate(Route.NotificationsPlaceholder) { launchSingleTop = true }
            }

            override fun navigateToProfileTab() {
                navigateToMyTabState()
            }

            override fun popBack() {
                navController.popBackStack()
            }

            override fun onSessionEnded() {
                onSessionEndedState()
            }
        }
    }
}
