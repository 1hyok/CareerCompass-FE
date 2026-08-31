package com.cambridge.feature.onboarding.presentation

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.android.tools.screenshot.PreviewTest
import com.cambridge.core.ui.theme.CareerCompassTheme

@PreviewTest
@Preview(name = "Onboarding Step 4 uploaded", widthDp = 360, heightDp = 800)
@Composable
public fun OnboardingStep4UploadedPreview() {
    CareerCompassTheme {
        OnboardingStep4Screen(
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
        OnboardingStep4Screen(
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
        OnboardingStep4Screen(
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
        OnboardingStep4Screen(
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
        OnboardingStep4Screen(
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

private val sampleApplicationDocument =
    OnboardingApplicationDocument(
        id = "application-1",
        fileName = "2024 카카오 인턴 자소서.pdf",
        format = OnboardingApplicationDocumentFormat.PDF,
        fileSizeBytes = 512L * 1024L,
        status = OnboardingApplicationDocumentStatus.Completed(classifiedItemCount = 4),
    )
