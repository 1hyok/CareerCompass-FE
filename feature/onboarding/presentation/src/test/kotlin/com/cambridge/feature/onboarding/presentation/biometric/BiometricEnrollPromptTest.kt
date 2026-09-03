package com.cambridge.feature.onboarding.presentation.biometric

import androidx.compose.ui.test.junit4.v2.createComposeRule
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
public class BiometricEnrollPromptTest {
    @get:Rule
    public val composeRule = createComposeRule()

    /**
     * `BiometricPrompt` 는 [androidx.fragment.app.FragmentActivity] 를 요구한다 — 아닌 호스트(테스트·프리뷰)에서는
     * 켜는 길을 아예 열지 않는다. 호출부는 이 null 로 스위치를 잠근다.
     */
    @Test
    public fun withoutFragmentActivityHost_hasNoLauncher() {
        var launch: (() -> Unit)? = {}

        composeRule.setContent {
            launch = rememberBiometricEnrollPrompt(onResult = {})
        }
        composeRule.waitForIdle()

        assertNull(launch)
    }
}
