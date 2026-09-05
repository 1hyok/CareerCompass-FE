package com.careercompass.feature.feed.presentation.navigation

/**
 * 앱 셸이 피드 로컬 스택에 부탁하는 진입.
 *
 * 셸은 피처의 로컬 백스택에 직접 push 할 수 없다 — Nav2 시절 루트 `NavController.navigate(FeedRoute.PostingDetail)` 로
 * 하던 일이다. 대신 [FeedNavHost] 가 이 값을 받아 스택에 반영하고 1회 소비 콜백으로 비운다. host 는 루트 스택의 피드
 * 키가 보일 때만 그려지므로 로그인·온보딩 중에는 적용되지 않는다 — 딥링크의 인증 게이트가 그대로 지켜진다.
 */
public sealed interface FeedEntryRequest {
    /** 공고 상세 딥링크(`careercompass://postings/{id}`). 같은 상세가 이미 최상단이면 다시 쌓지 않는다. */
    public data class PostingDetail(
        val postingId: Long,
    ) : FeedEntryRequest

    /** 온보딩 완료 화면 「게시판 먼저 등록하기」. */
    public data object BoardRegister : FeedEntryRequest
}
