package com.cambridge.core.common.result

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.coroutines.cancellation.CancellationException

class RunCatchingCancellableTest {
    @Test
    fun `성공 값은 Result success 로 감싼다`() {
        assertEquals(Result.success(42), runCatchingCancellable { 42 })
    }

    @Test
    fun `일반 예외는 Result failure 로 흡수한다`() {
        val result = runCatchingCancellable<Int> { throw IllegalStateException("boom") }

        assertTrue(result.exceptionOrNull() is IllegalStateException)
    }

    @Test
    fun `CancellationException 은 삼키지 않고 다시 던진다`() {
        assertThrows(CancellationException::class.java) {
            runCatchingCancellable<Int> { throw CancellationException("cancelled") }
        }
    }
}
