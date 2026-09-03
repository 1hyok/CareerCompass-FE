package com.cambridge.core.domain.error

import java.io.IOException
import java.io.InterruptedIOException

/**
 * 데이터 요청 실패 중 **사유가 확인된 것**의 공통 루트 — API_SPEC v0.1 §9 에러 코드를 도메인 사유로 옮긴다.
 *
 * data 계층이 번역하고 presentation 은 이 루트로 좁혀 `when` 으로 가른다. 사유를 확인하지 못한 실패는
 * 원본 그대로 흘려보낸다. [code] 는 서버 코드 원문으로, 리포팅 필터에 쓴다.
 */
public sealed class CoreDataFailure(
    message: String,
    public val code: String?,
    cause: Throwable?,
) : Exception(message, cause) {
    /**
     * 서버 응답 없이 전송 계층에서 끝난 실패.
     *
     * data 계층이 `IOException` 을 **전부** 이 값으로 접는다 — 오프라인만이 아니라 cleartext 차단·TLS
     * 회귀·잘린 응답까지 같은 값이 된다. 그래서 원본을 [transportCause] 로 타입까지 붙여 들고 있는다:
     * 화면은 「네트워크 오류」 하나로 묶어도, 리포팅은 이 원인을 보고 우리 결함인지 갈라야 한다.
     */
    public class NetworkUnavailable(
        public val transportCause: IOException,
    ) : CoreDataFailure("network unavailable", code = null, cause = transportCause) {
        /**
         * 연결이 안 된 것이 아니라 **응답을 기다리다 우리가 먼저 끊은** 실패인지.
         *
         * 오래 걸리는 서버 작업(게시판 구조 감지 등)에서만 이 둘의 안내가 갈려야 한다. 연결이 멀쩡한
         * 사용자에게 「연결을 확인해 주세요」를 띄우면 헛수고를 시키고, 그 화면이 감지 실패 안내와
         * 구분되지 않으면 사이트가 지원되지 않는다는 오해까지 부른다(#134). 나머지 화면은 지금처럼
         * [NetworkUnavailable] 하나로 묶어도 사용자가 할 일이 같다 — 굳이 갈라 사유를 늘리지 않는다.
         *
         * 판정은 **예외 타입만** 본다. read·connect 초과는 `SocketTimeoutException`, OkHttp 의 call
         * timeout 은 `InterruptedIOException("timeout")` 이라 둘의 공통 상위 타입 하나로 갈린다. 메시지
         * 문구는 OkHttp·JDK 버전마다 바뀌므로 근거로 삼지 않는다.
         */
        public val isTimeout: Boolean
            get() = transportCause is InterruptedIOException
    }

    /** `AUTH_REQUIRED` / `AUTH_INVALID` (401). 세션 정리는 network 계층이 이미 끝냈다. */
    public class Unauthorized(
        code: String,
        cause: Throwable,
    ) : CoreDataFailure("unauthorized", code, cause)

    /** `PERMISSION_DENIED` (403). */
    public class Forbidden(
        code: String,
        cause: Throwable,
    ) : CoreDataFailure("forbidden", code, cause)

    /** `RESOURCE_NOT_FOUND` / `POSTING_NOT_FOUND` (404). */
    public class NotFound(
        code: String,
        cause: Throwable,
    ) : CoreDataFailure("not found", code, cause)

    /** `INVALID_INPUT` (400). [field] 는 서버가 지목한 필드. */
    public class InvalidInput(
        code: String,
        public val field: String?,
        cause: Throwable,
    ) : CoreDataFailure("invalid input", code, cause)

    /** `DUPLICATE_BOARD` (409). */
    public class DuplicateBoard(
        code: String,
        cause: Throwable,
    ) : CoreDataFailure("duplicate board", code, cause)

    /** `LIMIT_EXCEEDED` (422) — 경험 카드 30개·게시판 20개·지원서 10개 초과 등. */
    public class LimitExceeded(
        code: String,
        cause: Throwable,
    ) : CoreDataFailure("limit exceeded", code, cause)

    /** `PROFILE_INCOMPLETE` (422) — 적합도 산출 불가. */
    public class ProfileIncomplete(
        code: String,
        cause: Throwable,
    ) : CoreDataFailure("profile incomplete", code, cause)

    /** `PARSING_FAILED` (422) — 공고 파싱 실패. */
    public class ParsingFailed(
        code: String,
        cause: Throwable,
    ) : CoreDataFailure("parsing failed", code, cause)

    /** `BOARD_BLOCKED` (422) — 접근 차단 게시판. */
    public class BoardBlocked(
        code: String,
        cause: Throwable,
    ) : CoreDataFailure("board blocked", code, cause)

    /** `RATE_LIMITED` (429). */
    public class RateLimited(
        code: String,
        cause: Throwable,
    ) : CoreDataFailure("rate limited", code, cause)

    /** `LLM_UNAVAILABLE` (503) — 외부 의존성 장애. 사용자에게는 점검·재시도 안내. */
    public class ServiceUnavailable(
        code: String,
        cause: Throwable,
    ) : CoreDataFailure("service unavailable", code, cause)

    /** `INTERNAL_ERROR` (500) 및 5xx. */
    public class ServerError(
        code: String,
        cause: Throwable,
    ) : CoreDataFailure("server error", code, cause)
}
