package com.cambridge.feature.onboarding.presentation.biometric

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
public class BiometricEnrollSheetTest {
    @get:Rule
    public val composeRule = createComposeRule()

    @Test
    public fun defaultState_showsOfferAndBothAnswers() {
        setSheet(state = BiometricEnrollUiState())

        composeRule.onNodeWithText("지문으로 더 빠르게 로그인할까요?").assertIsDisplayed()
        composeRule.onNodeWithText("다음 로그인부터 지문만으로 바로 들어올 수 있어요").assertIsDisplayed()
        enrollButton().assertIsDisplayed().assertIsEnabled()
        laterButton().assertIsDisplayed().assertIsEnabled()
        composeRule.onAllNodesWithContentDescription("오류 안내 닫기").assertCountEquals(0)
        // 장식용 아이콘은 읽히지 않는다.
        composeRule.onAllNodesWithText("👆", useUnmergedTree = true).assertCountEquals(0)
    }

    @Test
    public fun answers_emitDistinctEvents() {
        val events = mutableListOf<BiometricEnrollEvent>()
        setSheet(state = BiometricEnrollUiState(), onEvent = events::add)

        enrollButton().performClick()
        laterButton().performClick()

        composeRule.runOnIdle {
            assertEquals(
                listOf(BiometricEnrollEvent.EnrollClicked, BiometricEnrollEvent.LaterClicked),
                events,
            )
        }
    }

    @Test
    public fun registeringState_locksBothAnswers() {
        val events = mutableListOf<BiometricEnrollEvent>()
        setSheet(state = BiometricEnrollUiState(isRegistering = true), onEvent = events::add)

        composeRule
            .onNodeWithText("등록하는 중")
            .assertIsDisplayed()
            .assertIsNotEnabled()
            .performClick()
        laterButton().assertIsNotEnabled().performClick()
        composeRule.onAllNodesWithText("지문 등록하기").assertCountEquals(0)

        composeRule.runOnIdle { assertTrue(events.isEmpty()) }
    }

    @Test
    public fun errorState_showsDismissibleCardAndKeepsRetry() {
        val events = mutableListOf<BiometricEnrollEvent>()
        setSheet(
            state = BiometricEnrollUiState(errorMessage = "지문을 확인하지 못했어요. 다시 시도해 주세요"),
            onEvent = events::add,
        )

        composeRule.onNodeWithText("지문을 확인하지 못했어요. 다시 시도해 주세요").assertIsDisplayed()
        enrollButton().assertIsEnabled()
        composeRule
            .onNodeWithContentDescription("오류 안내 닫기")
            .assertIsDisplayed()
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Button))
            .assertHeightIsAtLeast(48.dp)
            .assertWidthIsAtLeast(48.dp)
            .performClick()

        composeRule.runOnIdle { assertEquals(listOf(BiometricEnrollEvent.ErrorDismissed), events) }
    }

    @Test
    public fun largeFontScale_keepsAnswersReachable() {
        setSheet(state = BiometricEnrollUiState(), fontScale = 2f)

        enrollButton().performScrollTo().assertIsDisplayed()
        laterButton().performScrollTo().assertIsDisplayed()
    }

    private fun enrollButton() = composeRule.onNode(hasText("지문 등록하기") and hasClickAction())

    private fun laterButton() = composeRule.onNode(hasText("나중에 하기") and hasClickAction())

    private fun setSheet(
        state: BiometricEnrollUiState,
        onEvent: (BiometricEnrollEvent) -> Unit = {},
        fontScale: Float = 1f,
    ) {
        composeRule.setContent {
            val currentDensity = LocalDensity.current
            CompositionLocalProvider(
                LocalDensity provides Density(currentDensity.density, fontScale),
            ) {
                CareerCompassTheme {
                    BiometricEnrollSheet(state = state, onEvent = onEvent)
                }
            }
        }
    }
}
