package com.cambridge.core.ui.component

import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.v2.createComposeRule
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
public class CareerCompassBottomBarTest {
    @get:Rule
    public val composeRule = createComposeRule()

    @Test
    public fun bottomBar_displaysEveryDestinationLabel() {
        setBottomBar()

        listOf("피드", "분석", "지원서", "마이").forEach { label ->
            composeRule
                .onNodeWithText(label)
                .assertTextEquals(label)
        }
    }

    @Test
    public fun selectedDestination_exposesSelectedTabSemantics() {
        setBottomBar(selectedTab = CareerCompassBottomTab.Analysis)

        composeRule
            .onNodeWithText("분석")
            .assertIsSelected()
            .assert(
                SemanticsMatcher.expectValue(
                    SemanticsProperties.Role,
                    Role.Tab,
                ),
            )
        composeRule
            .onNodeWithText("피드")
            .assertIsNotSelected()
    }

    @Test
    public fun destinationClick_invokesCallbackWithClickedTab() {
        var clickedTab: CareerCompassBottomTab? = null
        setBottomBar(onTabClick = { clickedTab = it })

        composeRule
            .onNodeWithText("지원서")
            .performClick()

        composeRule.runOnIdle {
            assertEquals(CareerCompassBottomTab.Applications, clickedTab)
        }
    }

    private fun setBottomBar(
        selectedTab: CareerCompassBottomTab = CareerCompassBottomTab.Feed,
        onTabClick: (CareerCompassBottomTab) -> Unit = {},
    ) {
        composeRule.setContent {
            CareerCompassTheme {
                CareerCompassBottomBar(
                    selectedTab = selectedTab,
                    onTabClick = onTabClick,
                )
            }
        }
    }
}
