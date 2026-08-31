package com.cambridge.core.ui.component

import androidx.compose.foundation.layout.Column
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import com.cambridge.core.ui.theme.CareerCompassTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class CareerCompassScoreChipTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun everyLevel_exposesOneCombinedContentDescription() {
        val scores =
            mapOf(
                CareerCompassScoreLevel.High to 100,
                CareerCompassScoreLevel.Mid to 50,
                CareerCompassScoreLevel.Low to 0,
            )

        composeRule.setContent {
            CareerCompassTheme {
                Column {
                    scores.forEach { (level, score) ->
                        CareerCompassScoreChip(
                            label = level.name,
                            score = score,
                            level = level,
                        )
                    }
                }
            }
        }

        scores.forEach { (level, score) ->
            composeRule
                .onNodeWithContentDescription("${level.name} $score")
                .assertExists()
            composeRule.onAllNodesWithText(level.name).assertCountEquals(0)
            composeRule.onAllNodesWithText(score.toString()).assertCountEquals(0)
        }
    }

    @Test
    fun customContentDescription_replacesDefaultDescription() {
        composeRule.setContent {
            CareerCompassTheme {
                CareerCompassScoreChip(
                    label = "적합도",
                    score = 88,
                    level = CareerCompassScoreLevel.High,
                    contentDescription = "적합도 88점",
                )
            }
        }

        composeRule
            .onNodeWithContentDescription("적합도 88점")
            .assertExists()
        composeRule
            .onNodeWithContentDescription("적합도 88")
            .assertDoesNotExist()
    }

    @Test
    fun scoreBelowRange_isRejected() {
        val error =
            assertThrows(IllegalArgumentException::class.java) {
                composeRule.setContent {
                    CareerCompassTheme {
                        CareerCompassScoreChip(
                            label = "Score",
                            score = -1,
                            level = CareerCompassScoreLevel.Low,
                        )
                    }
                }
            }

        assertEquals("score must be between 0 and 100: -1", error.message)
    }

    @Test
    fun scoreAboveRange_isRejected() {
        val error =
            assertThrows(IllegalArgumentException::class.java) {
                composeRule.setContent {
                    CareerCompassTheme {
                        CareerCompassScoreChip(
                            label = "Score",
                            score = 101,
                            level = CareerCompassScoreLevel.High,
                        )
                    }
                }
            }

        assertEquals("score must be between 0 and 100: 101", error.message)
    }
}
