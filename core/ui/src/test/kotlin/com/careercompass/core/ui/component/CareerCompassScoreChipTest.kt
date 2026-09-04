package com.careercompass.core.ui.component

import androidx.compose.foundation.layout.Column
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import com.careercompass.core.ui.theme.CareerCompassTheme
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

    /**
     * 단계가 색 말고 개수로도 나가는지 본다. 세 단계가 서로 다른 개수여야 「색을 못 봐도 갈린다」가 성립한다 —
     * 라이트에서 세 컨테이너 색은 서로 1.01~1.04:1 이라 색은 단서가 되지 못한다(이슈 #205).
     */
    @Test
    fun everyLevel_mapsToADistinctFilledStepCount() {
        val steps = CareerCompassScoreLevel.entries.associateWith { it.filledSteps() }

        assertEquals(3, steps.getValue(CareerCompassScoreLevel.High))
        assertEquals(2, steps.getValue(CareerCompassScoreLevel.Mid))
        assertEquals(1, steps.getValue(CareerCompassScoreLevel.Low))
        assertEquals(
            "단계마다 눈금 개수가 달라야 색 없이 갈린다",
            CareerCompassScoreLevel.entries.size,
            steps.values.toSet().size,
        )
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
