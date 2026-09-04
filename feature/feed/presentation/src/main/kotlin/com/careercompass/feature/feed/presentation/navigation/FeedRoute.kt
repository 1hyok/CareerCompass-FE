package com.careercompass.feature.feed.presentation.navigation

import kotlinx.serialization.Serializable

/** 피드 그래프 자체의 경로 — 앱 셸이 `NavHost` 의 시작 목적지나 하단 탭 대상으로 쓴다. */
@Serializable
public data object FeedGraphRoute

/** 피드 그래프 안의 화면. 인자는 type-safe navigation 이 `SavedStateHandle` 로 넘긴다. */
public sealed interface FeedRoute {
    @Serializable
    public data object Home : FeedRoute

    @Serializable
    public data class PostingDetail(
        val postingId: Long,
    ) : FeedRoute

    @Serializable
    public data class PostingRaw(
        val postingId: Long,
    ) : FeedRoute

    @Serializable
    public data object BoardRegister : FeedRoute

    @Serializable
    public data object BoardList : FeedRoute
}

/** `SavedStateHandle` 에서 공고 id 를 읽는 키 — [FeedRoute.PostingDetail]·[FeedRoute.PostingRaw] 의 프로퍼티 이름. */
public const val FEED_ARG_POSTING_ID: String = "postingId"
