package com.cambridge.core.ui.component

import androidx.compose.foundation.layout.Column
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import com.cambridge.core.ui.theme.CareerCompassTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class CareerCompassBadgeTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun everyTone_exposesItsLabelAsOneSemanticsNode() {
        val tones = CareerCompassBadgeTone.entries

        composeRule.setContent {
            CareerCompassTheme {
                Column {
                    tones.forEach { tone ->
                        CareerCompassBadge(
                            label = tone.name,
                            tone = tone,
                        )
                    }
                }
            }
        }

        tones.forEach { tone ->
            composeRule.onAllNodesWithText(tone.name).assertCountEquals(1)
            composeRule.onNodeWithText(tone.name).assertTextEquals(tone.name)
        }
    }
}
