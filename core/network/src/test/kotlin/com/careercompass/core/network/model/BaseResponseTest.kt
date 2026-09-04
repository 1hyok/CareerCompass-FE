package com.careercompass.core.network.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Test

class BaseResponseTest {
    @Test
    fun `requireData returns payload from successful envelope`() {
        val response =
            BaseResponse(
                ok = true,
                data = "payload",
            )

        assertEquals("payload", response.requireData())
    }

    @Test
    fun `requireData maps failed envelope error fields to ApiException`() {
        val response =
            BaseResponse<String>(
                ok = false,
                error =
                    ApiErrorDto(
                        code = "INVALID_TOKEN",
                        message = "토큰이 유효하지 않습니다.",
                        field = "accessToken",
                    ),
            )

        val error =
            assertThrows(ApiException::class.java) {
                response.requireData()
            }

        assertEquals("INVALID_TOKEN", error.code)
        assertEquals("토큰이 유효하지 않습니다.", error.serverMessage)
        assertEquals("토큰이 유효하지 않습니다.", error.message)
    }

    @Test
    fun `requireData uses unknown error fallback when failed envelope has no error object`() {
        val response = BaseResponse<String>(ok = false)

        val error =
            assertThrows(ApiException::class.java) {
                response.requireData()
            }

        assertEquals("UNKNOWN", error.code)
        assertNull(error.serverMessage)
        assertEquals("알 수 없는 서버 에러가 발생했습니다.", error.message)
    }

    @Test
    fun `requireData uses fallback when server error message is missing`() {
        val response =
            BaseResponse<String>(
                ok = false,
                error = ApiErrorDto(code = "SERVER_ERROR"),
            )

        val error =
            assertThrows(ApiException::class.java) {
                response.requireData()
            }

        assertEquals("SERVER_ERROR", error.code)
        assertNull(error.serverMessage)
        assertEquals("알 수 없는 서버 에러가 발생했습니다.", error.message)
    }

    @Test
    fun `requireData uses fallback while preserving empty server error message`() {
        val response =
            BaseResponse<String>(
                ok = false,
                error = ApiErrorDto(code = "SERVER_ERROR", message = ""),
            )

        val error =
            assertThrows(ApiException::class.java) {
                response.requireData()
            }

        assertEquals("SERVER_ERROR", error.code)
        assertEquals("", error.serverMessage)
        assertEquals("알 수 없는 서버 에러가 발생했습니다.", error.message)
    }

    @Test
    fun `requireData rejects successful envelope with empty data`() {
        val response = BaseResponse<String>(ok = true)

        val error =
            assertThrows(ApiException::class.java) {
                response.requireData()
            }

        assertEquals("EMPTY_DATA", error.code)
        assertNull(error.serverMessage)
        assertEquals("성공했으나 데이터가 비어있습니다.", error.message)
    }

    @Test
    fun `requireData rejects failed envelope before returning present data`() {
        val response =
            BaseResponse(
                ok = false,
                data = "must-not-return",
                error = ApiErrorDto(code = "REQUEST_FAILED", message = "요청 실패"),
            )

        val error =
            assertThrows(ApiException::class.java) {
                response.requireData()
            }

        assertEquals("REQUEST_FAILED", error.code)
        assertEquals("요청 실패", error.message)
    }

    @Test
    fun `requireOk accepts successful envelope without data`() {
        val response = BaseResponse<Unit>(ok = true)

        assertEquals(Unit, response.requireOk())
    }

    @Test
    fun `requireOk maps failed envelope error fields to ApiException`() {
        val response =
            BaseResponse<Unit>(
                ok = false,
                error = ApiErrorDto(code = "FORBIDDEN", message = "권한이 없습니다."),
            )

        val error =
            assertThrows(ApiException::class.java) {
                response.requireOk()
            }

        assertEquals("FORBIDDEN", error.code)
        assertEquals("권한이 없습니다.", error.serverMessage)
        assertEquals("권한이 없습니다.", error.message)
    }

    @Test
    fun `requireOk uses request fallback when failed envelope has no error object`() {
        val response = BaseResponse<Unit>(ok = false)

        val error =
            assertThrows(ApiException::class.java) {
                response.requireOk()
            }

        assertEquals("UNKNOWN", error.code)
        assertNull(error.serverMessage)
        assertEquals("요청에 실패했습니다.", error.message)
    }

    @Test
    fun `requireOk uses request fallback when server error message is missing`() {
        val response =
            BaseResponse<Unit>(
                ok = false,
                error = ApiErrorDto(code = "SERVER_ERROR"),
            )

        val error =
            assertThrows(ApiException::class.java) {
                response.requireOk()
            }

        assertEquals("SERVER_ERROR", error.code)
        assertNull(error.serverMessage)
        assertEquals("요청에 실패했습니다.", error.message)
    }

    @Test
    fun `requireOk uses request fallback while preserving blank server error message`() {
        val response =
            BaseResponse<Unit>(
                ok = false,
                error = ApiErrorDto(code = "SERVER_ERROR", message = " \t"),
            )

        val error =
            assertThrows(ApiException::class.java) {
                response.requireOk()
            }

        assertEquals("SERVER_ERROR", error.code)
        assertEquals(" \t", error.serverMessage)
        assertEquals("요청에 실패했습니다.", error.message)
    }
}
