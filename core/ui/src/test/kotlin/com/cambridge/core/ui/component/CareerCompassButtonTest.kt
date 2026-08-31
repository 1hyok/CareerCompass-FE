package com.cambridge.core.ui.component

import androidx.compose.foundation.layout.Column
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertHeightIsEqualTo
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
import com.cambridge.core.ui.theme.CareerCompassTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
public class CareerCompassButtonTest {
    @get:Rule
    public val composeRule = createComposeRule()

    @Test
    public fun enabledButton_exposesButtonRoleAndInvokesClick() {
        var clickCount = 0

        composeRule.setContent {
            CareerCompassTheme {
                CareerCompassButton(
                    text = "계속",
                    onClick = { clickCount += 1 },
                    modifier = Modifier.testTag(BUTTON_TAG),
                    size = CareerCompassButtonSize.Medium,
                )
            }
        }

        composeRule
            .onNodeWithTag(BUTTON_TAG)
            .assertIsEnabled()
            .assert(
                SemanticsMatcher.expectValue(
                    SemanticsProperties.Role,
                    Role.Button,
                ),
            ).assertHeightIsEqualTo(44.dp)
            .assertTouchHeightIsAtLeast(48.dp)
            .performClick()

        assertEquals(1, clickCount)
    }

    @Test
    public fun disabledButton_isDisabledAndDoesNotInvokeClick() {
        var clickCount = 0

        composeRule.setContent {
            CareerCompassTheme {
                CareerCompassButton(
                    text = "삭제",
                    onClick = { clickCount += 1 },
                    modifier = Modifier.testTag(BUTTON_TAG),
                    variant = CareerCompassButtonVariant.Danger,
                    enabled = false,
                )
            }
        }

        composeRule
            .onNodeWithTag(BUTTON_TAG)
            .assertIsNotEnabled()
            .assert(
                SemanticsMatcher.expectValue(
                    SemanticsProperties.Role,
                    Role.Button,
                ),
            ).performClick()

        assertEquals(0, clickCount)
    }

    @Test
    public fun sizeVariants_keepFigmaHeightsAndSupportExplicitDescription() {
        composeRule.setContent {
            CareerCompassTheme {
                Column {
                    CareerCompassButton(
                        text = "작게",
                        onClick = {},
                        modifier = Modifier.testTag(SMALL_BUTTON_TAG),
                        size = CareerCompassButtonSize.Small,
                    )
                    CareerCompassButton(
                        text = "크게",
                        onClick = {},
                        modifier = Modifier.testTag(LARGE_BUTTON_TAG),
                        size = CareerCompassButtonSize.Large,
                        contentDescription = "지원서 제출",
                    )
                }
            }
        }

        composeRule
            .onNodeWithTag(SMALL_BUTTON_TAG)
            .assertHeightIsEqualTo(36.dp)
            .assertTouchHeightIsAtLeast(48.dp)
        composeRule
            .onNodeWithTag(LARGE_BUTTON_TAG)
            .assertHeightIsEqualTo(52.dp)
            .assertTouchHeightIsAtLeast(48.dp)
            .assert(
                SemanticsMatcher.expectValue(
                    SemanticsProperties.ContentDescription,
                    listOf("지원서 제출"),
                ),
            )
    }

    private companion object {
        const val BUTTON_TAG = "career_compass_button"
        const val SMALL_BUTTON_TAG = "career_compass_small_button"
        const val LARGE_BUTTON_TAG = "career_compass_large_button"
    }
}
