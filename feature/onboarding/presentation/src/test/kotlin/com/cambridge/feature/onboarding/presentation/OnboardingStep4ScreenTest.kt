package com.cambridge.feature.onboarding.presentation

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.assertHeightIsEqualTo
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertWidthIsEqualTo
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.ComposeContentTestRule
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import com.cambridge.core.ui.theme.CareerCompassTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], qualifiers = "w360dp-h800dp")
public class OnboardingStep4ScreenTest {
    @get:Rule
    public val composeRule = createComposeRule()

    @Test
    public fun invalidDocumentAndDuplicateIds_areRejectedByContract() {
        assertThrows(IllegalArgumentException::class.java) {
            sampleDocument.copy(fileName = "   ")
        }
        assertThrows(IllegalArgumentException::class.java) {
            OnboardingApplicationDocumentStatus.Completed(classifiedItemCount = -1)
        }
        assertThrows(IllegalArgumentException::class.java) {
            OnboardingApplicationDocumentStatus.Failed(message = "   ")
        }
        assertThrows(IllegalArgumentException::class.java) {
            OnboardingStep4UiState(
                uploadedDocuments = listOf(sampleDocument, sampleDocument),
            )
        }
    }

    @Test
    public fun supportedFormatsSizeBoundaryAndEleventhUpload_areEnforced() {
        assertEquals(
            OnboardingApplicationDocumentFormat.PDF,
            OnboardingApplicationDocumentFormat.fromFileName("resume.PDF"),
        )
        assertEquals(
            OnboardingApplicationDocumentFormat.DOCX,
            OnboardingApplicationDocumentFormat.fromFileName("resume.docx"),
        )
        assertEquals(
            OnboardingApplicationDocumentFormat.TXT,
            OnboardingApplicationDocumentFormat.fromFileName("resume.txt"),
        )
        assertThrows(IllegalArgumentException::class.java) {
            OnboardingApplicationDocumentFormat.fromFileName("resume.png")
        }
        assertEquals(
            ONBOARDING_MAX_APPLICATION_FILE_SIZE_BYTES,
            sampleDocument
                .copy(fileSizeBytes = ONBOARDING_MAX_APPLICATION_FILE_SIZE_BYTES)
                .fileSizeBytes,
        )
        assertThrows(IllegalArgumentException::class.java) {
            sampleDocument.copy(
                fileSizeBytes = ONBOARDING_MAX_APPLICATION_FILE_SIZE_BYTES + 1L,
            )
        }
        assertThrows(IllegalArgumentException::class.java) {
            sampleDocument.copy(
                fileName = "resume.docx",
                format = OnboardingApplicationDocumentFormat.PDF,
            )
        }
        assertThrows(IllegalArgumentException::class.java) {
            OnboardingStep4UiState(
                uploadedDocuments =
                    List(11) { index ->
                        sampleDocument.copy(
                            id = "application-$index",
                            fileName = "resume-$index.pdf",
                        )
                    },
            )
        }
    }

    @Test
    public fun emptyState_disablesCompleteButKeepsSkipAvailable() {
        val state = OnboardingStep4UiState()

        composeRule.setStep4Content(state = state)

        assertFalse(state.isCompleteEnabled)
        composeRule.onNodeWithContentDescription("지원서 파일 업로드").assertIsEnabled()
        composeRule.onAllNodesWithText("업로드한 지원서 (0/10)").assertCountEquals(0)
        skipButton().assertIsEnabled()
        completeButton().assertIsNotEnabled()
    }

    @Test
    public fun everyCompletedDocument_enablesComplete() {
        composeRule.setStep4Content(state = uploadedState)

        assertTrue(uploadedState.isCompleteEnabled)
        composeRule
            .onNodeWithText("업로드한 지원서 (1/10)")
            .performScrollTo()
            .assertIsDisplayed()
        composeRule
            .onNodeWithText(sampleDocument.fileName)
            .performScrollTo()
            .assertIsDisplayed()
        composeRule
            .onNodeWithText("분류 완료 · 4개 항목")
            .performScrollTo()
            .assertIsDisplayed()
        completeButton().assertIsEnabled()
    }

    @Test
    public fun processingDocument_disablesCompleteAndShowsProgressState() {
        composeRule.setStep4Content(state = processingState)

        assertFalse(processingState.isCompleteEnabled)
        composeRule
            .onNodeWithText("분류 중")
            .performScrollTo()
            .assertIsDisplayed()
            .assert(
                SemanticsMatcher.expectValue(
                    SemanticsProperties.LiveRegion,
                    LiveRegionMode.Polite,
                ),
            )
        completeButton().assertIsNotEnabled()
    }

    @Test
    public fun uploadLimit_disablesOnlyUploadTarget() {
        val fullState =
            OnboardingStep4UiState(
                uploadedDocuments =
                    List(ONBOARDING_MAX_APPLICATION_UPLOAD_COUNT) { index ->
                        sampleDocument.copy(
                            id = "document-$index",
                            fileName = "지원서-$index.pdf",
                        )
                    },
            )

        composeRule.setStep4Content(state = fullState)

        assertFalse(fullState.isUploadEnabled)
        composeRule.onNodeWithContentDescription("지원서 파일 업로드").assertIsNotEnabled()
        directInputButton().assertIsEnabled()
        completeButton().assertIsEnabled()
    }

    @Test
    public fun disabledState_disablesDocumentAndFooterActions() {
        val events = mutableListOf<OnboardingStep4Event>()
        composeRule.setStep4Content(
            state = failedState.copy(isInputEnabled = false),
            onEvent = events::add,
        )

        composeRule.onNodeWithContentDescription("지원서 파일 업로드").assertIsNotEnabled()
        directInputButton().assertIsNotEnabled()
        retryButton().assertIsNotEnabled()
        documentMenuButton().assertIsNotEnabled()
        skipButton().assertIsNotEnabled()
        completeButton().assertIsNotEnabled()

        composeRule.onNodeWithContentDescription("지원서 파일 업로드").performClick()
        directInputButton().performClick()
        retryButton().performClick()
        documentMenuButton().performClick()
        skipButton().performClick()
        completeButton().performClick()

        assertTrue(events.isEmpty())
    }

    @Test
    public fun primaryControls_forwardSeparateExplicitEvents() {
        val events = mutableListOf<OnboardingStep4Event>()
        composeRule.setStep4Content(state = uploadedState, onEvent = events::add)

        composeRule.onNodeWithContentDescription("지원서 파일 업로드").performClick()
        directInputButton().performScrollTo().performClick()
        documentMenuButton().performScrollTo().performClick()
        composeRule.onNodeWithContentDescription("뒤로가기").performClick()
        skipButton().performClick()
        completeButton().performClick()

        assertEquals(
            listOf(
                OnboardingStep4Event.UploadClicked,
                OnboardingStep4Event.DirectInputClicked,
                OnboardingStep4Event.DocumentMenuClicked(sampleDocument.id),
                OnboardingStep4Event.BackClicked,
                OnboardingStep4Event.SkipClicked,
                OnboardingStep4Event.CompleteClicked,
            ),
            events,
        )
    }

    @Test
    public fun failedDocument_disablesCompleteAndEmitsDistinctRetryAndMenuEvents() {
        val events = mutableListOf<OnboardingStep4Event>()
        composeRule.setStep4Content(state = failedState, onEvent = events::add)

        assertFalse(failedState.isCompleteEnabled)
        composeRule
            .onNodeWithText("파일을 처리하지 못했어요 · 재시도")
            .performScrollTo()
            .assertIsDisplayed()
        completeButton().assertIsNotEnabled()
        retryButton().assert(
            SemanticsMatcher.expectValue(
                SemanticsProperties.LiveRegion,
                LiveRegionMode.Polite,
            ),
        )
        retryButton().performClick()
        documentMenuButton().performClick()

        assertEquals(
            listOf(
                OnboardingStep4Event.DocumentRetryClicked(sampleDocument.id),
                OnboardingStep4Event.DocumentMenuClicked(sampleDocument.id),
            ),
            events,
        )
    }

    @Test
    public fun compactControlsAndDocumentCard_matchFigmaDimensions() {
        composeRule.setStep4Content(state = uploadedState)

        val directInputButton =
            directInputButton()
                .performScrollTo()
                .assertWidthIsEqualTo(103.dp)
                .assertHeightIsEqualTo(51.dp)
        val directInputLabel =
            composeRule.onNodeWithText(
                text = "직접 입력하기",
                useUnmergedTree = true,
            )
        assertBoundsContained(parent = directInputButton, child = directInputLabel)

        val uploadIconSlot =
            composeRule
                .onNodeWithTag(
                    testTag = "onboarding_step4_upload_icon_slot",
                    useUnmergedTree = true,
                ).assertWidthIsEqualTo(44.dp)
                .assertHeightIsEqualTo(44.dp)
        val uploadTitle =
            composeRule.onNodeWithText(
                text = "파일을 드래그하거나 탭하세요",
                useUnmergedTree = true,
            )
        val iconToTitleGap =
            uploadTitle.getUnclippedBoundsInRoot().top -
                uploadIconSlot.getUnclippedBoundsInRoot().bottom
        assertEquals(8f, iconToTitleGap.value, 0.5f)
        val uploadIconArt =
            composeRule
                .onNodeWithTag(
                    testTag = "onboarding_step4_upload_icon_art",
                    useUnmergedTree = true,
                ).assertWidthIsEqualTo(62.05.dp)
                .assertHeightIsEqualTo(63.05.dp)
        val uploadIconSlotBounds = uploadIconSlot.getUnclippedBoundsInRoot()
        val uploadIconArtBounds = uploadIconArt.getUnclippedBoundsInRoot()
        assertEquals(uploadIconSlotBounds.left.value, uploadIconArtBounds.left.value, 0.5f)
        assertEquals(uploadIconSlotBounds.top.value, uploadIconArtBounds.top.value, 0.5f)

        composeRule
            .onNodeWithTag("onboarding_step4_document_${sampleDocument.id}")
            .performScrollTo()
            .assertWidthIsEqualTo(232.dp)
        composeRule
            .onNodeWithTag("onboarding_step4_document_text_${sampleDocument.id}")
            .assertWidthIsEqualTo(164.dp)
        composeRule
            .onNodeWithTag("onboarding_step4_document_${sampleDocument.id}")
            .assertHeightIsEqualTo(63.dp)
        documentMenuButton()
            .assertWidthIsEqualTo(48.dp)
            .assertHeightIsEqualTo(48.dp)
        composeRule
            .onNodeWithTag("onboarding_step4_document_format_${sampleDocument.id}")
            .assertWidthIsEqualTo(20.dp)
            .assertHeightIsEqualTo(15.dp)
        skipButton()
            .assertWidthIsEqualTo(100.dp)
            .assertHeightIsEqualTo(52.dp)
        completeButton()
            .assertWidthIsEqualTo(220.dp)
            .assertHeightIsEqualTo(52.dp)
    }

    @Test
    public fun uploadTarget_exposesButtonRoleAndAccessibleName() {
        composeRule.setStep4Content(state = OnboardingStep4UiState())

        composeRule
            .onNodeWithContentDescription("지원서 파일 업로드")
            .assert(
                SemanticsMatcher.expectValue(
                    SemanticsProperties.Role,
                    Role.Button,
                ),
            )
    }

    @Test
    public fun largeFontScale_expandsControlsWithoutClippingActionText() {
        composeRule.setStep4Content(
            state =
                uploadedState.copy(
                    uploadedDocuments =
                        listOf(
                            sampleDocument.copy(
                                fileName = "아주 긴 이름을 가진 2024 카카오 인턴 과거 지원서.pdf",
                            ),
                        ),
                ),
            fontScale = 2f,
        )

        composeRule.onNodeWithContentDescription("지원서 파일 업로드").assertIsDisplayed()
        val directInputButton =
            directInputButton()
                .performScrollTo()
                .assertIsDisplayed()
                .assertWidthIsEqualTo(328.dp)
                .assertHeightIsAtLeast(51.dp)
        val directInputLabel =
            composeRule.onNodeWithText(
                text = "직접 입력하기",
                useUnmergedTree = true,
            )
        assertBoundsContained(parent = directInputButton, child = directInputLabel)
        assertTextHasNoVisualOverflow(directInputLabel)
        composeRule
            .onNodeWithText("분류 완료 · 4개 항목")
            .performScrollTo()
            .assertIsDisplayed()
        val documentCard =
            composeRule
                .onNodeWithTag("onboarding_step4_document_${sampleDocument.id}")
                .assertWidthIsEqualTo(328.dp)
                .assertHeightIsAtLeast(64.dp)
        val statusLabel =
            composeRule.onNodeWithText(
                text = "분류 완료 · 4개 항목",
                useUnmergedTree = true,
            )
        assertBoundsContained(parent = documentCard, child = statusLabel)
        assertTextHasNoVisualOverflow(statusLabel)
        assertTrue(
            "status text overlaps the 48dp document menu",
            statusLabel.getUnclippedBoundsInRoot().right <=
                documentCard.getUnclippedBoundsInRoot().right - 48.dp,
        )
        composeRule
            .onNodeWithTag("onboarding_step4_document_format_${sampleDocument.id}")
            .assertWidthIsEqualTo(40.dp)
            .assertHeightIsEqualTo(30.dp)
        val skipButton =
            skipButton()
                .assertIsDisplayed()
                .assertWidthIsEqualTo(100.dp)
                .assertHeightIsAtLeast(52.dp)
        val completeButton =
            completeButton()
                .assertIsDisplayed()
                .assertWidthIsEqualTo(220.dp)
                .assertHeightIsAtLeast(52.dp)
        val skipLabel =
            composeRule.onNodeWithText(
                text = "건너뛰기",
                useUnmergedTree = true,
            )
        val completeLabel =
            composeRule.onNodeWithText(
                text = "완료",
                useUnmergedTree = true,
            )
        assertBoundsContained(parent = skipButton, child = skipLabel)
        assertBoundsContained(parent = completeButton, child = completeLabel)
        assertTextHasNoVisualOverflow(skipLabel)
        assertTextHasNoVisualOverflow(completeLabel)
    }

    private fun directInputButton() =
        composeRule.onNode(
            hasText("직접 입력하기") and hasClickAction(),
        )

    private fun retryButton() = composeRule.onNodeWithContentDescription("${sampleDocument.fileName} 분류 재시도")

    private fun documentMenuButton() = composeRule.onNodeWithContentDescription("${sampleDocument.fileName} 메뉴")

    private fun skipButton() =
        composeRule.onNode(
            hasText("건너뛰기") and hasClickAction(),
        )

    private fun completeButton() =
        composeRule.onNode(
            hasText("완료") and hasClickAction(),
        )

    private fun assertBoundsContained(
        parent: SemanticsNodeInteraction,
        child: SemanticsNodeInteraction,
    ) {
        val parentBounds = parent.getUnclippedBoundsInRoot()
        val childBounds = child.getUnclippedBoundsInRoot()

        assertTrue(
            "child starts before parent: $childBounds vs $parentBounds",
            childBounds.left >= parentBounds.left && childBounds.top >= parentBounds.top,
        )
        assertTrue(
            "child ends after parent: $childBounds vs $parentBounds",
            childBounds.right <= parentBounds.right && childBounds.bottom <= parentBounds.bottom,
        )
    }

    private fun assertTextHasNoVisualOverflow(node: SemanticsNodeInteraction) {
        val textLayoutResults = mutableListOf<TextLayoutResult>()
        val getTextLayoutResult =
            node.fetchSemanticsNode().config[SemanticsActions.GetTextLayoutResult]

        assertTrue(getTextLayoutResult.action?.invoke(textLayoutResults) == true)
        assertEquals(1, textLayoutResults.size)
        val result = textLayoutResults.single()
        assertFalse(
            "Text overflowed: size=${result.size}, lines=${result.lineCount}, " +
                "width=${result.didOverflowWidth}, height=${result.didOverflowHeight}, " +
                "constraints=${result.layoutInput.constraints}, " +
                "lineRight=${result.getLineRight(0)}, end=${result.getLineEnd(0)}, " +
                "ellipsized=${result.isLineEllipsized(0)}, paragraphWidth=${result.multiParagraph.width}",
            result.hasVisualOverflow,
        )
    }

    private companion object {
        val sampleDocument =
            OnboardingApplicationDocument(
                id = "application-1",
                fileName = "2024 카카오 인턴 자소서.pdf",
                format = OnboardingApplicationDocumentFormat.PDF,
                fileSizeBytes = 512L * 1024L,
                status = OnboardingApplicationDocumentStatus.Completed(classifiedItemCount = 4),
            )

        val uploadedState =
            OnboardingStep4UiState(
                uploadedDocuments = listOf(sampleDocument),
            )

        val processingState =
            OnboardingStep4UiState(
                uploadedDocuments =
                    listOf(
                        sampleDocument,
                        sampleDocument.copy(
                            id = "application-2",
                            fileName = "processing.pdf",
                            status = OnboardingApplicationDocumentStatus.Processing,
                        ),
                    ),
            )

        val failedState =
            OnboardingStep4UiState(
                uploadedDocuments =
                    listOf(
                        sampleDocument.copy(
                            status =
                                OnboardingApplicationDocumentStatus.Failed(
                                    message = "파일을 처리하지 못했어요",
                                ),
                        ),
                    ),
            )
    }
}

private fun ComposeContentTestRule.setStep4Content(
    state: OnboardingStep4UiState,
    onEvent: (OnboardingStep4Event) -> Unit = {},
    fontScale: Float = 1f,
) {
    setContent {
        val currentDensity = LocalDensity.current
        CompositionLocalProvider(
            LocalDensity provides Density(currentDensity.density, fontScale),
        ) {
            CareerCompassTheme {
                OnboardingStep4Screen(state = state, onEvent = onEvent)
            }
        }
    }
}
