package com.cambridge.feature.onboarding.presentation.basicinfo

import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.unit.dp
import com.cambridge.core.ui.theme.CareerCompassTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], qualifiers = "w360dp-h800dp")
public class SchoolPickerSheetTest {
    @get:Rule
    public val composeRule = createComposeRule()

    @Test
    public fun results_renderAsButtonRowsWithAccessibleNames() {
        setSheet(SchoolPickerState(results = listOf("건국대학교", "고려대학교")))

        composeRule.onNodeWithText("학교 선택").assertIsDisplayed()
        composeRule
            .onNodeWithContentDescription("건국대학교 선택")
            .assertIsDisplayed()
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Button))
            .assertHeightIsAtLeast(48.dp)
        composeRule.onNodeWithContentDescription("고려대학교 선택").assertIsDisplayed()
    }

    @Test
    public fun selectingRow_emitsSchoolSelected() {
        val events = mutableListOf<SchoolPickerEvent>()
        setSheet(SchoolPickerState(results = listOf("건국대학교")), onEvent = events::add)

        composeRule.onNodeWithContentDescription("건국대학교 선택").performClick()

        composeRule.runOnIdle {
            assertEquals(listOf(SchoolPickerEvent.SchoolSelected("건국대학교")), events)
        }
    }

    @Test
    public fun typingQuery_emitsQueryChanged() {
        val events = mutableListOf<SchoolPickerEvent>()
        setSheet(SchoolPickerState(results = listOf("건국대학교")), onEvent = events::add)

        composeRule.onNodeWithContentDescription("학교 검색").performTextInput("건국")

        composeRule.runOnIdle {
            assertEquals(listOf(SchoolPickerEvent.QueryChanged("건국")), events)
        }
    }

    @Test
    public fun emptyResults_showEmptyMessage() {
        setSheet(SchoolPickerState(query = "없는대", results = emptyList()))

        composeRule.onNodeWithText("검색 결과가 없어요").assertIsDisplayed()
    }

    private fun setSheet(
        state: SchoolPickerState,
        onEvent: (SchoolPickerEvent) -> Unit = {},
    ) {
        composeRule.setContent {
            CareerCompassTheme {
                SchoolPickerSheet(state = state, onEvent = onEvent)
            }
        }
    }
}
