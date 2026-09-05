package com.careercompass.feature.feed.presentation.navigation

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation.compose.navigation
import com.careercompass.feature.feed.presentation.board.BoardListScreen
import com.careercompass.feature.feed.presentation.board.BoardRegisterScreen
import com.careercompass.feature.feed.presentation.feed.FeedScreen
import com.careercompass.feature.feed.presentation.postingdetail.PostingDetailScreen
import com.careercompass.feature.feed.presentation.postingraw.PostingRawScreen

/** 피드 그래프 — [FeedGraphRoute] 아래에 홈·상세·원문·게시판 등록·게시판 목록을 등록한다. */
public fun NavGraphBuilder.feedNavGraph(actions: FeedNavActions) {
    navigation<FeedGraphRoute>(startDestination = FeedRoute.Home) {
        composable<FeedRoute.Home> {
            FeedScreen(
                onPostingClick = actions::navigateToPostingDetail,
                onNotificationsClick = actions::navigateToNotifications,
                onBoardRegisterClick = actions::navigateToBoardRegister,
                onBoardListClick = actions::navigateToBoardList,
                onProfileClick = actions::navigateToProfileTab,
                onSessionEnded = actions::onSessionEnded,
            )
        }
        composable<FeedRoute.PostingDetail> {
            PostingDetailScreen(
                onBackClick = actions::popBack,
                onPostingClick = actions::navigateToPostingDetail,
                onRawClick = actions::navigateToPostingRaw,
                onProfileClick = actions::navigateToProfileTab,
                onSessionEnded = actions::onSessionEnded,
            )
        }
        composable<FeedRoute.PostingRaw> {
            PostingRawScreen(
                onBackClick = actions::popBack,
                onSessionEnded = actions::onSessionEnded,
            )
        }
        composable<FeedRoute.BoardRegister> {
            BoardRegisterScreen(
                onBackClick = actions::popBack,
                onSessionEnded = actions::onSessionEnded,
            )
        }
        composable<FeedRoute.BoardList> {
            BoardListScreen(
                onBackClick = actions::popBack,
                onAddBoardClick = actions::navigateToBoardRegister,
                onSessionEnded = actions::onSessionEnded,
            )
        }
    }
}
