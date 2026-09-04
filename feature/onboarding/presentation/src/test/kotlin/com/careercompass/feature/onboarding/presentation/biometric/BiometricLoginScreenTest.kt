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
    public fun defaultState_showsGreetingAccountAndPrompt() {
        setScreen(state = sampleState)

        composeRule.onNodeWithContentDescription("CareerCompass").assertIsDisplayed()
        composeRule.onNodeWithText("일혁님, 안녕하세요").assertIsDisplayed()
        composeRule.onNodeWithText("1hyok@konkuk.ac.kr").assertIsDisplayed()
        composeRule.onNodeWithText("지문으로 빠른 로그인").assertIsDisplayed()
        composeRule.onNodeWithText("등록된 지문을 사용해 바로 로그인해요").assertIsDisplayed()
        otherMethodButton().assertIsDisplayed().assertIsEnabled()
        composeRule.onAllNodesWithContentDescription("오류 안내 닫기").assertCountEquals(0)
    }

    @Test
    public fun missingAccountLabel_hidesCaption() {
        setScreen(state = sampleState.copy(accountLabel = null))

        composeRule.onNodeWithText("일혁님, 안녕하세요").assertIsDisplayed()
        composeRule.onAllNodesWithText("1hyok@konkuk.ac.kr").assertCountEquals(0)
    }

    @Test
    public fun biometricButton_exposesRoleNameAndSize() {
        setScreen(state = sampleState)

        biometricButton()
            .assertIsDisplayed()
            .assertIsEnabled()
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Button))
            .assertWidthIsEqualTo(96.dp)
            .assertHeightIsEqualTo(96.dp)
        composeRule.onAllNodesWithText("👆", useUnmergedTree = true).assertCountEquals(0)
    }

    @Test
    public fun controls_emitDistinctEvents() {
        val events = mutableListOf<BiometricLoginEvent>()
        setScreen(state = sampleState, onEvent = events::add)

        biometricButton().performClick()
        otherMethodButton().performClick()

        composeRule.runOnIdle {
            assertEquals(
                listOf(
                    BiometricLoginEvent.BiometricClicked,
                    BiometricLoginEvent.OtherMethodClicked,
                ),
                events,
            )
        }
    }

    @Test
    public fun authenticatingState_disablesOnlyBiometricButton() {
        val events = mutableListOf<BiometricLoginEvent>()
        setScreen(state = sampleState.copy(isAuthenticating = true), onEvent = events::add)

        biometricButton()
            .assertIsNotEnabled()
            .assert(hasStateDescription("지문 확인 중"))
            .performClick()
        otherMethodButton().assertIsEnabled()

        composeRule.runOnIdle { assertTrue(events.isEmpty()) }
    }

    @Test
    public fun errorState_showsDismissibleCard() {
        val events = mutableListOf<BiometricLoginEvent>()
        setScreen(
            state = sampleState.copy(errorMessage = "지문을 인식하지 못했어요"),
            onEvent = events::add,
        )

        composeRule.onNodeWithText("지문을 인식하지 못했어요").assertIsDisplayed()
        composeRule
            .onNodeWithContentDescription("오류 안내 닫기")
            .assertIsDisplayed()
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Button))
            .assertHeightIsAtLeast(48.dp)
            .assertWidthIsAtLeast(48.dp)
            .performClick()

        composeRule.runOnIdle {
            assertEquals(listOf(BiometricLoginEvent.ErrorDismissed), events)
        }
    }

    @Test
    public fun largeFontScale_keepsActionsReachable() {
        setScreen(state = sampleState, fontScale = 2f)

        biometricButton().performScrollTo().assertIsDisplayed()
        otherMethodButton().performScrollTo().assertIsDisplayed()
    }

    private fun biometricButton() = composeRule.onNodeWithContentDescription("지문으로 로그인")

    private fun otherMethodButton() = composeRule.onNode(hasText("다른 방법으로 로그인") and hasClickAction())

    private fun setScreen(
        state: BiometricLoginUiState,
        onEvent: (BiometricLoginEvent) -> Unit = {},
        fontScale: Float = 1f,
    ) {
        composeRule.setContent {
            val currentDensity = LocalDensity.current
            CompositionLocalProvider(
                LocalDensity provides Density(currentDensity.density, fontScale),
            ) {
                CareerCompassTheme {
                    BiometricLoginScreen(state = state, onEvent = onEvent)
                }
            }
        }
    }

    private companion object {
        val sampleState =
            BiometricLoginUiState(
                userName = "일혁",
                accountLabel = "1hyok@konkuk.ac.kr",
            )
    }
}
