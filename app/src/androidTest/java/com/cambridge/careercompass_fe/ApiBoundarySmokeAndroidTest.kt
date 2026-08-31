package com.cambridge.careercompass_fe

import android.os.Build
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onRoot
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.cambridge.careercompass_fe.test.FailureArtifactRule
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class ApiBoundarySmokeAndroidTest {
    @get:Rule(order = 0)
    val hiltRule = HiltAndroidRule(this)

    @get:Rule(order = 1)
    val composeRule = createAndroidComposeRule<MainActivity>()

    @get:Rule(order = 2)
    val failureArtifactRule =
        FailureArtifactRule {
            composeRule.onRoot(useUnmergedTree = true).captureToImage().asAndroidBitmap()
        }

    @Test
    fun appStartIsVisibleWithinSupportedApiBoundary() {
        assertTrue(
            "API boundary smoke must run on a supported API, but was ${Build.VERSION.SDK_INT}",
            Build.VERSION.SDK_INT in 26..36,
        )
        composeRule
            .onNodeWithTag("careercompass_app_start", useUnmergedTree = true)
            .assertIsDisplayed()
    }
}
