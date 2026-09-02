package com.cambridge.core.common.reporting

import kotlin.coroutines.cancellation.CancellationException

/**
 * 앱을 죽이지 않고 흡수되는 실패(handled 예외)를 개발자 텔레메트리로 남기는 창구.
 *
 * 크래시 리포팅의 자동 수집은 uncaught 예외만 잡는다. 로그인 실패처럼 `Result.failure` 로 흡수되어
 * 스낵바 한 줄로 끝나는 오류는 이 인터페이스를 거쳐 명시적으로 기록해야 실기 QA 전까지 미검출로
 * 남지 않는다.
 *
 * 구현은 app 모듈에만 두고 여기서는 추상만 노출한다 — presentation·data 레이어가 크래시 리포팅
 * SDK 에 직접 의존하지 않게 하기 위함이다. 사용자 노출 문구와는 별개 층이다.
 */
public interface ErrorReporter {
    /**
     * 실패 [throwable] 을 non-fatal 로 기록한다. 코루틴 취소는 기록하지 않고 반환한다.
     *
     * 예외 원문은 여기서 버린다: [Throwable.message] 와 cause 체인에는 서버 응답 본문·OAuth 오류
     * 응답이 그대로 담겨 있을 수 있고, 그 안에 이메일이나 자격증명 조각이 섞이면 콘솔로 유출된다.
     * 그래서 타입·스택트레이스만 남기고, 버린 문구 대신 타입 이름을 속성으로 넘긴다.
     *
     * @param attributes 이 실패 이벤트에만 붙는 컨텍스트. 어느 단계에서 깨졌는지 구분할 최소 정보만
     *                   담는다(예: `"stage" to "social_login"`). 개인정보·자격증명은 넣지 않는다.
     */
    public fun recordFailure(
        throwable: Throwable,
        attributes: Map<String, String> = emptyMap(),
    ) {
        if (throwable is CancellationException) return
        writeFailure(
            throwable = redact(throwable),
            attributes = attributes + throwable.typeAttributes(),
        )
    }

    /** 걸러진 실패를 실제 리포팅 백엔드에 쓴다. 구현이 채우는 건 이쪽이고, 호출부는 [recordFailure] 만 쓴다. */
    public fun writeFailure(
        throwable: Throwable,
        attributes: Map<String, String>,
    )
}

/** 리포팅 속성 키 — 원문 대신 남기는 예외 타입 정보. */
public const val ERROR_REPORT_KEY_TYPE: String = "error_type"
public const val ERROR_REPORT_KEY_CAUSE_TYPE: String = "error_cause_type"

/**
 * 문구를 버리고 타입·스택트레이스만 남긴 사본. 리포팅 백엔드에는 이 사본이 올라간다.
 *
 * cause 는 잇지 않는다 — 원인 예외의 문구도 같은 위험을 갖기 때문이다(타입은 속성으로 남는다).
 */
private class RedactedFailure(
    originalType: String,
) : Throwable(originalType)

private fun redact(throwable: Throwable): Throwable =
    RedactedFailure(throwable.javaClass.name).apply {
        stackTrace = throwable.stackTrace
    }

private fun Throwable.typeAttributes(): Map<String, String> {
    val type = javaClass.name
    val causeType = cause?.javaClass?.name
    return buildMap {
        put(ERROR_REPORT_KEY_TYPE, type)
        causeType?.let { put(ERROR_REPORT_KEY_CAUSE_TYPE, it) }
    }
}
