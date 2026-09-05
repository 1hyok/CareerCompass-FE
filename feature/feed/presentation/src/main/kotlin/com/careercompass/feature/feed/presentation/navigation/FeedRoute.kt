package com.careercompass.feature.feed.presentation.navigation

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

/**
 * 루트 `NavHost`(Navigation 2)에 남는 피드 host destination — 앱 셸이 시작 목적지와 하단 탭 대상으로 쓰고, 그 안에서
 * [FeedNavHost] 가 로컬 Navigation 3 스택을 돌린다.
 */
@Serializable
public data object FeedGraphRoute

/**
 * 피드 로컬 Navigation 3 스택의 키.
 *
 * 인자(공고 id)는 키 자체가 나른다 — Nav3 entry 엔 Nav2 의 `SavedStateHandle` 자동 채움이 없어, ViewModel 은 이 키를
 * assisted 주입으로 받는다(`PostingDetailViewModel.Factory`). `@Serializable` 은 프로세스 재생성 뒤 스택 복원에 쓰인다.
 */
@Serializable
public sealed interface FeedRoute : NavKey {
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
