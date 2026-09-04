package com.careercompass.feature.onboarding.presentation.login.util

import android.app.Activity
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class KakaoLoginHelperTest {
    @Test
    fun `SDK 가 초기화되지 않았으면 예외 대신 실패 Result 를 돌려준다`() =
        runTest {
            val activity = Robolectric.buildActivity(Activity::class.java).setup().get()

            val result = KakaoLoginHelper.requestKakaoAccessToken(activity)

            assertTrue("초기화되지 않은 SDK 호출은 실패로 끝나야 한다", result.isFailure)
        }
}
