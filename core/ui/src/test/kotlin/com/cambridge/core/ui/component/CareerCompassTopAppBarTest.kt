package com.cambridge.core.ui.component

import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertHeightIsEqualTo
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.assertWidthIsEqualTo
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
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
public class CareerCompassTopAppBarTest {
    @get:Rule
    public val composeRule = createComposeRule()

    @Test
    public fun topAppBar_rendersTitleSubtitleAndActionsAtFixedHeight() {
        composeRule.setContent {
            CareerCompassTheme {
                CareerCompassTopAppBar(
                    title = "지원서 분석",
                    onBackClick = {},
                    modifier = Modifier.testTag(TOP_APP_BAR_TAG),
                    subtitle = "최근 업데이트",
                    actions = { Text("저장") },
                )
            }
        }

        composeRule
            .onNodeWithTag(TOP_APP_BAR_TAG)
            .assertHeightIsEqualTo(56.dp)
        composeRule
            .onNodeWithText("지원서 분석")
            .assertTextEquals("지원서 분석")
        composeRule
            .onNodeWithText("최근 업데이트")
            .assertTextEquals("최근 업데이트")
        composeRule
            .onNodeWithText("저장")
            .assertTextEquals("저장")
    }

    @Test
    public fun nullBackClick_hidesBackControl() {
        setTopAppBar(onBackClick = null)

        composeRule
            .onNodeWithContentDescription("뒤로가기")
            .assertDoesNotExist()
    }

    @Test
    public fun backControl_hasButtonSemanticsAndInvokesCallback() {
        var clickCount = 0
        setTopAppBar(onBackClick = { clickCount += 1 })

        composeRule
            .onNodeWithContentDescription("뒤로가기")
            .assert(
                SemanticsMatcher.expectValue(
                    SemanticsProperties.Role,
                    Role.Button,
                ),
            ).assertWidthIsEqualTo(48.dp)
            .assertHeightIsEqualTo(48.dp)
            .assertTouchHeightIsAtLeast(48.dp)
            .performClick()

        composeRule.runOnIdle {
            assertEquals(1, clickCount)
        }
    }

    @Test
    public fun blankTitle_isRejected() {
        val error =
            assertThrows(IllegalArgumentException::class.java) {
                setTopAppBar(title = " \t\n")
            }

        assertEquals("title must not be blank", error.message)
    }

    @Test
    public fun blankSubtitle_isRejected() {
        val error =
            assertThrows(IllegalArgumentException::class.java) {
                setTopAppBar(subtitle = " \t\n")
            }

        assertEquals("subtitle must be null or non-blank", error.message)
    }

    private fun setTopAppBar(
        title: String = "지원서 분석",
        onBackClick: (() -> Unit)? = {},
        subtitle: String? = null,
    ) {
        composeRule.setContent {
            CareerCompassTheme {
                CareerCompassTopAppBar(
                    title = title,
                    onBackClick = onBackClick,
                    subtitle = subtitle,
                )
            }
        }
    }

    private companion object {
        const val TOP_APP_BAR_TAG = "career_compass_top_app_bar"
    }
}
