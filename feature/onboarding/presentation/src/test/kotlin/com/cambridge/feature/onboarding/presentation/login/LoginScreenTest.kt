package com.cambridge.feature.onboarding.presentation.login

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
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertWidthIsAtLeast
import androidx.compose.ui.test.hasClickAction
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
public class LoginScreenTest {
    @get:Rule
    public val composeRule = createComposeRule()

    @Test
    public fun defaultState_showsBrandAndSocialActionsOnly() {
        setScreen(state = LoginUiState())

        composeRule.onNodeWithText("CareerCompass").assertIsDisplayed()
        composeRule.onNodeWithText("AI가 분석하는 나만의 진로 나침반").assertIsDisplayed()
        composeRule
            .onNodeWithText("계속하면 서비스 이용약관과 개인정보 처리방침에 동의하는 것으로 간주돼요")
            .assertIsDisplayed()
        kakaoButton().assertIsDisplayed().assertIsEnabled()
        googleButton().assertIsDisplayed().assertIsEnabled()
        composeRule.onAllNodesWithText("로그인하는 중이에요").assertCountEquals(0)
        composeRule.onAllNodesWithContentDescription("오류 안내 닫기").assertCountEquals(0)
    }

    @Test
    public fun socialButtons_exposeButtonRoleNameAndTouchTarget() {
        setScreen(state = LoginUiState())

        listOf(kakaoButton(), googleButton()).forEach { button ->
            button
                .assert(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Button))
                .assertHeightIsAtLeast(52.dp)
                .assertWidthIsAtLeast(48.dp)
        }
        // 브랜드 마크가 다시 글리프·이모지로 돌아가지 않는지. 마크는 벡터라 어느 쪽도 텍스트로 잡히지 않는다.
        composeRule.onAllNodesWithText("💬", useUnmergedTree = true).assertCountEquals(0)
        composeRule.onAllNodesWithText("G", useUnmergedTree = true).assertCountEquals(0)
    }

    @Test
    public fun socialButtons_emitDistinctEvents() {
        val events = mutableListOf<LoginEvent>()
        setScreen(state = LoginUiState(), onEvent = events::add)

        kakaoButton().performClick()
        googleButton().performClick()

        composeRule.runOnIdle {
            assertEquals(
                listOf(LoginEvent.KakaoLoginClicked, LoginEvent.GoogleLoginClicked),
                events,
            )
        }
    }

    @Test
    public fun loadingState_disablesSocialButtonsAndShowsProgress() {
        val events = mutableListOf<LoginEvent>()
        setScreen(state = LoginUiState(isLoading = true), onEvent = events::add)

        composeRule.onNodeWithText("로그인하는 중이에요").assertIsDisplayed()
        kakaoButton().assertIsNotEnabled().performClick()
        googleButton().assertIsNotEnabled().performClick()

        composeRule.runOnIdle { assertTrue(events.isEmpty()) }
    }

    @Test
    public fun errorState_showsDismissibleCard() {
        val events = mutableListOf<LoginEvent>()
        setScreen(
            state = LoginUiState(errorMessage = "카카오 로그인에 실패했어요"),
            onEvent = events::add,
        )

        composeRule.onNodeWithText("카카오 로그인에 실패했어요").assertIsDisplayed()
        composeRule
            .onNodeWithContentDescription("오류 안내 닫기")
            .assertIsDisplayed()
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Button))
            .assertHeightIsAtLeast(48.dp)
            .assertWidthIsAtLeast(48.dp)
            .performClick()

        composeRule.runOnIdle {
            assertEquals(listOf(LoginEvent.ErrorDismissed), events)
        }
    }

    @Test
    public fun largeFontScale_keepsSocialButtonsReachable() {
        setScreen(state = LoginUiState(), fontScale = 2f)

        kakaoButton().performScrollTo().assertIsDisplayed()
        googleButton().performScrollTo().assertIsDisplayed()
    }

    private fun kakaoButton() = composeRule.onNode(hasText("카카오 로그인") and hasClickAction())

    private fun googleButton() = composeRule.onNode(hasText("Google 계정으로 로그인") and hasClickAction())

    private fun setScreen(
        state: LoginUiState,
        onEvent: (LoginEvent) -> Unit = {},
        fontScale: Float = 1f,
    ) {
        composeRule.setContent {
            val currentDensity = LocalDensity.current
            CompositionLocalProvider(
                LocalDensity provides Density(currentDensity.density, fontScale),
            ) {
                CareerCompassTheme {
                    LoginScreen(state = state, onEvent = onEvent)
                }
            }
        }
    }
}
