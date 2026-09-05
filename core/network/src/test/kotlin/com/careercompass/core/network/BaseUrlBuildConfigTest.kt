package com.careercompass.core.network

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import retrofit2.Retrofit

/**
 * `BuildConfig.BASE_URL` 이 어느 빌드 타입에서든 Retrofit 이 그대로 받는 형태인지 고정한다.
 *
 * 주소는 빌드 타입마다 다른 값이 저장소 밖에서 주입된다(`core/network/build.gradle.kts` 의
 * `apiBaseUrls()`). 값이 갈리는 지점이 생겼으니 실제로 구워진 값을 한 번 본다. 단위 테스트 변형은
 * debug 하나뿐이라(`testReleaseUnitTest` 가 없다) 여기서 보는 값은 `BASE_URL_DEV` 쪽이고, release
 * 쪽 주입은 build-logic 의 BaseUrlGuardTest 가 값 수준에서 고정한다.
 *
 * 주소 형식 자체는 빌드 설정에서도 끊지만(BaseUrlGuard 의 형식 검증), 그것은 주입된 값에만 걸린다.
 * 자리표시자까지 포함해 실제로 구워진 값을 보는 것은 이 테스트다.
 */
class BaseUrlBuildConfigTest {
    @Test
    fun `구워진 주소는 https 스킴에 슬래시로 끝난다`() {
        assertTrue(BuildConfig.BASE_URL, BuildConfig.BASE_URL.startsWith("https://"))
        assertTrue(BuildConfig.BASE_URL, BuildConfig.BASE_URL.endsWith("/"))
    }

    @Test
    fun `Retrofit 이 구워진 주소를 baseUrl 로 받는다`() {
        val baseUrl =
            Retrofit
                .Builder()
                .baseUrl(BuildConfig.BASE_URL)
                .build()
                .baseUrl()

        assertEquals("https", baseUrl.scheme)
        assertTrue(baseUrl.host.isNotBlank())
        assertTrue(baseUrl.encodedPath.endsWith("/"))
    }
}
