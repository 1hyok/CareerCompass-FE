package com.careercompass.core.network.dto

import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class RequestDtoSerializationTest {
    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun `PATCH 요청은 null 필드를 직렬화하지 않는다`() {
        assertEquals(
            """{"name":"정일혁","gpa":3.87}""",
            json.encodeToString(UpdateProfileRequestDto.serializer(), UpdateProfileRequestDto(name = "정일혁", gpa = 3.87)),
        )
        assertEquals(
            """{"isActive":false}""",
            json.encodeToString(BoardUpdateRequestDto.serializer(), BoardUpdateRequestDto(isActive = false)),
        )
    }

    @Test
    fun `응답 DTO 는 필수 키가 빠지면 파싱을 실패시킨다`() {
        assertThrows(SerializationException::class.java) {
            json.decodeFromString(
                PostingDto.serializer(),
                """{"id":101,"title":"t","type":"recruit","board":{"id":3,"name":"b"},"collectedAt":"2026-05-18T07:00:00+09:00","isRead":false}""",
            )
        }
        assertThrows(SerializationException::class.java) {
            json.decodeFromString(
                BoardDto.serializer(),
                """{"id":1,"url":"u","name":"n","type":"scholarship","cycleHours":24,"isActive":true}""",
            )
        }
    }

    @Test
    fun `조건부 필드는 없어도 파싱된다`() {
        val posting =
            json.decodeFromString(
                PostingDto.serializer(),
                """{"id":101,"title":"t","type":"recruit","board":{"id":3,"name":"b"},"collectedAt":"2026-05-18T07:00:00+09:00","isRead":false,"isBookmarked":true}""",
            )
        val detection = json.decodeFromString(BoardDetectionDto.serializer(), """{"detectStatus":"blocked"}""")

        assertEquals(null, posting.score)
        assertEquals(null, posting.dueDate)
        assertEquals("blocked", detection.detectStatus)
        assertEquals(null, detection.preview)
    }

    @Test
    fun `토큰 DTO 의 toString 은 비밀값을 가린다`() {
        assertEquals("RefreshRequestDto(refreshToken=<redacted>)", RefreshRequestDto("secret").toString())
        assertEquals("RefreshDto(accessToken=<redacted>, refreshToken=<redacted>, expiresIn=3600)", RefreshDto("a", "r", 3600).toString())
        assertEquals("BiometricRegisterRequestDto(deviceId=<redacted>)", BiometricRegisterRequestDto("device").toString())
    }

    @Test
    fun `§7 추천은 배열 이유와 문자열 이유를 그대로 받는다`() {
        val feed =
            json.decodeFromString(
                ForYouFeedDto.serializer(),
                """{"topPick":{"postingId":101,"reason":["전공 적합"]},"byStrength":[{"postingId":102,"reason":"Kotlin 경험"}],"byGap":[]}""",
            )

        assertEquals(listOf("전공 적합"), feed.topPick?.reason)
        assertEquals("Kotlin 경험", feed.byStrength.single().reason)
        // 두 모양을 앱에서 한쪽으로 맞추면 반대쪽이 조용히 실패한다 — 문자열을 배열로 받으려 하면 여기서 막힌다.
        assertThrows(SerializationException::class.java) {
            json.decodeFromString(ForYouTopPickDto.serializer(), """{"postingId":101,"reason":"전공 적합"}""")
        }
    }

    @Test
    fun `§7 export 요청은 빈 구획을 만들지 못한다`() {
        assertThrows(IllegalArgumentException::class.java) {
            StrengthExportRequestDto(format = "markdown", sections = emptyList())
        }
    }
}
