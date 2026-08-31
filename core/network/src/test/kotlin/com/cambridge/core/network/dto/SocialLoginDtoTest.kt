package com.cambridge.core.network.dto

import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Test

class SocialLoginDtoTest {
    @Test
    fun `request rejects blank required values and blank optional FCM token`() {
        val invalidCases =
            listOf(
                InvalidRequestCase("empty accessToken", "accessToken must not be blank") {
                    validRequest().copy(accessToken = "")
                },
                InvalidRequestCase("whitespace accessToken", "accessToken must not be blank") {
                    validRequest().copy(accessToken = " \t\n")
                },
                InvalidRequestCase("empty deviceId", "deviceId must not be blank") {
                    validRequest().copy(deviceId = "")
                },
                InvalidRequestCase("whitespace deviceId", "deviceId must not be blank") {
                    validRequest().copy(deviceId = " \t\n")
                },
                InvalidRequestCase("empty fcmToken", "fcmToken must be null or non-blank") {
                    validRequest().copy(fcmToken = "")
                },
                InvalidRequestCase("whitespace fcmToken", "fcmToken must be null or non-blank") {
                    validRequest().copy(fcmToken = " \t\n")
                },
            )

        invalidCases.forEach { invalidCase ->
            val error =
                assertThrows(IllegalArgumentException::class.java) {
                    invalidCase.create()
                }

            assertEquals(invalidCase.description, invalidCase.expectedMessage, error.message)
        }
    }

    @Test
    fun `response rejects blank tokens and non-positive expiry`() {
        val invalidCases =
            listOf(
                InvalidResponseCase("empty accessToken", "accessToken must not be blank") {
                    validResponse().copy(accessToken = "")
                },
                InvalidResponseCase("whitespace accessToken", "accessToken must not be blank") {
                    validResponse().copy(accessToken = " \t\n")
                },
                InvalidResponseCase("empty refreshToken", "refreshToken must not be blank") {
                    validResponse().copy(refreshToken = "")
                },
                InvalidResponseCase("whitespace refreshToken", "refreshToken must not be blank") {
                    validResponse().copy(refreshToken = " \t\n")
                },
                InvalidResponseCase("zero expiresIn", "expiresIn must be greater than zero") {
                    validResponse().copy(expiresIn = 0)
                },
                InvalidResponseCase("negative expiresIn", "expiresIn must be greater than zero") {
                    validResponse().copy(expiresIn = -1)
                },
            )

        invalidCases.forEach { invalidCase ->
            val error =
                assertThrows(IllegalArgumentException::class.java) {
                    invalidCase.create()
                }

            assertEquals(invalidCase.description, invalidCase.expectedMessage, error.message)
        }
    }

    @Test
    fun `request toString redacts provider token and device identifiers`() {
        val request = validRequest()

        val rendered = request.toString()

        listOf(REQUEST_ACCESS_TOKEN, DEVICE_ID, FCM_TOKEN).forEach { sensitiveValue ->
            assertFalse("toString leaked $sensitiveValue", rendered.contains(sensitiveValue))
        }
        assertEquals(
            "SocialLoginRequestDto(" +
                "accessToken=<redacted>, deviceId=<redacted>, fcmToken=<redacted>)",
            rendered,
        )
        assertEquals(
            "SocialLoginRequestDto(" +
                "accessToken=<redacted>, deviceId=<redacted>, fcmToken=null)",
            request.copy(fcmToken = null).toString(),
        )
    }

    @Test
    fun `response toString redacts access and refresh tokens`() {
        val response = validResponse()

        val rendered = response.toString()

        listOf(RESPONSE_ACCESS_TOKEN, REFRESH_TOKEN).forEach { sensitiveValue ->
            assertFalse("toString leaked $sensitiveValue", rendered.contains(sensitiveValue))
        }
        assertEquals(
            "SocialLoginDto(" +
                "accessToken=<redacted>, refreshToken=<redacted>, isNewUser=false, expiresIn=1)",
            rendered,
        )
    }

    @Test
    fun `serialization preserves social login wire property names`() {
        val requestJson = Json.encodeToString(SocialLoginRequestDto.serializer(), validRequest())
        val responseJson = Json.encodeToString(SocialLoginDto.serializer(), validResponse())

        assertEquals(
            """{"accessToken":"$REQUEST_ACCESS_TOKEN","deviceId":"$DEVICE_ID","fcmToken":"$FCM_TOKEN"}""",
            requestJson,
        )
        assertEquals(
            """{"accessToken":"$RESPONSE_ACCESS_TOKEN","refreshToken":"$REFRESH_TOKEN","isNewUser":false,"expiresIn":1}""",
            responseJson,
        )
    }

    @Test
    fun `deserialization accepts null FCM token and positive expiry boundary`() {
        val request =
            Json.decodeFromString(
                SocialLoginRequestDto.serializer(),
                """{"accessToken":"$REQUEST_ACCESS_TOKEN","deviceId":"$DEVICE_ID"}""",
            )
        val response =
            Json.decodeFromString(
                SocialLoginDto.serializer(),
                """{"accessToken":"$RESPONSE_ACCESS_TOKEN","refreshToken":"$REFRESH_TOKEN","isNewUser":false,"expiresIn":1}""",
            )

        assertNull(request.fcmToken)
        assertEquals(1L, response.expiresIn)
    }

    @Test
    fun `response deserialization enforces token and expiry invariants`() {
        val invalidCases =
            listOf(
                InvalidResponseJsonCase(
                    description = "blank accessToken",
                    expectedMessage = "accessToken must not be blank",
                    json =
                        """{"accessToken":" ","refreshToken":"$REFRESH_TOKEN","isNewUser":false,"expiresIn":1}""",
                ),
                InvalidResponseJsonCase(
                    description = "blank refreshToken",
                    expectedMessage = "refreshToken must not be blank",
                    json =
                        """{"accessToken":"$RESPONSE_ACCESS_TOKEN","refreshToken":" ","isNewUser":false,"expiresIn":1}""",
                ),
                InvalidResponseJsonCase(
                    description = "zero expiresIn",
                    expectedMessage = "expiresIn must be greater than zero",
                    json =
                        """{"accessToken":"$RESPONSE_ACCESS_TOKEN","refreshToken":"$REFRESH_TOKEN","isNewUser":false,"expiresIn":0}""",
                ),
            )

        invalidCases.forEach { invalidCase ->
            val error =
                assertThrows(IllegalArgumentException::class.java) {
                    Json.decodeFromString(SocialLoginDto.serializer(), invalidCase.json)
                }

            assertEquals(invalidCase.description, invalidCase.expectedMessage, error.message)
        }
    }

    private fun validRequest() =
        SocialLoginRequestDto(
            accessToken = REQUEST_ACCESS_TOKEN,
            deviceId = DEVICE_ID,
            fcmToken = FCM_TOKEN,
        )

    private fun validResponse() =
        SocialLoginDto(
            accessToken = RESPONSE_ACCESS_TOKEN,
            refreshToken = REFRESH_TOKEN,
            isNewUser = false,
            expiresIn = 1,
        )

    private data class InvalidRequestCase(
        val description: String,
        val expectedMessage: String,
        val create: () -> SocialLoginRequestDto,
    )

    private data class InvalidResponseCase(
        val description: String,
        val expectedMessage: String,
        val create: () -> SocialLoginDto,
    )

    private data class InvalidResponseJsonCase(
        val description: String,
        val expectedMessage: String,
        val json: String,
    )

    private companion object {
        const val REQUEST_ACCESS_TOKEN = "provider-access-token"
        const val DEVICE_ID = "device-identifier"
        const val FCM_TOKEN = "fcm-device-token"
        const val RESPONSE_ACCESS_TOKEN = "response-access-token"
        const val REFRESH_TOKEN = "response-refresh-token"
    }
}
