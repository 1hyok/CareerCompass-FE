package com.cambridge.feature.onboarding.presentation.flow

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.cambridge.core.model.application.PastApplicationFileFormat
import com.cambridge.core.model.application.PastApplicationItem
import com.cambridge.feature.onboarding.presentation.OnboardingApplicationDocument
import com.cambridge.feature.onboarding.presentation.OnboardingApplicationDocumentFormat
import com.cambridge.feature.onboarding.presentation.OnboardingApplicationDocumentStatus
import com.cambridge.feature.onboarding.presentation.OnboardingApplicationItem
import com.cambridge.feature.onboarding.presentation.OnboardingStep4Event
import com.cambridge.feature.onboarding.presentation.OnboardingStep4Screen
import com.cambridge.feature.onboarding.presentation.OnboardingStep4UiState
import com.cambridge.feature.onboarding.presentation.flow.component.OnboardingFlowFailureHost
import com.cambridge.feature.onboarding.presentation.flow.util.UploadFileSelectionException
import com.cambridge.feature.onboarding.presentation.flow.util.readUploadFile
import com.cambridge.feature.onboarding.presentation.pastapplication.DirectInputEvent
import com.cambridge.feature.onboarding.presentation.pastapplication.DirectInputSheet
import com.cambridge.feature.onboarding.presentation.pastapplication.PastApplicationItemCategoryEvent
import com.cambridge.feature.onboarding.presentation.pastapplication.PastApplicationItemCategorySheet
import com.cambridge.feature.onboarding.presentation.pastapplication.UploadLabelEvent
import com.cambridge.feature.onboarding.presentation.pastapplication.UploadLabelSheet
import com.cambridge.feature.onboarding.presentation.pastapplication.labelResId
import com.cambridge.feature.onboarding.presentation.shared.component.OnboardingSheetHost

/**
 * Step 4(과거 지원서) 화면의 상태 배선. 파일 선택(SAF)은 여기서 열고, 읽은 [com.cambridge.core.model.application.UploadFile]
 * 만 [viewModel] 에 넘긴다. [viewModel] 은 그래프 스코프 [OnboardingViewModel] 이어야 한다.
 */
@Composable
public fun OnboardingStep4Entry(
    viewModel: OnboardingViewModel,
    onNavigate: (OnboardingDestination) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val contentResolver = LocalContext.current.contentResolver
    ConsumePendingNavigation(
        destination = state.pendingNavigation,
        onNavigate = onNavigate,
        onConsumed = viewModel::onNavigationConsumed,
    )

    val documentPicker =
        rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
            if (uri == null) return@rememberLauncherForActivityResult
            readUploadFile(contentResolver, uri)
                .onSuccess(viewModel::onFileSelected)
                .onFailure { throwable ->
                    val reason = (throwable as? UploadFileSelectionException)?.reason ?: OnboardingFailureReason.Unknown
                    viewModel.onFileSelectionFailed(reason, throwable)
                }
        }

    OnboardingFlowFailureHost(
        failure = state.failure,
        onDismiss = viewModel::onFailureConsumed,
        modifier = modifier,
    ) {
        OnboardingStep4Screen(
            state = state.step4.toUiState(isInputEnabled = state.isInputEnabled),
            onEvent = { event ->
                when (event) {
                    OnboardingStep4Event.BackClicked -> onBack()
                    OnboardingStep4Event.UploadClicked -> documentPicker.launch(SUPPORTED_MIME_TYPES)
                    else -> viewModel.onStep4Event(event)
                }
            },
        )
    }

    state.directInput?.let { input ->
        OnboardingSheetHost(onDismissRequest = { viewModel.onDirectInputEvent(DirectInputEvent.Dismissed) }) {
            DirectInputSheet(state = input, onEvent = viewModel::onDirectInputEvent)
        }
    }

    state.uploadLabel?.let { sheet ->
        OnboardingSheetHost(onDismissRequest = { viewModel.onUploadLabelEvent(UploadLabelEvent.Dismissed) }) {
            UploadLabelSheet(state = sheet, onEvent = viewModel::onUploadLabelEvent)
        }
    }

    state.itemCategoryPicker?.let { picker ->
        OnboardingSheetHost(
            onDismissRequest = { viewModel.onItemCategoryPickerEvent(PastApplicationItemCategoryEvent.Dismissed) },
        ) {
            PastApplicationItemCategorySheet(state = picker, onEvent = viewModel::onItemCategoryPickerEvent)
        }
    }
}

@Composable
private fun OnboardingStep4FormState.toUiState(isInputEnabled: Boolean): OnboardingStep4UiState =
    OnboardingStep4UiState(
        uploadedDocuments = documents.map { it.toUiModel() },
        expandedDocumentId = expandedDocumentId,
        isInputEnabled = isInputEnabled,
    )

/**
 * 라벨은 사용자가 정한 이름이라 형식을 담고 있지 않다. 형식 배지는 올린 파일의 실제 이름에서 읽고, 파일이 없는
 * 서버 목록의 문서는 라벨에서 읽는다(API_SPEC §4 는 라벨만 돌려준다 — 확장자가 없으면 직접 입력 문서라 TXT).
 * 카드 제목은 「라벨.확장자」다: 기본 라벨을 그대로 둔 업로드는 파일명을 라벨로 쓰던 이전과 같은 문구가 된다.
 * 크기는 화면에 그리지 않는 값이라 계약 하한으로 채운다.
 */
@Composable
private fun OnboardingUploadDocument.toUiModel(): OnboardingApplicationDocument {
    val format = PastApplicationFileFormat.fromFileName(file?.fileName ?: label) ?: PastApplicationFileFormat.Txt
    val extension = format.extension
    val fileName = if (label.endsWith(".$extension", ignoreCase = true)) label else "$label.$extension"
    val status = status
    return OnboardingApplicationDocument(
        id = id,
        fileName = fileName,
        format = format.toUiFormat(),
        fileSizeBytes = sizeBytes ?: UNKNOWN_SIZE_PLACEHOLDER_BYTES,
        status =
            when (status) {
                OnboardingUploadStatus.Processing -> OnboardingApplicationDocumentStatus.Processing
                is OnboardingUploadStatus.Completed -> OnboardingApplicationDocumentStatus.Completed(status.classifiedItemCount)
                is OnboardingUploadStatus.Failed -> OnboardingApplicationDocumentStatus.Failed(status.reason.toShortMessage())
            },
        items = (status as? OnboardingUploadStatus.Completed)?.items.orEmpty().map { it.toUiModel() },
    )
}

/** 본문은 카드 아래 미리보기라 원문을 그대로 넘기고, 줄 수 제한은 화면이 한다. */
@Composable
private fun PastApplicationItem.toUiModel(): OnboardingApplicationItem =
    OnboardingApplicationItem(
        id = id,
        categoryLabel = stringResource(category.labelResId()),
        contentPreview = content,
        needsReview = !confident,
    )

private fun PastApplicationFileFormat.toUiFormat(): OnboardingApplicationDocumentFormat =
    when (this) {
        PastApplicationFileFormat.Pdf -> OnboardingApplicationDocumentFormat.PDF
        PastApplicationFileFormat.Docx -> OnboardingApplicationDocumentFormat.DOCX
        PastApplicationFileFormat.Txt -> OnboardingApplicationDocumentFormat.TXT
    }

private val SUPPORTED_MIME_TYPES: Array<String> = PastApplicationFileFormat.entries.map { it.mimeType }.toTypedArray()

private const val UNKNOWN_SIZE_PLACEHOLDER_BYTES = 1L
