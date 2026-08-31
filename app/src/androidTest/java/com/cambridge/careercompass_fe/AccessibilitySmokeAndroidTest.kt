package com.cambridge.careercompass_fe

import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.accessibility.enableAccessibilityChecks
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.tryPerformAccessibilityChecks
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SdkSuppress
import com.cambridge.careercompass_fe.test.FailureArtifactRule
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** API 34 Accessibility Test Framework로 현재 최소 시작 화면의 실제 semantics를 검사한다. */
@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
@SdkSuppress(minSdkVersion = 34)
@OptIn(ExperimentalTestApi::class)
class AccessibilitySmokeAndroidTest {
    @get:Rule(order = 0)
    val hiltRule = HiltAndroidRule(this)

    @get:Rule(order = 1)
    val composeRule = createAndroidComposeRule<MainActivity>()

    @get:Rule(order = 2)
    val failureArtifactRule =
        FailureArtifactRule {
            composeRule.onRoot(useUnmergedTree = true).captureToImage().asAndroidBitmap()
        }

    @Before
    fun enableChecks() {
        hiltRule.inject()
        composeRule.enableAccessibilityChecks()
    }

    @Test
    fun welcomeAndLogin_haveNoAutomatedAccessibilityErrors() {
        composeRule
            .onNodeWithTag("careercompass_app_start", useUnmergedTree = true)
            .assertIsDisplayed()
        composeRule.onRoot().tryPerformAccessibilityChecks()
    }
}
