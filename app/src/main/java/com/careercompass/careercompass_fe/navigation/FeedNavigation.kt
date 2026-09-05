package com.careercompass.careercompass_fe.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.navigation.NavHostController
import com.careercompass.feature.feed.presentation.navigation.FeedExternalActions

/**
 * 피드 로컬 스택이 앱 셸에 남긴 이동의 구현. 상세·원문·게시판은 `FeedNavHost` 가 제 백스택으로 처리한다.
 *
 * @param navigateToMyTab 프로필 입력 안내 — 마이 탭(profile 모듈 진입점이 생기기 전까지 자리표시자).
 * @param onSessionEnded 401 로 세션이 끝났다 — 시작 목적지를 다시 계산해 로그인으로 보낸다.
 */
@Composable
internal fun rememberFeedExternalActions(
    navController: NavHostController,
    navigateToMyTab: () -> Unit,
    onSessionEnded: () -> Unit,
): FeedExternalActions {
    val navigateToMyTabState by rememberUpdatedState(navigateToMyTab)
    val onSessionEndedState by rememberUpdatedState(onSessionEnded)
    return remember(navController) {
        object : FeedExternalActions {
            override fun navigateToNotifications() {
                // notification 모듈이 진입점을 제공할 때까지 자리표시자.
                navController.navigate(Route.NotificationsPlaceholder) { launchSingleTop = true }
            }

            override fun navigateToProfileTab() = navigateToMyTabState()

            override fun onSessionEnded() = onSessionEndedState()
        }
    }
}
