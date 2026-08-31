package com.cambridge.core.network.service

import com.cambridge.core.network.dto.SocialLoginRequestDto
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonObject
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.junit.AfterClass
import org.junit.Assert.assertEquals
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.BeforeClass
import org.junit.Test
import org.testcontainers.DockerClientFactory
import org.testcontainers.mockserver.MockServerContainer
import org.testcontainers.utility.DockerImageName
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory

/**
 * Retrofit 선언과 kotlinx-serialization DTO 를 실제 HTTP 소켓 경계까지 통과시키는 smoke 계약.
 *
 * 단순 fixture decode 테스트와 달리 method/path/header/request body 가 하나라도 바뀌면 MockServer 의
 * strict matcher 가 응답하지 않아 실패한다. 실제 운영 서버를 호출하지 않으므로 계정·secret 은 필요 없다.
 * 일반 unit-test 실행에서는 환경 플래그로 건너뛰고, 전용 Actions workflow 가 명시적으로 활성화한다.
 * 전용 workflow 에서 Docker 런타임을 찾지 못하면 skip 하지 않고 즉시 실패한다.
 *
 * 엔드포인트가 늘면 여기에 케이스를 추가한다 — API_SPEC v0.1 의 각 도메인이 대상이다.
 */
class ApiWireContractSmokeTest {
    private lateinit var authService: AuthApiService

    @Before
    fun setUp() {
        controlPut("/mockserver/reset")

        val okHttpClient =
            OkHttpClient
                .Builder()
                .addInterceptor { chain ->
                    chain.proceed(
                        chain
                            .request()
                            .newBuilder()
                            .header("Authorization", "Bearer contract-token")
                            .build(),
                    )
                }.build()
        val publicRetrofit =
            Retrofit
                .Builder()
                .baseUrl("${mockServer.endpoint}/api/v1/")
                .addConverterFactory(
                    wireJson.asConverterFactory("application/json".toMediaType()),
                ).client(okHttpClient)
                .build()

        authService = publicRetrofit.create(AuthApiService::class.java)
    }

    @Test
    fun `social login preserves HTTP route, strict request JSON, and response schema`() =
        runTest {
            installExpectation(
                method = "POST",
                path = "/api/v1/auth/social/kakao",
                requestBody =
                    wireJson
                        .parseToJsonElement(
                            """{"accessToken":"provider-token","deviceId":"device-uuid","fcmToken":"fcm-token"}""",
                        ).jsonObject,
                responseBody =
                    """
                    {
                      "ok": true,
                      "data": {
                        "accessToken": "access",
                        "refreshToken": "refresh",
                        "isNewUser": true,
                        "expiresIn": 3600
                      }
                    }
                    """.trimIndent(),
            )

            val result =
                authService.socialLogin(
                    provider = "kakao",
                    body =
                        SocialLoginRequestDto(
                            accessToken = "provider-token",
                            deviceId = "device-uuid",
                            fcmToken = "fcm-token",
                        ),
                )
            val data = requireNotNull(result.data)

            assertEquals(true, result.ok)
            assertEquals("access", data.accessToken)
            assertEquals("refresh", data.refreshToken)
            assertEquals(true, data.isNewUser)
            assertEquals(3600L, data.expiresIn)
            assertExactlyOneRecordedRequest("POST", "/api/v1/auth/social/kakao")
        }

    private fun installExpectation(
        method: String,
        path: String,
        requestBody: JsonElement? = null,
        requestHeaders: Map<String, String> = emptyMap(),
        requestQueryParameters: Map<String, String> = emptyMap(),
        responseBody: String,
    ) {
        val expectation =
            buildJsonObject {
                putJsonObject("httpRequest") {
                    put("method", method)
                    put("path", path)
                    if (requestHeaders.isNotEmpty()) {
                        putJsonObject("headers") {
                            requestHeaders.forEach { (name, value) ->
                                put(name, buildJsonArray { add(JsonPrimitive(value)) })
                            }
                        }
                    }
                    if (requestQueryParameters.isNotEmpty()) {
                        putJsonObject("queryStringParameters") {
                            requestQueryParameters.forEach { (name, value) ->
                                put(name, buildJsonArray { add(JsonPrimitive(value)) })
                            }
                        }
                    }
                    if (requestBody != null) {
                        putJsonObject("body") {
                            put("type", "JSON")
                            put("json", requestBody)
                            put("matchType", "STRICT")
                        }
                    }
                }
                putJsonObject("httpResponse") {
                    put("statusCode", 200)
                    putJsonObject("headers") {
                        put(
                            "Content-Type",
                            buildJsonArray { add(JsonPrimitive("application/json")) },
                        )
                    }
                    put("body", responseBody)
                }
            }

        controlPut("/mockserver/expectation", expectation.toString())
    }

    private fun assertExactlyOneRecordedRequest(
        method: String,
        path: String,
    ) {
        val recorded = recordedRequests(method, path)

        assertEquals("$method $path must cross the socket exactly once", 1, recorded.size)
    }

    private fun recordedRequests(
        method: String,
        path: String,
    ): JsonArray {
        val matcher =
            buildJsonObject {
                put("method", method)
                put("path", path)
            }

        return wireJson
            .parseToJsonElement(
                controlPut("/mockserver/retrieve?type=REQUESTS", matcher.toString()),
            ).jsonArray
    }

    private fun controlPut(
        path: String,
        payload: String = "",
    ): String {
        val request =
            Request
                .Builder()
                .url("${mockServer.endpoint}$path")
                .put(payload.toRequestBody(JSON_MEDIA_TYPE))
                .build()

        return controlClient.newCall(request).execute().use { response ->
            val responseBody = response.body.string()
            check(response.isSuccessful) {
                "MockServer control PUT $path failed: ${response.code} $responseBody"
            }
            responseBody
        }
    }

    companion object {
        private const val ENABLE_ENV = "RUN_API_CONTRACT_SMOKE"
        private const val MOCKSERVER_VERSION = "7.6.0"
        private val JSON_MEDIA_TYPE = "application/json".toMediaType()
        private val controlClient = OkHttpClient()
        private val wireJson =
            Json {
                ignoreUnknownKeys = true
            }

        private lateinit var mockServer: MockServerContainer

        @BeforeClass
        @JvmStatic
        fun startContainer() {
            assumeTrue(
                "$ENABLE_ENV=true 인 전용 workflow 에서만 Docker 계약 검증을 실행한다",
                System.getenv(ENABLE_ENV) == "true",
            )
            check(DockerClientFactory.instance().isDockerAvailable) {
                "Docker runtime is required when $ENABLE_ENV=true"
            }

            mockServer =
                MockServerContainer(
                    DockerImageName.parse("mockserver/mockserver:mockserver-$MOCKSERVER_VERSION"),
                )
            mockServer.start()
        }

        @AfterClass
        @JvmStatic
        fun stopContainer() {
            if (::mockServer.isInitialized) {
                mockServer.stop()
            }
            controlClient.dispatcher.executorService.shutdown()
            controlClient.connectionPool.evictAll()
        }
    }
}
