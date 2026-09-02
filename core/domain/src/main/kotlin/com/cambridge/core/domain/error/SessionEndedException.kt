package com.cambridge.core.domain.error

/**
 * 요청이 기대한 세션이 더 이상 현재 세션이 아니다 — 로그아웃·세션 정리로 끝났거나 새 로그인으로 교체됐다.
 *
 * data 계층은 토큰 회전 결과를 저장하기 직전 세션 세대를 다시 비교해 로그아웃이 끼어든 회전을 이 예외로
 * 폐기한다. network 계층의 재발급 single-flight 는 이를 "이 요청은 포기하되 세션은 다시 지우지 않는다" 로
 * 다룬다 — 그 사이 새 로그인이 있었을 수 있어 세션을 또 지우면 새 세션이 날아간다.
 */
public class SessionEndedException(
    message: String,
) : IllegalStateException(message)
