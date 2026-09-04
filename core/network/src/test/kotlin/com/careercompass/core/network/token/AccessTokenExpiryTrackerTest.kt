package com.careercompass.core.network.token

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AccessTokenExpiryTrackerTest {
    private var now = 1_000_000L
    private val tracker = AccessTokenExpiryTracker { now }

    @Test
    fun `기록이 없으면 임박이 아니다`() {
        assertFalse(tracker.isExpiringSoon())
    }

    @Test
    fun `잔여 수명이 60초 미만이면 임박이다`() {
        tracker.record(expiresInSeconds = 120)

        assertFalse(tracker.isExpiringSoon())
        now += 61_000
        assertTrue(tracker.isExpiringSoon())
    }

    @Test
    fun `clear 하면 다음 기록까지 임박 판정을 쉰다`() {
        tracker.record(expiresInSeconds = 1)

        tracker.clear()

        assertFalse(tracker.isExpiringSoon())
    }
}
