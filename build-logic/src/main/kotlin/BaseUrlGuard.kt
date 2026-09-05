import org.gradle.api.GradleException
import org.gradle.api.Project
import java.net.URI

/**
 * 백엔드 실주소가 도착하기 전까지 debug·release 가 함께 쓰는 자리표시자.
 *
 * `.invalid` 는 예약 TLD 라 어떤 DNS 로도 해석되지 않는다(RFC 6761). 주소 주입을 잊은 빌드는
 * 남의 서버로 요청을 보내는 대신 그 자리에서 연결에 실패한다.
 */
const val PLACEHOLDER_API_BASE_URL: String = "https://api.careercompass.invalid/api/v1/"

/** debug 빌드가 굽는 개발 서버 주소 키. */
const val DEV_API_BASE_URL_KEY: String = "BASE_URL_DEV"

/** release 빌드가 굽는 운영 서버 주소 키. 가드는 이 값의 호스트만 운영으로 안다. */
const val PROD_API_BASE_URL_KEY: String = "BASE_URL_PROD"

/** 빌드 타입별 API 주소. [apiBaseUrls] 가 만든다. */
data class ApiBaseUrls(
    val debug: String,
    val release: String,
)

/**
 * 빌드 타입별 API 주소를 읽으면서 그 자리에서 debug 가드까지 배선한다.
 *
 * 주소는 저장소 밖(`local.properties`·환경변수)에서 [DEV_API_BASE_URL_KEY]·[PROD_API_BASE_URL_KEY]
 * 로 읽고, 없으면 [PLACEHOLDER_API_BASE_URL] 로 폴백한다. 읽기와
 * [requireDebugBaseUrlIsNotProduction] 을 한 호출로 묶은 이유는 소셜 키의 `socialLoginKey` 와 같다:
 * 같은 주소를 굽는 주입 지점이 늘 때 가드 호출 한 줄을 빠뜨리면, 운영을 가리키는 debug 산출물이
 * 조용히 다시 나온다.
 *
 * 어느 빌드가 어느 주소를 쓰는지는 `docs/api-base-url.md`.
 */
fun Project.apiBaseUrls(): ApiBaseUrls {
    val debugBaseUrl = injectedBaseUrl(DEV_API_BASE_URL_KEY)
    val releaseBaseUrl = injectedBaseUrl(PROD_API_BASE_URL_KEY)
    requireDebugBaseUrlIsNotProduction(debugBaseUrl, releaseBaseUrl)
    return ApiBaseUrls(debug = debugBaseUrl, release = releaseBaseUrl)
}

/**
 * debug 빌드가 운영 호스트를 가리키면 `preDebugBuild` 에서 끊는다.
 *
 * 운영 호스트는 [releaseBaseUrl] 이 자리표시자가 아닐 때의 그 호스트다 — 저장소는 운영 주소를
 * 갖고 있지 않으므로, 가드가 아는 운영은 이 빌드에 주입된 release 주소뿐이다. 경로가 달라도
 * 호스트가 같으면 같은 서버라 막는다(`/api/v1/` 과 `/api/v2/`). 뒤집어 말하면 운영 주소를
 * [PROD_API_BASE_URL_KEY] 없이 [DEV_API_BASE_URL_KEY] 에만 적은 머신은 이 가드가 못 잡는다.
 *
 * 서명 키 가드와 같은 자리에서 끊는 이유도 같다. debug variant 를 실제로 빌드할 때만 돌아야
 * release 빌드와 설정 단계가 이 판정에 걸리지 않는다.
 */
fun Project.requireDebugBaseUrlIsNotProduction(
    debugBaseUrl: String,
    releaseBaseUrl: String,
) {
    val productionBaseUrl = releaseBaseUrl.takeIf { it.trim() != PLACEHOLDER_API_BASE_URL }
    val debugHost = hostOf(debugBaseUrl)
    val productionHost = productionBaseUrl?.let(::hostOf)
    val pointsAtProduction =
        when {
            productionBaseUrl == null -> false

            debugHost != null && productionHost != null -> debugHost.equals(productionHost, ignoreCase = true)

            // 호스트를 못 뽑는 값은 형식 검증에서 이미 끊기지만, 저수준 진입점으로 직접 들어온
            // 경우까지 통과시키지 않으려고 문자열로 한 번 더 본다.
            else -> debugBaseUrl.trim().equals(productionBaseUrl.trim(), ignoreCase = true)
        }
    registerVariantBuildGuard(
        preBuildTaskName = "preDebugBuild",
        taskName = "checkDebugBaseUrlIsNotProduction",
        taskDescription = "debug 빌드 전에 API 주소가 운영 호스트가 아닌지 검증한다.",
        shouldFail = pointsAtProduction,
        failureMessage =
            """
            |debug 빌드가 운영 API 주소를 가리켜 빌드를 중단합니다: ${debugHost ?: debugBaseUrl}
            |$DEV_API_BASE_URL_KEY 와 $PROD_API_BASE_URL_KEY 의 호스트가 같습니다.
            |debug 산출물은 QA 와 로컬 개발이 쓰므로 운영 데이터를 건드리면 안 됩니다.
            |루트 local.properties 의 $DEV_API_BASE_URL_KEY 를 개발 서버 주소로 바꾸거나 지운 뒤 다시 빌드하세요.
            |어느 빌드가 어느 주소를 쓰는지는 docs/api-base-url.md 참고.
            """.trimMargin(),
    )
}

/**
 * 키 하나를 읽어 주소로 만든다. 값이 없으면 자리표시자, 있으면 형식을 그 자리에서 검증한다.
 *
 * 형식 검증을 configuration 단계에서 하는 이유는 실패 지점 때문이다. Retrofit 은 `/` 로 끝나지
 * 않는 `baseUrl` 을 런타임에 거부해서, 오타 하나가 앱 첫 요청에서야 크래시로 드러난다.
 */
private fun Project.injectedBaseUrl(keyName: String): String {
    val value = externalBuildValue(keyName)?.trim().orEmpty()
    if (value.isEmpty()) return PLACEHOLDER_API_BASE_URL
    val host = hostOf(value)
    val scheme = runCatching { URI(value).scheme }.getOrNull()?.lowercase()
    if (host.isNullOrBlank() || scheme !in setOf("http", "https") || !value.endsWith("/")) {
        throw GradleException(
            """
            |$keyName 의 주소 형식이 잘못돼 빌드를 중단합니다: $value
            |`https://<호스트>/<경로>/` 처럼 http(s) 스킴과 호스트를 갖추고 `/` 로 끝나야 합니다.
            |Retrofit 은 `/` 로 끝나지 않는 baseUrl 을 런타임에 거부합니다.
            """.trimMargin(),
        )
    }
    return value
}

private fun hostOf(url: String): String? = runCatching { URI(url.trim()).host }.getOrNull()
