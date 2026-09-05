package com.careercompass.feature.onboarding.presentation.biometric

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.assertHeightIsEqualTo
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertWidthIsAtLeast
import androidx.compose.ui.test.assertWidthIsEqualTo
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasStateDescription
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import com.careercompass.core.ui.theme.CareerCompassTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], qualifiers = "w360dp-h800dp")
public class BiometricLoginScreenTest {
    @get:Rule
    public val composeRule = createComposeRule()

    @Test
    public fun defaultState_showsGreetingAndPrompt() {
        setContent(state = sampleState)

        composeRule.onNodeWithContentDescription("CareerCompass").assertIsDisplayed()
        composeRule.onNodeWithText("일혁님, 안녕하세요").assertIsDisplayed()
        composeRule.onNodeWithText("지문으로 빠른 로그인").assertIsDisplayed()
        composeRule.onNodeWithText("등록된 지문을 사용해 바로 로그인해요").assertIsDisplayed()
        otherMethodButton().assertIsDisplayed().assertIsEnabled()
        composeRule.onAllNodesWithContentDescription("오류 안내 닫기").assertCountEquals(0)
    }

    /** 프로필이 아직 없으면 기본 호칭으로 인사한다 — 빈 이름으로 「님, 안녕하세요」가 되지 않는다. */
    @Test
    public fun missingUserName_fallsBackToDefaultGreeting() {
        setContent(state = sampleState.copy(userName = null))

        composeRule.onNodeWithText("회원님, 안녕하세요").assertIsDisplayed()
    }

    @Test
    public fun biometricButton_exposesRoleNameAndSize() {
        setContent(state = sampleState)

        biometricButton()
            .assertIsDisplayed()
            .assertIsEnabled()
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Button))
            .assertWidthIsEqualTo(96.dp)
            .assertHeightIsEqualTo(96.dp)
        composeRule.onAllNodesWithText("👆", useUnmergedTree = true).assertCountEquals(0)
    }

    @Test
    public fun controls_emitDistinctSignals() {
        val intents = mutableListOf<BiometricLoginIntent>()
        var biometricClicks = 0
        setContent(state = sampleState, onIntent = intents::add, onBiometricClick = { biometricClicks++ })

        biometricButton().performClick()
        otherMethodButton().performClick()

        composeRule.runOnIdle {
            // 프롬프트는 stateful 층이 띄우므로 지문 버튼은 Intent 가 아니라 콜백이다.
            assertEquals(1, biometricClicks)
            assertEquals(listOf<BiometricLoginIntent>(BiometricLoginIntent.ChooseOtherMethod), intents)
        }
    }

    @Test
    public fun authenticatingState_disablesOnlyBiometricButton() {
        var biometricClicks = 0
        setContent(state = sampleState.copy(isAuthenticating = true), onBiometricClick = { biometricClicks++ })

        biometricButton()
            .assertIsNotEnabled()
            .assert(hasStateDescription("지문 확인 중"))
            .performClick()
        otherMethodButton().assertIsEnabled()

        composeRule.runOnIdle { assertEquals(0, biometricClicks) }
    }

    @Test
    public fun failureState_showsReasonAndDismissConsumesFailure() {
        val intents = mutableListOf<BiometricLoginIntent>()
        setContent(state = sampleState.copy(failure = BiometricFailureReason.Failed), onIntent = intents::add)

        composeRule.onNodeWithText("지문을 확인하지 못했어요. 다시 시도해 주세요").assertIsDisplayed()
        composeRule
            .onNodeWithContentDescription("오류 안내 닫기")
            .assertIsDisplayed()
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Button))
            .assertHeightIsAtLeast(48.dp)
            .assertWidthIsAtLeast(48.dp)
            .performClick()

        composeRule.runOnIdle {
            assertEquals(listOf<BiometricLoginIntent>(BiometricLoginIntent.ConsumeFailure), intents)
        }
    }

    @Test
    public fun largeFontScale_keepsActionsReachable() {
        setContent(state = sampleState, fontScale = 2f)

        biometricButton().performScrollTo().assertIsDisplayed()
        otherMethodButton().performScrollTo().assertIsDisplayed()
    }

    private fun biometricButton() = composeRule.onNodeWithContentDescription("지문으로 로그인")

    private fun otherMethodButton() = composeRule.onNode(hasText("다른 방법으로 로그인") and hasClickAction())

    private fun setContent(
        state: BiometricLoginUiState,
        onIntent: (BiometricLoginIntent) -> Unit = {},
        onBiometricClick: () -> Unit = {},
        fontScale: Float = 1f,
    ) {
        composeRule.setContent {
            val currentDensity = LocalDensity.current
            CompositionLocalProvider(
                LocalDensity provides Density(currentDensity.density, fontScale),
            ) {
                CareerCompassTheme {
                    BiometricLoginContent(state = state, onIntent = onIntent, onBiometricClick = onBiometricClick)
                }
            }
        }
    }

    private companion object {
        val sampleState = BiometricLoginUiState(userName = "일혁", isBiometricEnabled = true)
    }
}
