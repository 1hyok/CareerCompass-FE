package com.careercompass.core.common.url

/**
 * 앱 밖으로 여는 링크의 최소 안전 조건. `Intent.ACTION_VIEW` 로 넘기기 전에 이걸 통과시킨다.
 *
 * 서버가 준 값이라고 안전한 것이 아니다. 공고 원문 주소는 사용자가 등록한 게시판을 크롤러가 긁어서 채우는 값이라,
 * 게시판 쪽이 심어 둔 문자열이 그대로 여기까지 온다. `ACTION_VIEW` 는 스킴에 따라 브라우저가 아닌 다른 앱의
 * 딥링크를 열 수 있으므로, 원문 보기가 여는 것은 웹 페이지뿐이라는 것을 앱에서 잠근다.
 *
 * 통과 조건은 셋이다. 스킴이 `http` 또는 `https` 로 명시돼 있을 것, 호스트가 있을 것, 공백이 없을 것.
 * 스킴이 없는 값은 보완하지 않고 거절한다. 사용자가 입력한 게시판 주소는
 * `feature:feed:domain` 의 `BoardUrl` 이 https 를 보완해 주지만, 서버가 준 값에 우리가 스킴을 붙여 주면
 * 무엇을 열지 앱이 추측하는 셈이 된다.
 */
public object ExternalUrl {
    private val schemeRegex = Regex("""^([A-Za-z][A-Za-z0-9+.\-]*)://""")
    private val allowedSchemes = setOf("http", "https")

    /** 열어도 되는 링크면 앞뒤 공백을 턴 값을, 아니면 null 을 준다. */
    public fun openableOrNull(raw: String): String? {
        val trimmed = raw.trim()
        if (trimmed.isEmpty() || trimmed.any(Char::isWhitespace)) return null

        val scheme = schemeRegex.find(trimmed)?.groupValues?.get(1)?.lowercase() ?: return null
        if (scheme !in allowedSchemes) return null

        val rest = trimmed.substring(scheme.length + "://".length)
        val authorityEnd = rest.indexOfFirst { it == '/' || it == '?' || it == '#' }.takeIf { it >= 0 } ?: rest.length
        val host = rest.substring(0, authorityEnd).substringAfterLast('@').substringBefore(':')
        if (host.isEmpty()) return null

        return trimmed
    }

    /** 리포팅에 실을 스킴. 주소 전체는 남기지 않는다 — 쿼리에 무엇이 실려 있을지 모른다. */
    public fun schemeOrNull(raw: String): String? = schemeRegex.find(raw.trim())?.groupValues?.get(1)?.lowercase()
}
