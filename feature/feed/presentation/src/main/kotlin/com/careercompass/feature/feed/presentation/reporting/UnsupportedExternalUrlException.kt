package com.careercompass.feature.feed.presentation.reporting

/**
 * 공고 원문 주소가 웹 주소가 아니어서 열지 않았다.
 *
 * 서버 응답 계약 위반이라 일시 오류가 아니라 결함으로 남긴다. 메시지에는 스킴만 싣는다. 주소 전체를 실으면
 * 쿼리에 붙어 온 것까지 리포팅 콘솔에 남는다.
 */
internal class UnsupportedExternalUrlException(
    scheme: String?,
) : IllegalStateException("unsupported external url scheme: ${scheme ?: "none"}")
