package com.cambridge.core.domain.error

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
    /** 서버 응답 없이 전송 계층에서 끝난 실패. */
    public class NetworkUnavailable(
        cause: Throwable,
    ) : CoreDataFailure("network unavailable", code = null, cause = cause)

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
