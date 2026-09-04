package com.cambridge.feature.onboarding.presentation.pastapplication

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextReplacement
import com.cambridge.core.ui.theme.CareerCompassTheme
import com.cambridge.feature.onboarding.presentation.shared.model.OnboardingFieldError
import com.careercompass.core.model.application.UploadFile
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.ByteArrayInputStream

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], qualifiers = "w360dp-h800dp")
public class UploadLabelSheetTest {
    @get:Rule
    public val composeRule = createComposeRule()

    @Test
    public fun defaultState_showsSelectedFileAndPrefilledLabel() {
        setSheet(state(label = "이력서_최종_v3(2)"))

        composeRule.onNodeWithText("지원서 이름 확인").assertIsDisplayed()
        composeRule.onNodeWithText("선택한 파일 · 이력서_최종_v3(2).pdf").assertIsDisplayed()
        composeRule.onNodeWithText("이력서_최종_v3(2)").assertIsDisplayed()
        submitButton().assertIsEnabled()
    }

    @Test
    public fun blankLabel_disablesSubmit() {
        setSheet(state(label = "   "))

        submitButton().assertIsNotEnabled()
    }

    @Test
    public fun labelError_isRendered() {
        setSheet(state(label = "가".repeat(51), labelError = OnboardingFieldError.TooLong(50)))

        composeRule.onNodeWithText("50자 이내로 입력해 주세요").assertIsDisplayed()
    }

    @Test
    public fun controls_emitDistinctEvents() {
        val events = mutableListOf<UploadLabelEvent>()
        setSheet(state(label = "이력서"), onEvent = events::add)

        composeRule.onNodeWithContentDescription("지원서 이름 *").performTextReplacement("2024 카카오 인턴 자소서")
        submitButton().performClick()
        composeRule.onNode(hasText("취소") and hasClickAction()).performClick()

        // stateless 필드는 호스트가 값을 되돌리지 않으므로 입력 이벤트는 첫 발생만 본다.
        composeRule.runOnIdle {
            assertEquals(
                UploadLabelEvent.LabelChanged("2024 카카오 인턴 자소서"),
                events.filterIsInstance<UploadLabelEvent.LabelChanged>().first(),
            )
            assertEquals(
                listOf(UploadLabelEvent.Submitted, UploadLabelEvent.Dismissed),
                events.filterNot { it is UploadLabelEvent.LabelChanged },
            )
        }
    }

    private fun submitButton() = composeRule.onNode(hasText("업로드") and hasClickAction())

    private fun state(
        label: String,
        labelError: OnboardingFieldError? = null,
    ) = UploadLabelState(
        file = UploadFile(fileName = "이력서_최종_v3(2).pdf", sizeBytes = 16L) { ByteArrayInputStream(ByteArray(16)) },
        label = label,
        labelError = labelError,
    )

    private fun setSheet(
        state: UploadLabelState,
        onEvent: (UploadLabelEvent) -> Unit = {},
    ) {
        composeRule.setContent {
            CareerCompassTheme {
                UploadLabelSheet(state = state, onEvent = onEvent)
            }
        }
    }
}
