package com.cambridge.core.ui.component

import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.click
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.unit.dp
import com.cambridge.core.ui.theme.CareerCompassTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class CareerCompassTagTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun selectedTag_exposesCheckboxRoleStateAndOneLabel() {
        var clicks = 0
        composeRule.setContent {
            CareerCompassTheme {
                CareerCompassTag(
                    label = "Selected tag",
                    selected = true,
                    onClick = { clicks += 1 },
                    stateDescription = "선택됨",
                )
            }
        }

        val tag = composeRule.onNodeWithText("Selected tag")
        tag
            .assertIsOn()
            .assertIsEnabled()
            .assertHasClickAction()
            .assertTouchHeightIsAtLeast(48.dp)
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Checkbox))
            .assert(
                SemanticsMatcher.expectValue(
                    SemanticsProperties.StateDescription,
                    "선택됨",
                ),
            ).performClick()

        composeRule.runOnIdle { assertEquals(1, clicks) }
        composeRule.onAllNodesWithText("Selected tag").assertCountEquals(1)
    }

    @Test
    fun unselectedTag_exposesOffState() {
        composeRule.setContent {
            CareerCompassTheme {
                CareerCompassTag(
                    label = "Available tag",
                    selected = false,
                    onClick = {},
                )
            }
        }

        composeRule
            .onNodeWithText("Available tag")
            .assertIsOff()
    }

    @Test
    fun disabledTag_rejectsPointerInputAndExposesDisabledState() {
        var clicks = 0
        composeRule.setContent {
            CareerCompassTheme {
                CareerCompassTag(
                    label = "Disabled tag",
                    selected = false,
                    enabled = false,
                    onClick = { clicks += 1 },
                )
            }
        }

        composeRule
            .onNodeWithText("Disabled tag")
            .assertIsOff()
            .assertIsNotEnabled()
            .performTouchInput { click() }

        composeRule.runOnIdle { assertEquals(0, clicks) }
    }
}
