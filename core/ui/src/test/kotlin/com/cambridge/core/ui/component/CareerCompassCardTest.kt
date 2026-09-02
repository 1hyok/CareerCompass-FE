package com.cambridge.core.ui.component

import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.cambridge.core.ui.theme.CareerCompassTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
public class CareerCompassCardTest {
    @get:Rule
    public val composeRule = createComposeRule()

    @Test
    public fun card_rendersContent() {
        composeRule.setContent {
            CareerCompassTheme {
                CareerCompassCard {
                    Text("지원 현황")
                }
            }
        }

        composeRule
            .onNodeWithText("지원 현황")
            .assertTextEquals("지원 현황")
    }

    @Test
    public fun nullOnClick_doesNotExposeClickSemantics() {
        composeRule.setContent {
            CareerCompassTheme {
                CareerCompassCard(
                    modifier = Modifier.testTag(CARD_TAG),
                    onClick = null,
                ) {
                    Text("읽기 전용")
                }
            }
        }

        composeRule
            .onNodeWithTag(CARD_TAG)
            .assert(!hasClickAction())
    }

    @Test
    public fun clickableCard_exposesButtonRoleAndInvokesCallback() {
        var clickCount = 0

        composeRule.setContent {
            CareerCompassTheme {
                CareerCompassCard(
                    modifier = Modifier.testTag(CARD_TAG),
                    onClick = { clickCount += 1 },
                ) {
                    Text("상세 보기")
                }
            }
        }

        composeRule
            .onNodeWithTag(CARD_TAG)
            .assertHasClickAction()
            .assert(
                SemanticsMatcher.expectValue(
                    SemanticsProperties.Role,
                    Role.Button,
                ),
            ).performClick()

        composeRule.runOnIdle {
            assertEquals(1, clickCount)
        }
    }

    private companion object {
        const val CARD_TAG = "career_compass_card"
    }
}
