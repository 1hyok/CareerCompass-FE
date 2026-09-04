package com.careercompass.core.common.result

import kotlin.coroutines.cancellation.CancellationException

/**
 * suspend 호출을 감싸 [Result] 로 돌려주되, [CancellationException] 만은 삼키지 않고 다시 던진다.
 *
 * stdlib `runCatching` 은 모든 [Throwable] 을 잡으므로 코루틴 취소까지 `Result.failure` 로 바꾼다.
 * 그러면 취소가 상위로 전파되지 못하고, 이미 취소된 코루틴에서 호출부의 실패 갈래(UI 상태 갱신·
 * 스낵바 안내 등)가 실행된다. Android 코루틴 모범 사례가 이 소비를 직접 금지한다.
 *
 * 쓰는 자리는 suspend 를 감싸는 경계다. 취소가 없는 동기 코드에는 stdlib `runCatching` 을 그대로 쓴다.
 */
public inline fun <T> runCatchingCancellable(block: () -> T): Result<T> =
    try {
        Result.success(block())
    } catch (e: CancellationException) {
        throw e
    } catch (e: Throwable) {
        Result.failure(e)
    }
