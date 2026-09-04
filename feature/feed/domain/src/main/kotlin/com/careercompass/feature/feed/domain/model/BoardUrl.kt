package com.careercompass.feature.feed.domain.model

import com.careercompass.feature.feed.domain.error.FeedFailure

/**
 * 게시판 URL 정규화 — 감지·등록 전에 사용자 입력을 서버에 보낼 형태로 다듬는다.
 *
 * - 앞뒤 공백 제거, 스킴이 없으면 `https://` 보완, 스킴·호스트는 소문자.
 * - `http`/`https` 외 스킴, 호스트 없음, 내부 공백은 [FeedFailure.InvalidBoardUrl] 로 거절한다.
 *
 * `java.net.URI` 를 쓰지 않는 이유 — 주소창에서 복사한 한글 경로처럼 인코딩되지 않은 문자를 예외로 튕겨
 * 멀쩡한 게시판 주소를 거절하게 된다. 서버가 실제 접근 가능성을 검증하므로 여기서는 형태만 본다.
 */
public object BoardUrl {
    private val schemeRegex = Regex("""^([A-Za-z][A-Za-z0-9+.\-]*):""")
    private val allowedSchemes = setOf("http", "https")

    public fun normalize(raw: String): Result<String> {
        val trimmed = raw.trim()
        if (trimmed.isEmpty() || trimmed.any(Char::isWhitespace)) return invalid(raw)

        val schemeMatch = schemeRegex.find(trimmed)
        val scheme: String
        val rest: String
        if (schemeMatch == null) {
            scheme = "https"
            rest = trimmed
        } else {
            scheme = schemeMatch.groupValues[1].lowercase()
            if (scheme !in allowedSchemes) return invalid(raw)
            val afterScheme = trimmed.substring(schemeMatch.range.last + 1)
            if (!afterScheme.startsWith("//")) return invalid(raw)
            rest = afterScheme.removePrefix("//")
        }

        val authorityEnd = rest.indexOfFirst { it == '/' || it == '?' || it == '#' }.takeIf { it >= 0 } ?: rest.length
        val authority = rest.substring(0, authorityEnd)
        val pathAndQuery = rest.substring(authorityEnd)
        val hostAndPort = authority.substringAfterLast('@')
        val host = hostAndPort.substringBefore(':')
        if (host.isEmpty()) return invalid(raw)

        val normalizedAuthority = authority.dropLast(hostAndPort.length) + hostAndPort.lowercase()
        return Result.success("$scheme://$normalizedAuthority$pathAndQuery")
    }

    private fun invalid(raw: String): Result<String> = Result.failure(FeedFailure.InvalidBoardUrl(raw))
}
