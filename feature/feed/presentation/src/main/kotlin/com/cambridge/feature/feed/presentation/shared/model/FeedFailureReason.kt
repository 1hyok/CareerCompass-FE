package com.cambridge.feature.feed.presentation.shared.model

import com.cambridge.core.domain.error.CoreDataFailure

/**
 * 실패 화면이 갈라 그려야 하는 사유. 사용자가 할 일이 다른 것만 가른다.
 *
 * - [NetworkUnavailable] — 내 연결을 확인하면 된다.
 * - [Maintenance] — 서버가 스스로 「지금은 안 된다」고 알린 상태(503 `LLM_UNAVAILABLE`)다. 재시도만
 *   되풀이해도 소용없다는 것을 말해 줘야 한다.
 * - [Generic] — 사유를 특정할 수 없다. 잠시 뒤 재시도 안내로 접는다.
 */
public enum class FeedFailureReason {
    NetworkUnavailable,
    Maintenance,
    Generic,
}

public fun Throwable.toFeedFailureReason(): FeedFailureReason =
    when (this) {
        is CoreDataFailure.NetworkUnavailable -> FeedFailureReason.NetworkUnavailable
        is CoreDataFailure.ServiceUnavailable -> FeedFailureReason.Maintenance
        else -> FeedFailureReason.Generic
    }
