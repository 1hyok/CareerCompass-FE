package com.careercompass.feature.onboarding.presentation

import androidx.compose.runtime.Immutable

/**
 * Immutable rendering state for the first onboarding step.
 *
 * Required values are validated as trimmed, non-empty strings. Optional values still participate
 * in validation when the host supplies an error for them.
 */
@Immutable
public data class OnboardingStep1UiState(
    public val name: String = "",
    public val school: String = "",
    public val major: String = "",
    public val gradePointAverage: String = "",
    public val graduationDate: String = "",
    public val isInputEnabled: Boolean = true,
    public val nameError: String? = null,
    public val schoolError: String? = null,
    public val majorError: String? = null,
    public val gradePointAverageError: String? = null,
    public val graduationDateError: String? = null,
    public val currentStep: Int = 1,
    public val totalSteps: Int = 4,
) {
    init {
        require(totalSteps > 0) { "totalSteps must be positive" }
        require(currentStep in 1..totalSteps) {
            "currentStep must be within 1..totalSteps"
        }
        require(
            listOfNotNull(
                nameError,
                schoolError,
                majorError,
                gradePointAverageError,
                graduationDateError,
            ).all(String::isNotBlank),
        ) {
            "Field errors must be null or non-blank"
        }
    }

    /** Whether all required values are present and no field has a validation error. */
    public val isNextEnabled: Boolean
        get() =
            isInputEnabled &&
                name.isNotBlank() &&
                school.isNotBlank() &&
                major.isNotBlank() &&
                nameError == null &&
                schoolError == null &&
                majorError == null &&
                gradePointAverageError == null &&
                graduationDateError == null
}

/** User intentions emitted by [OnboardingStep1Content]. */
public sealed interface OnboardingStep1Event {
    public data class NameChanged(
        public val value: String,
    ) : OnboardingStep1Event

    public data class MajorChanged(
        public val value: String,
    ) : OnboardingStep1Event

    public data class GradePointAverageChanged(
        public val value: String,
    ) : OnboardingStep1Event

    public data object SchoolPickerClicked : OnboardingStep1Event

    public data object GraduationDatePickerClicked : OnboardingStep1Event

    public data object BackClicked : OnboardingStep1Event

    public data object NextClicked : OnboardingStep1Event
}
