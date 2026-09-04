package com.careercompass.feature.feed.presentation.postingraw

import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.ComposeContentTestRule
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
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
class PostingRawScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun contract_rejectsBlankStringsButAllowsNullUrl() {
        assertThrows(IllegalArgumentException::class.java) { sampleState().copy(title = " ") }
        assertThrows(IllegalArgumentException::class.java) { sampleState().copy(sourceLabel = " ") }
        assertThrows(IllegalArgumentException::class.java) { sampleState().copy(rawContent = " ") }
        assertThrows(IllegalArgumentException::class.java) { sampleState().copy(originalUrl = " ") }
        assertEquals(null, sampleState().copy(originalUrl = null).originalUrl)
    }

    @Test
    fun withOriginalUrl_showsOpenLinkButtonAndEmitsIntents() {
        val events = mutableListOf<PostingRawEvent>()
        composeRule.setRawContent(state = sampleState(), onEvent = events::add)

        composeRule.onNodeWithText(SAMPLE_TITLE).assertIsDisplayed()
        composeRule.onNodeWithText("careers.kakao.com").assertIsDisplayed()
        composeRule.onNodeWithText(SAMPLE_CONTENT).assertIsDisplayed()
        composeRule.onNode(hasText("원본 링크 열기") and hasClickAction()).assertIsDisplayed().performClick()
        composeRule.onNodeWithContentDescription("뒤로가기").performClick()

        composeRule.runOnIdle {
            assertEquals(
                listOf(PostingRawEvent.OpenOriginalClicked, PostingRawEvent.BackClicked),
                events,
            )
        }
    }

    @Test
    fun withoutOriginalUrl_hidesOpenLinkButton() {
        composeRule.setRawContent(state = sampleState().copy(originalUrl = null))

        composeRule.onNodeWithText(SAMPLE_CONTENT).assertIsDisplayed()
        composeRule.onAllNodesWithText("원본 링크 열기").assertCountEquals(0)
    }
}

private fun ComposeContentTestRule.setRawContent(
    state: PostingRawUiState,
    onEvent: (PostingRawEvent) -> Unit = {},
) {
    setContent {
        CareerCompassTheme {
            PostingRawScreen(state = state, onEvent = onEvent)
        }
    }
}

private fun sampleState(): PostingRawUiState =
    PostingRawUiState(
        title = SAMPLE_TITLE,
        sourceLabel = "careers.kakao.com",
        originalUrl = "https://careers.kakao.com/jobs/1",
        rawContent = SAMPLE_CONTENT,
    )

private const val SAMPLE_TITLE = "2026 카카오 SW 인턴십 (백엔드) 모집"
private const val SAMPLE_CONTENT = "[모집 분야]\nServer (Java/Kotlin) 백엔드 개발 인턴십"
