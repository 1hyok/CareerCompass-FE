package com.cambridge.feature.feed.presentation.feedfilter

import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.hasStateDescription
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.ComposeContentTestRule
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
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
class FeedSortMenuContentTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun options_formRadioGroupWithSelectedState() {
        composeRule.setSortContent(state = FeedSortMenuUiState(selected = FeedSortOption.ScoreDesc))

        composeRule
            .onNode(SemanticsMatcher.keyIsDefined(SemanticsProperties.SelectableGroup))
            .assertExists()
        composeRule
            .onNode(hasText("적합도 높은순") and hasStateDescription("선택됨"))
            .assertIsSelected()
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.RadioButton))
        composeRule
            .onNode(hasText("수집 최신순") and hasStateDescription("선택 안 됨"))
            .assertIsNotSelected()
        composeRule.onNodeWithText("마감 임박순").assertIsNotSelected()
    }

    @Test
    fun selectingOptionAndClosing_emitSeparateIntents() {
        val events = mutableListOf<FeedSortMenuEvent>()
        composeRule.setSortContent(
            state = FeedSortMenuUiState(selected = FeedSortOption.CollectedDesc),
            onEvent = events::add,
        )

        composeRule.onNodeWithText("마감 임박순").performClick()
        composeRule.onNodeWithContentDescription("닫기").performClick()

        composeRule.runOnIdle {
            assertEquals(
                listOf(
                    FeedSortMenuEvent.SortSelected(FeedSortOption.DueAsc),
                    FeedSortMenuEvent.DismissClicked,
                ),
                events,
            )
        }
    }
}

private fun ComposeContentTestRule.setSortContent(
    state: FeedSortMenuUiState,
    onEvent: (FeedSortMenuEvent) -> Unit = {},
) {
    setContent {
        CareerCompassTheme {
            FeedSortMenuContent(state = state, onEvent = onEvent)
        }
    }
}
