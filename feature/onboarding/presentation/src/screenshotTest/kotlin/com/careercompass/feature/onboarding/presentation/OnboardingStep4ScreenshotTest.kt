package com.careercompass.feature.onboarding.presentation

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.android.tools.screenshot.PreviewTest
import com.careercompass.core.ui.theme.CareerCompassTheme

@PreviewTest
@Preview(name = "Onboarding Step 4 uploaded", widthDp = 360, heightDp = 800)
@Composable
public fun OnboardingStep4UploadedPreview() {
    CareerCompassTheme {
        OnboardingStep4Content(
            state =
                OnboardingStep4UiState(
                    uploadedDocuments = listOf(sampleApplicationDocument),
                ),
            onEvent = {},
        )
    }
}

@PreviewTest
@Preview(name = "Onboarding Step 4 empty", widthDp = 360, heightDp = 800)
@Composable
public fun OnboardingStep4EmptyPreview() {
    CareerCompassTheme {
        OnboardingStep4Content(
            state = OnboardingStep4UiState(),
            onEvent = {},
        )
    }
}

@PreviewTest
@Preview(name = "Onboarding Step 4 disabled", widthDp = 360, heightDp = 800)
@Composable
public fun OnboardingStep4DisabledPreview() {
    CareerCompassTheme {
        OnboardingStep4Content(
            state =
                OnboardingStep4UiState(
                    uploadedDocuments = listOf(sampleApplicationDocument),
                    isInputEnabled = false,
                ),
            onEvent = {},
        )
    }
}

@PreviewTest
@Preview(name = "Onboarding Step 4 processing", widthDp = 360, heightDp = 800)
@Composable
public fun OnboardingStep4ProcessingPreview() {
    CareerCompassTheme {
        OnboardingStep4Content(
            state =
                OnboardingStep4UiState(
                    uploadedDocuments =
                        listOf(
                            sampleApplicationDocument.copy(
                                status = OnboardingApplicationDocumentStatus.Processing,
                            ),
                        ),
                ),
            onEvent = {},
        )
    }
}

@PreviewTest
@Preview(name = "Onboarding Step 4 failure", widthDp = 360, heightDp = 800)
@Composable
public fun OnboardingStep4FailurePreview() {
    CareerCompassTheme {
        OnboardingStep4Content(
            state =
                OnboardingStep4UiState(
                    uploadedDocuments =
                        listOf(
                            sampleApplicationDocument.copy(
                                status =
                                    OnboardingApplicationDocumentStatus.Failed(
                                        message = "파일을 처리하지 못했어요",
                                    ),
                            ),
                        ),
                ),
            onEvent = {},
        )
    }
}

@PreviewTest
@Preview(name = "Onboarding Step 4 expanded items", widthDp = 360, heightDp = 800)
@Composable
public fun OnboardingStep4ExpandedItemsPreview() {
    CareerCompassTheme {
        OnboardingStep4Content(
            state =
                OnboardingStep4UiState(
                    uploadedDocuments = listOf(sampleClassifiedDocument),
                    expandedDocumentId = sampleClassifiedDocument.id,
                ),
            onEvent = {},
        )
    }
}

private val sampleApplicationDocument =
    OnboardingApplicationDocument(
        id = "application-1",
        fileName = "2024 카카오 인턴 자소서.pdf",
        format = OnboardingApplicationDocumentFormat.PDF,
        fileSizeBytes = 512L * 1024L,
        status = OnboardingApplicationDocumentStatus.Completed(classifiedItemCount = 4),
    )

private val sampleClassifiedDocument =
    sampleApplicationDocument.copy(
        status = OnboardingApplicationDocumentStatus.Completed(classifiedItemCount = 2),
        items =
            listOf(
                OnboardingApplicationItem(
                    id = 1L,
                    categoryLabel = "지원 동기",
                    contentPreview = "사용자에게 닿는 제품을 만들고 싶어 지원했습니다.",
                    needsReview = false,
                ),
                OnboardingApplicationItem(
                    id = 2L,
                    categoryLabel = "기타",
                    contentPreview = "동아리에서 팀장을 맡아 6명과 함께 서비스를 만들며 협업하는 법을 배웠습니다.",
                    needsReview = true,
                ),
            ),
    )
