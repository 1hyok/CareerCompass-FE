package com.careercompass.feature.onboarding.presentation.complete

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import com.careercompass.core.ui.theme.CareerCompassTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], qualifiers = "w360dp-h800dp")
public class OnboardingCompleteScreenTest {
    @get:Rule
    public val composeRule = createComposeRule()

    @Test
    public fun namedState_showsPersonalizedMessage() {
        setScreen(state = OnboardingCompleteUiState(userName = "일혁"))

        composeRule.onNodeWithText("프로필 완성!").assertIsDisplayed()
        composeRule.onNodeWithText("일혁님, 이제 딱 맞는 공고를 찾아드릴게요").assertIsDisplayed()
        composeRule.onNodeWithText("게시판을 등록하면 새 공고를 자동으로 모아 분석해요").assertIsDisplayed()
        composeRule.onAllNodesWithText("이제 딱 맞는 공고를 찾아드릴게요").assertCountEquals(0)
    }

    @Test
    public fun anonymousState_showsGenericMessage() {
        setScreen(state = OnboardingCompleteUiState())

        composeRule.onNodeWithText("이제 딱 맞는 공고를 찾아드릴게요").assertIsDisplayed()
        composeRule
            .onAllNodesWithText("님, 이제 딱 맞는 공고를 찾아드릴게요", substring = true)
            .assertCountEquals(0)
    }

    @Test
    public fun checkBadge_isDecorative() {
        setScreen(state = OnboardingCompleteUiState())

        composeRule.onAllNodesWithText("✓", useUnmergedTree = true).assertCountEquals(0)
    }

    @Test
    public fun actions_exposeButtonRoleAndEmitDistinctEvents() {
        val events = mutableListOf<OnboardingCompleteEvent>()
        setScreen(state = OnboardingCompleteUiState(userName = "일혁"), onEvent = events::add)

        viewFeedButton()
            .assertIsEnabled()
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Button))
            .assertHeightIsAtLeast(48.dp)
            .performClick()
        registerBoardButton()
            .assertIsEnabled()
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Button))
            .assertHeightIsAtLeast(48.dp)
            .performClick()

        composeRule.runOnIdle {
            assertEquals(
                listOf(
                    OnboardingCompleteEvent.ViewFeedClicked,
                    OnboardingCompleteEvent.RegisterBoardClicked,
                ),
                events,
            )
        }
    }

    @Test
    public fun largeFontScale_keepsPrimaryActionReachable() {
        setScreen(state = OnboardingCompleteUiState(userName = "일혁"), fontScale = 2f)

        viewFeedButton().performScrollTo().assertIsDisplayed()
        registerBoardButton().performScrollTo().assertIsDisplayed()
    }

    private fun viewFeedButton() = composeRule.onNode(hasText("공고 보러 가기") and hasClickAction())

    private fun registerBoardButton() = composeRule.onNode(hasText("게시판 먼저 등록하기") and hasClickAction())

    private fun setScreen(
        state: OnboardingCompleteUiState,
        onEvent: (OnboardingCompleteEvent) -> Unit = {},
        fontScale: Float = 1f,
    ) {
        composeRule.setContent {
            val currentDensity = LocalDensity.current
            CompositionLocalProvider(
                LocalDensity provides Density(currentDensity.density, fontScale),
            ) {
                CareerCompassTheme {
                    OnboardingCompleteScreen(state = state, onEvent = onEvent)
                }
            }
        }
    }
}
