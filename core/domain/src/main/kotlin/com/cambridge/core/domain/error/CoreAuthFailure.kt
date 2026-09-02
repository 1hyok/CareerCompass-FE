package com.cambridge.core.domain.error

/**
 * 인증 흐름(소셜 로그인·토큰 재발급·지문 로그인)의 실패 중 **사유가 확인된 것**의 공통 루트.
 *
 * data 계층이 `ApiException`·`IOException` 을 이 계열로 번역하고, presentation 은 이 루트로 좁힌 뒤 `when` 으로
 * 가른다 — 하위 타입이 늘면 컴파일러가 소비처를 잡아준다. 사유를 확인하지 못한 실패는 번역하지 않고 원본
 * 그대로 흘려보내므로, 소비처의 `else` 분기는 계속 필요하다.
 *
 * `message` 는 리포팅 콘솔용 정적 진단 문구다. 화면 문구는 호출처 리소스가 갖는다 — 서버 `message` 는
 * 사용자 노출용이라는 규정이 없다.
 */
public sealed class CoreAuthFailure(
    message: String,
    cause: Throwable?,
) : Exception(message, cause) {
    /** 서버 응답 없이 전송 계층에서 끝난 실패(DNS·타임아웃·연결 거부). */
    public class NetworkUnavailable(
        cause: Throwable,
    ) : CoreAuthFailure("network unavailable", cause)

    /** 소셜 로그인 거절(`AUTH_INVALID` 등). 입력 필드와 무관한 실패라 별도 안내로 표시한다. */
    public class SocialLoginRejected(
        cause: Throwable,
    ) : CoreAuthFailure("social login rejected", cause)

    /** refresh 토큰까지 거절돼 세션이 끝났다. 소비처는 로그인 화면으로 보낸다. */
    public class SessionExpired(
        cause: Throwable?,
    ) : CoreAuthFailure("session expired", cause)

    /**
     * 사용자가 소셜 로그인·생체 인증을 직접 취소했다는 사실.
     *
     * 코루틴의 [kotlinx.coroutines.CancellationException] 과 구분해 구조화된 동시성이 깨지지 않도록 별도
     * 타입으로 둔다. 소비처는 리포팅·에러 표시에서 제외하고 조용히 흘려보낸다.
     */
    public class UserCancelledAuth : CoreAuthFailure("user cancelled auth", null)
}
