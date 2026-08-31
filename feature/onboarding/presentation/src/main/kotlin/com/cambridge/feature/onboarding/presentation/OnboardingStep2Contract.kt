package com.cambridge.feature.onboarding.presentation

/** One stable, display-ready job option offered during onboarding. */
public data class OnboardingJobOption(
    public val id: String,
    public val label: String,
) {
    init {
        require(id.isNotBlank()) { "job option id must not be blank" }
        require(label.isNotBlank()) { "job option label must not be blank" }
    }
}

/** Complete rendering state for the second onboarding step. */
public data class OnboardingStep2UiState(
    public val jobOptions: List<OnboardingJobOption>,
    public val selectedJobIds: Set<String> = emptySet(),
    public val interestInput: String = "",
    public val interestTags: List<String> = emptyList(),
    public val maxJobSelections: Int = 3,
    public val isInputEnabled: Boolean = true,
    public val currentStep: Int = 2,
    public val totalSteps: Int = 4,
) {
    init {
        require(totalSteps > 0) { "totalSteps must be positive" }
        require(currentStep in 1..totalSteps) { "currentStep must be within 1..totalSteps" }
        require(maxJobSelections > 0) { "maxJobSelections must be positive" }
        require(jobOptions.map(OnboardingJobOption::id).distinct().size == jobOptions.size) {
            "job option ids must be unique"
        }
        require(jobOptions.map(OnboardingJobOption::label).distinct().size == jobOptions.size) {
            "job option labels must be unique"
        }
        val knownJobIds = jobOptions.mapTo(mutableSetOf(), OnboardingJobOption::id)
        require(selectedJobIds.all(knownJobIds::contains)) {
            "selected job ids must exist in jobOptions"
        }
        require(selectedJobIds.size <= maxJobSelections) {
            "selected job count must not exceed maxJobSelections"
        }
        require(interestTags.all(String::isNotBlank)) {
            "interest tags must not be blank"
        }
        require(interestTags.distinct().size == interestTags.size) {
            "interest tags must be unique"
        }
    }

    /** Whether a job option remains interactive without exceeding the selection limit. */
    public fun isJobOptionEnabled(id: String): Boolean =
        isInputEnabled &&
            (id in selectedJobIds || selectedJobIds.size < maxJobSelections)

    /** Both required sections must have a value before the host can advance. */
    public val isNextEnabled: Boolean
        get() = isInputEnabled && selectedJobIds.isNotEmpty() && interestTags.isNotEmpty()
}

/** User intentions emitted by [OnboardingStep2Screen]. */
public sealed interface OnboardingStep2Event {
    public data class JobSelectionToggled(
        public val jobId: String,
    ) : OnboardingStep2Event

    public data class InterestInputChanged(
        public val value: String,
    ) : OnboardingStep2Event

    public data object InterestTagSubmitted : OnboardingStep2Event

    public data class InterestTagRemoved(
        public val tag: String,
    ) : OnboardingStep2Event

    public data object BackClicked : OnboardingStep2Event

    public data object NextClicked : OnboardingStep2Event
}
