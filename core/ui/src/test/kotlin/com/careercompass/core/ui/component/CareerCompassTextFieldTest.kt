package com.careercompass.core.ui.component

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertHeightIsEqualTo
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.click
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.unit.dp
import com.careercompass.core.ui.theme.CareerCompassTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
public class CareerCompassTextFieldTest {
    @get:Rule
    public val composeRule = createComposeRule()

    @Test
    public fun enabledField_supportsEditingAndSelectionSemantics() {
        composeRule.setContent {
            var value by remember { mutableStateOf("이력서") }

            CareerCompassTheme {
                CareerCompassTextField(
                    value = value,
                    onValueChange = { value = it },
                    label = "문서 이름",
                )
            }
        }

        val editableNode =
            composeRule.onNode(
                SemanticsMatcher.keyIsDefined(SemanticsProperties.EditableText),
            )

        editableNode
            .assertIsEnabled()
            .assert(SemanticsMatcher.keyIsDefined(SemanticsActions.SetText))
            .assert(SemanticsMatcher.keyIsDefined(SemanticsActions.SetSelection))

        editableNode.performSemanticsAction(SemanticsActions.SetSelection) { setSelection ->
            assertTrue(setSelection(0, 2, false))
        }

        editableNode
            .assert(
                SemanticsMatcher.expectValue(
                    SemanticsProperties.TextSelectionRange,
                    TextRange(0, 2),
                ),
            )

        editableNode.performTextReplacement("포트폴리오")

        editableNode.assert(
            SemanticsMatcher.expectValue(
                SemanticsProperties.EditableText,
                AnnotatedString("포트폴리오"),
            ),
        )
    }

    @Test
    public fun disabledField_isDisabledAndDoesNotExposeTextEditing() {
        composeRule.setContent {
            CareerCompassTheme {
                CareerCompassTextField(
                    value = "읽기 전용",
                    onValueChange = {},
                    label = "상태",
                    enabled = false,
                )
            }
        }

        composeRule
            .onNode(SemanticsMatcher.keyIsDefined(SemanticsProperties.EditableText))
            .assertIsNotEnabled()
            .assert(!SemanticsMatcher.keyIsDefined(SemanticsActions.SetText))
    }

    @Test
    public fun readOnlyField_hasAccessibleLabelAndDoesNotExposeTextEditing() {
        composeRule.setContent {
            CareerCompassTheme {
                CareerCompassTextField(
                    value = "등록 완료",
                    onValueChange = {},
                    label = "지원 상태",
                    readOnly = true,
                )
            }
        }

        composeRule
            .onNode(SemanticsMatcher.keyIsDefined(SemanticsProperties.EditableText))
            .assertIsEnabled()
            .assert(
                SemanticsMatcher.expectValue(
                    SemanticsProperties.ContentDescription,
                    listOf("지원 상태"),
                ),
            ).assert(!SemanticsMatcher.keyIsDefined(SemanticsActions.SetText))
            .assert(SemanticsMatcher.keyIsDefined(SemanticsActions.SetSelection))
    }

    @Test
    public fun errorField_exposesSupportingTextAsErrorSemantics() {
        composeRule.setContent {
            CareerCompassTheme {
                CareerCompassTextField(
                    value = "",
                    onValueChange = {},
                    label = "이메일",
                    supportingText = "올바른 이메일을 입력해 주세요",
                    isError = true,
                )
            }
        }

        composeRule
            .onNode(SemanticsMatcher.keyIsDefined(SemanticsProperties.Error))
            .assert(
                SemanticsMatcher.expectValue(
                    SemanticsProperties.Error,
                    "올바른 이메일을 입력해 주세요",
                ),
            )
    }

    @Test
    public fun largeReadOnlyField_matchesOnboardingSizeAndInvokesPickerAction() {
        var clickCount = 0

        composeRule.setContent {
            CareerCompassTheme {
                CareerCompassTextField(
                    value = "2027년 2월",
                    onValueChange = {},
                    label = "졸업 예정일",
                    readOnly = true,
                    size = CareerCompassTextFieldSize.Large,
                    onClick = { clickCount += 1 },
                )
            }
        }

        composeRule
            .onNodeWithContentDescription("졸업 예정일")
            .assertHeightIsEqualTo(50.dp)
            .performTouchInput { click() }

        assertEquals(1, clickCount)
        composeRule
            .onAllNodesWithContentDescription("졸업 예정일")
            .assertCountEquals(1)
    }

    @Test
    public fun errorMessage_overridesNonErrorSupportingHintForAccessibility() {
        composeRule.setContent {
            CareerCompassTheme {
                CareerCompassTextField(
                    value = "",
                    onValueChange = {},
                    label = "이메일",
                    supportingText = "학교 이메일을 입력해 주세요",
                    errorMessage = "이메일 형식이 올바르지 않습니다",
                    isError = true,
                )
            }
        }

        composeRule
            .onNode(SemanticsMatcher.keyIsDefined(SemanticsProperties.Error))
            .assert(
                SemanticsMatcher.expectValue(
                    SemanticsProperties.Error,
                    "이메일 형식이 올바르지 않습니다",
                ),
            )
    }

    @Test
    public fun nonBlankLabel_isAcceptedAsAccessibilityName() {
        composeRule.setContent {
            CareerCompassTheme {
                CareerCompassTextField(
                    value = "",
                    onValueChange = {},
                    label = "문서 이름",
                )
            }
        }

        composeRule.onNodeWithContentDescription("문서 이름").assertExists()
    }

    @Test
    public fun blankLabel_isRejected() {
        val error =
            assertThrows(IllegalArgumentException::class.java) {
                composeRule.setContent {
                    CareerCompassTheme {
                        CareerCompassTextField(
                            value = "",
                            onValueChange = {},
                            label = " \t\n",
                        )
                    }
                }
            }

        assertEquals("label must not be blank", error.message)
    }
}
