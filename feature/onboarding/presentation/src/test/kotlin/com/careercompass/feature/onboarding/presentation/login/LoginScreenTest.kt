package com.careercompass.feature.onboarding.presentation.login

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
import com.careercompass.core.model.auth.SocialProvider
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
        setContent(state = LoginUiState())

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
        setContent(state = LoginUiState())

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
    public fun socialButtons_requestDistinctProviders() {
        val clicks = mutableListOf<SocialProvider>()
        val intents = mutableListOf<LoginIntent>()
        setContent(state = LoginUiState(), onIntent = intents::add, onSocialLoginClick = clicks::add)

        kakaoButton().performClick()
        googleButton().performClick()

        composeRule.runOnIdle {
            assertEquals(listOf(SocialProvider.Kakao, SocialProvider.Google), clicks)
            // 토큰 요청은 stateful 층의 몫이라 Content 는 Intent 를 만들지 않는다.
            assertTrue(intents.isEmpty())
        }
    }

    @Test
    public fun loadingState_disablesSocialButtonsAndShowsProgress() {
        val clicks = mutableListOf<SocialProvider>()
        setContent(state = LoginUiState(isLoading = true), onSocialLoginClick = clicks::add)

        composeRule.onNodeWithText("로그인하는 중이에요").assertIsDisplayed()
        kakaoButton().assertIsNotEnabled().performClick()
        googleButton().assertIsNotEnabled().performClick()

        composeRule.runOnIdle { assertTrue(clicks.isEmpty()) }
    }

    /** 이동이 대기 중인 동안도 진행 중으로 그린다 — 관문이 프로필을 받는 사이 버튼이 살아 있으면 SDK 를 또 연다. */
    @Test
    public fun pendingNavigation_keepsProgressAndLocksButtons() {
        setContent(state = LoginUiState(pendingNavigation = LoginDestination.Feed))

        composeRule.onNodeWithText("로그인하는 중이에요").assertIsDisplayed()
        kakaoButton().assertIsNotEnabled()
        googleButton().assertIsNotEnabled()
    }

    @Test
    public fun failureState_showsReasonAndDismissConsumesFailure() {
        val intents = mutableListOf<LoginIntent>()
        setContent(state = LoginUiState(failure = LoginFailureReason.Network), onIntent = intents::add)

        composeRule.onNodeWithText("네트워크 연결을 확인한 뒤 다시 시도해 주세요").assertIsDisplayed()
        composeRule
            .onNodeWithContentDescription("오류 안내 닫기")
            .assertIsDisplayed()
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Button))
            .assertHeightIsAtLeast(48.dp)
            .assertWidthIsAtLeast(48.dp)
            .performClick()

        composeRule.runOnIdle {
            assertEquals(listOf<LoginIntent>(LoginIntent.ConsumeFailure), intents)
        }
    }

    /** 만료 안내는 셸의 상태라 닫기가 Intent 가 아니라 셸 콜백으로 돌아간다. */
    @Test
    public fun sessionExpiryNotice_showsSameCardAndDismissReturnsToShell() {
        val intents = mutableListOf<LoginIntent>()
        var dismissed = 0
        setContent(
            state = LoginUiState(),
            isSessionExpiryNoticeVisible = true,
            onIntent = intents::add,
            onSessionExpiryNoticeDismissed = { dismissed++ },
        )

        composeRule.onNodeWithText("로그인이 만료됐어요. 다시 로그인해 주세요").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("오류 안내 닫기").performClick()

        composeRule.runOnIdle {
            assertEquals(1, dismissed)
            assertTrue(intents.isEmpty())
        }
    }

    /** 방금 누른 버튼의 결과가 급하므로 실패가 만료 안내보다 먼저다. */
    @Test
    public fun failure_takesPrecedenceOverSessionExpiryNotice() {
        setContent(state = LoginUiState(failure = LoginFailureReason.Rejected), isSessionExpiryNoticeVisible = true)

        composeRule.onNodeWithText("로그인에 실패했어요. 다시 시도해 주세요").assertIsDisplayed()
        composeRule.onAllNodesWithText("로그인이 만료됐어요. 다시 로그인해 주세요").assertCountEquals(0)
    }

    @Test
    public fun largeFontScale_keepsSocialButtonsReachable() {
        setContent(state = LoginUiState(), fontScale = 2f)

        kakaoButton().performScrollTo().assertIsDisplayed()
        googleButton().performScrollTo().assertIsDisplayed()
    }

    private fun kakaoButton() = composeRule.onNode(hasText("카카오 로그인") and hasClickAction())

    private fun googleButton() = composeRule.onNode(hasText("Google 계정으로 로그인") and hasClickAction())

    private fun setContent(
        state: LoginUiState,
        isSessionExpiryNoticeVisible: Boolean = false,
        onIntent: (LoginIntent) -> Unit = {},
        onSocialLoginClick: (SocialProvider) -> Unit = {},
        onSessionExpiryNoticeDismissed: () -> Unit = {},
        fontScale: Float = 1f,
    ) {
        composeRule.setContent {
            val currentDensity = LocalDensity.current
            CompositionLocalProvider(
                LocalDensity provides Density(currentDensity.density, fontScale),
            ) {
                CareerCompassTheme {
                    LoginContent(
                        state = state,
                        isSessionExpiryNoticeVisible = isSessionExpiryNoticeVisible,
                        onIntent = onIntent,
                        onSocialLoginClick = onSocialLoginClick,
                        onSessionExpiryNoticeDismissed = onSessionExpiryNoticeDismissed,
                    )
                }
            }
        }
    }
}
