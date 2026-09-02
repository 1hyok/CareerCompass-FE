package com.cambridge.core.common.reporting

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.coroutines.cancellation.CancellationException

class ErrorReporterTest {
    private class RecordingReporter : ErrorReporter {
        val written = mutableListOf<Pair<Throwable, Map<String, String>>>()

        override fun writeFailure(
            throwable: Throwable,
            attributes: Map<String, String>,
        ) {
            written += throwable to attributes
        }
    }

    @Test
    fun `예외 문구를 버리고 타입과 스택트레이스만 남긴다`() {
        val reporter = RecordingReporter()
        val original = IllegalStateException("user@example.com 이 포함된 서버 원문", RuntimeException("cause 원문"))

        reporter.recordFailure(original, attributes = mapOf("stage" to "social_login"))

        val (written, attributes) = reporter.written.single()
        assertEquals(IllegalStateException::class.java.name, written.message)
        assertFalse(written.message.orEmpty().contains("user@example.com"))
        assertNull(written.cause)
        assertTrue(written.stackTrace.contentEquals(original.stackTrace))
        assertEquals("social_login", attributes["stage"])
        assertEquals(IllegalStateException::class.java.name, attributes[ERROR_REPORT_KEY_TYPE])
        assertEquals(RuntimeException::class.java.name, attributes[ERROR_REPORT_KEY_CAUSE_TYPE])
    }

    @Test
    fun `코루틴 취소는 기록하지 않는다`() {
        val reporter = RecordingReporter()

        reporter.recordFailure(CancellationException("scope cancelled"))

        assertTrue(reporter.written.isEmpty())
    }
}
