package com.cambridge.feature.onboarding.presentation

import androidx.compose.runtime.Immutable

public const val ONBOARDING_MAX_APPLICATION_UPLOAD_COUNT: Int = 10
public const val ONBOARDING_MAX_APPLICATION_FILE_SIZE_MEGABYTES: Int = 10
public const val ONBOARDING_MAX_APPLICATION_FILE_SIZE_BYTES: Long =
    ONBOARDING_MAX_APPLICATION_FILE_SIZE_MEGABYTES * 1024L * 1024L

/** File formats accepted by the past-application uploader. */
public enum class OnboardingApplicationDocumentFormat(
    public val extension: String,
    public val label: String,
) {
    PDF(extension = "pdf", label = "PDF"),
    DOCX(extension = "docx", label = "DOCX"),
    TXT(extension = "txt", label = "TXT"),
    ;

    public companion object {
        /** Resolves a supported format from a file name and rejects every other extension. */
        public fun fromFileName(fileName: String): OnboardingApplicationDocumentFormat {
            val extension = fileName.substringAfterLast(delimiter = '.', missingDelimiterValue = "")
            return entries.firstOrNull { format ->
                format.extension.equals(extension, ignoreCase = true)
            } ?: throw IllegalArgumentException("Unsupported application document format")
        }
    }
}

/** Classification lifecycle for an uploaded application document. */
@Immutable
public sealed interface OnboardingApplicationDocumentStatus {
    /** The document is still being uploaded or classified. */
    public data object Processing : OnboardingApplicationDocumentStatus

    /** Classification completed and produced [classifiedItemCount] reusable entries. */
    @Immutable
    public data class Completed(
        public val classifiedItemCount: Int,
    ) : OnboardingApplicationDocumentStatus {
        init {
            require(classifiedItemCount >= 0) { "classifiedItemCount must not be negative" }
        }
    }

    /** Classification failed with a user-facing [message]. */
    @Immutable
    public data class Failed(
        public val message: String,
    ) : OnboardingApplicationDocumentStatus {
        init {
            require(message.isNotBlank()) { "Failure message must not be blank" }
        }
    }
}

/** A previously submitted application that can be reused during onboarding. */
@Immutable
public data class OnboardingApplicationDocument(
    public val id: String,
    public val fileName: String,
    public val format: OnboardingApplicationDocumentFormat,
    public val fileSizeBytes: Long,
    public val status: OnboardingApplicationDocumentStatus,
) {
    init {
        require(id.isNotBlank()) { "Application document id must not be blank" }
        require(fileName.isNotBlank()) { "Application document fileName must not be blank" }
        require(OnboardingApplicationDocumentFormat.fromFileName(fileName) == format) {
            "Application document extension must match its format"
        }
        require(fileSizeBytes in 1..ONBOARDING_MAX_APPLICATION_FILE_SIZE_BYTES) {
            "Application document fileSizeBytes must be within the upload limit"
        }
    }
}

/** Immutable rendering state for the fourth onboarding step. */
@Immutable
public data class OnboardingStep4UiState(
    public val uploadedDocuments: List<OnboardingApplicationDocument> = emptyList(),
    public val isInputEnabled: Boolean = true,
    public val currentStep: Int = 4,
    public val totalSteps: Int = 4,
) {
    init {
        require(totalSteps > 0) { "totalSteps must be positive" }
        require(currentStep in 1..totalSteps) { "currentStep must be within 1..totalSteps" }
        require(uploadedDocuments.size <= ONBOARDING_MAX_APPLICATION_UPLOAD_COUNT) {
            "uploaded document count must not exceed the upload limit"
        }
        require(
            uploadedDocuments
                .map(OnboardingApplicationDocument::id)
                .distinct()
                .size == uploadedDocuments.size,
        ) {
            "Application document ids must be unique"
        }
    }

    /** Whether another document can be added without exceeding the fixed upload limit. */
    public val isUploadEnabled: Boolean
        get() =
            isInputEnabled &&
                uploadedDocuments.size < ONBOARDING_MAX_APPLICATION_UPLOAD_COUNT

    /** Every uploaded document must finish classification before onboarding can complete. */
    public val isCompleteEnabled: Boolean
        get() =
            isInputEnabled &&
                uploadedDocuments.isNotEmpty() &&
                uploadedDocuments.all { document ->
                    document.status is OnboardingApplicationDocumentStatus.Completed
                }
}

/** User intentions emitted by [OnboardingStep4Screen]. */
public sealed interface OnboardingStep4Event {
    public data object UploadClicked : OnboardingStep4Event

    public data object DirectInputClicked : OnboardingStep4Event

    /** Requests the document action menu, including removal. */
    public data class DocumentMenuClicked(
        public val documentId: String,
    ) : OnboardingStep4Event

    /** Requests classification retry for a failed document. */
    public data class DocumentRetryClicked(
        public val documentId: String,
    ) : OnboardingStep4Event

    public data object BackClicked : OnboardingStep4Event

    public data object SkipClicked : OnboardingStep4Event

    public data object CompleteClicked : OnboardingStep4Event
}
