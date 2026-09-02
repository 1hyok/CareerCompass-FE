package com.cambridge.feature.feed.presentation.navigation

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation.compose.navigation
import com.cambridge.feature.feed.presentation.board.BoardListEntry
import com.cambridge.feature.feed.presentation.board.BoardRegisterEntry
import com.cambridge.feature.feed.presentation.feed.FeedEntry
import com.cambridge.feature.feed.presentation.postingdetail.PostingDetailEntry
import com.cambridge.feature.feed.presentation.postingraw.PostingRawEntry

/** 피드 그래프 — [FeedGraphRoute] 아래에 홈·상세·원문·게시판 등록·게시판 목록을 등록한다. */
public fun NavGraphBuilder.feedNavGraph(actions: FeedNavActions) {
    navigation<FeedGraphRoute>(startDestination = FeedRoute.Home) {
        composable<FeedRoute.Home> {
            FeedEntry(
                onPostingClick = actions::navigateToPostingDetail,
                onNotificationsClick = actions::navigateToNotifications,
                onBoardRegisterClick = actions::navigateToBoardRegister,
                onBoardListClick = actions::navigateToBoardList,
                onProfileClick = actions::navigateToProfileTab,
                onSessionEnded = actions::onSessionEnded,
            )
        }
        composable<FeedRoute.PostingDetail> {
            PostingDetailEntry(
                onBackClick = actions::popBack,
                onPostingClick = actions::navigateToPostingDetail,
                onRawClick = actions::navigateToPostingRaw,
                onProfileClick = actions::navigateToProfileTab,
                onSessionEnded = actions::onSessionEnded,
            )
        }
        composable<FeedRoute.PostingRaw> {
            PostingRawEntry(
                onBackClick = actions::popBack,
                onSessionEnded = actions::onSessionEnded,
            )
        }
        composable<FeedRoute.BoardRegister> {
            BoardRegisterEntry(
                onBackClick = actions::popBack,
                onSessionEnded = actions::onSessionEnded,
            )
        }
        composable<FeedRoute.BoardList> {
            BoardListEntry(
                onBackClick = actions::popBack,
                onAddBoardClick = actions::navigateToBoardRegister,
                onSessionEnded = actions::onSessionEnded,
            )
        }
    }
}
