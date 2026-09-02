package com.cambridge.core.network.calladapter

import com.cambridge.core.network.model.ApiException
import com.cambridge.core.network.model.BaseResponse
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Test
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import retrofit2.http.GET

class ApiErrorCallAdapterFactoryTest {
    private interface ProbeService {
        @GET("probe")
        suspend fun probe(): BaseResponse<String>
    }

    private fun service(
        status: Int,
        body: String,
    ): ProbeService {
        val json = Json { ignoreUnknownKeys = true }
        val client =
            OkHttpClient
                .Builder()
                .addInterceptor(
                    Interceptor { chain ->
                        Response
                            .Builder()
                            .request(chain.request())
                            .protocol(Protocol.HTTP_1_1)
                            .code(status)
                            .message(if (status == 200) "OK" else "Error")
                            .body(body.toResponseBody("application/json".toMediaType()))
                            .build()
                    },
                ).build()
        return Retrofit
            .Builder()
            .baseUrl("https://api.careercompass.invalid/api/v1/")
            .client(client)
            .addCallAdapterFactory(ApiErrorCallAdapterFactory(json))
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
            .create(ProbeService::class.java)
    }

    @Test
    fun `봉투 오류 본문을 ApiException 으로 옮긴다`() {
        val service =
            service(
                status = 404,
                body = """{"ok":false,"error":{"code":"POSTING_NOT_FOUND","message":"공고를 찾을 수 없습니다","field":null}}""",
            )

        val error = assertThrows(ApiException::class.java) { runBlocking { service.probe() } }

        assertEquals("POSTING_NOT_FOUND", error.code)
        assertEquals("공고를 찾을 수 없습니다", error.serverMessage)
        assertEquals(404, error.status)
        assertNull(error.field)
    }

    @Test
    fun `봉투가 없는 실패 본문은 HTTP 상태 코드로 감싼다`() {
        val service = service(status = 502, body = "<html>Bad Gateway</html>")

        val error = assertThrows(ApiException::class.java) { runBlocking { service.probe() } }

        assertEquals("HTTP_502", error.code)
        assertEquals(502, error.status)
        assertNull(error.serverMessage)
    }

    @Test
    fun `검증 실패의 field 를 보존한다`() {
        val service =
            service(
                status = 400,
                body = """{"ok":false,"error":{"code":"INVALID_INPUT","message":"학점 범위","field":"gpa"}}""",
            )

        val error = assertThrows(ApiException::class.java) { runBlocking { service.probe() } }

        assertEquals("INVALID_INPUT", error.code)
        assertEquals("gpa", error.field)
    }

    @Test
    fun `성공 응답은 그대로 통과한다`() {
        val service = service(status = 200, body = """{"ok":true,"data":"payload"}""")

        assertEquals("payload", runBlocking { service.probe() }.data)
    }
}
