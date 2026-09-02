package com.cambridge.feature.feed.presentation.navigation

/**
 * 피드 그래프가 바깥으로 요청하는 이동. 앱 셸이 `NavController` 로 구현해 [feedNavGraph] 에 넘긴다.
 *
 * 그래프 안 이동(상세·원문·게시판)도 여기로 모아 두어, 앱 셸이 탭 전환·세션 종료와 함께 한 곳에서
 * 백스택 정책을 정할 수 있게 한다.
 */
public interface FeedNavActions {
    public fun navigateToPostingDetail(postingId: Long)

    public fun navigateToPostingRaw(postingId: Long)

    public fun navigateToBoardRegister()

    public fun navigateToBoardList()

    public fun navigateToNotifications()

    /** 프로필 입력 안내 — 앱 셸이 마이 탭으로 보낸다. */
    public fun navigateToProfileTab()

    public fun popBack()

    /** 401 로 세션이 끝났다. 앱 셸이 로그인 화면으로 보낸다. */
    public fun onSessionEnded()
}
