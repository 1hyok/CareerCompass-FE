package com.cambridge.feature.onboarding.presentation

import androidx.compose.runtime.Immutable

/** A localized, stable filter shown above the onboarding experience list. */
@Immutable
public data class OnboardingExperienceType(
    public val id: String,
    public val label: String,
) {
    init {
        require(id.isNotBlank()) { "Experience type id must not be blank" }
        require(label.isNotBlank()) { "Experience type label must not be blank" }
    }
}

/** Display-ready data for one experience card. */
public data class OnboardingExperience(
    public val id: String,
    public val typeId: String,
    public val title: String,
    public val period: String,
    public val role: String,
    public val tags: List<String>,
) {
    init {
        require(id.isNotBlank()) { "Experience id must not be blank" }
        require(typeId.isNotBlank()) { "Experience type id must not be blank" }
        require(title.isNotBlank()) { "Experience title must not be blank" }
        require(period.isNotBlank()) { "Experience period must not be blank" }
        require(role.isNotBlank()) { "Experience role must not be blank" }
        require(tags.all(String::isNotBlank)) { "Experience tags must not be blank" }
        require(tags.distinct().size == tags.size) { "Experience tags must be unique" }
    }
}

/** Immutable rendering state for the third onboarding step. */
public data class OnboardingStep3UiState(
    public val experienceTypes: List<OnboardingExperienceType>,
    public val selectedExperienceTypeId: String,
    public val experiences: List<OnboardingExperience> = emptyList(),
    public val isInputEnabled: Boolean = true,
    public val currentStep: Int = 3,
    public val totalSteps: Int = 4,
) {
    init {
        require(totalSteps > 0) { "totalSteps must be positive" }
        require(currentStep in 1..totalSteps) { "currentStep must be within 1..totalSteps" }
        require(experienceTypes.isNotEmpty()) { "Experience types must not be empty" }
        require(experienceTypes.map(OnboardingExperienceType::id).distinct().size == experienceTypes.size) {
            "Experience type ids must be unique"
        }
        require(experienceTypes.any { it.id == selectedExperienceTypeId }) {
            "Selected experience type must be present in experienceTypes"
        }
        require(experiences.map(OnboardingExperience::id).distinct().size == experiences.size) {
            "Experience ids must be unique"
        }
        require(experiences.all { experience -> experienceTypes.any { it.id == experience.typeId } }) {
            "Every experience type must be present in experienceTypes"
        }
    }

    /** Experiences matching the currently selected filter. */
    public val visibleExperiences: List<OnboardingExperience>
        get() = experiences.filter { it.typeId == selectedExperienceTypeId }

    /** Whether onboarding can advance with at least one saved experience. */
    public val isNextEnabled: Boolean
        get() = isInputEnabled && experiences.isNotEmpty()
}

/** User intentions emitted by [OnboardingStep3Screen]. */
public sealed interface OnboardingStep3Event {
    public data class ExperienceTypeSelected(
        public val typeId: String,
    ) : OnboardingStep3Event

    public data class ExperienceSelected(
        public val experienceId: String,
    ) : OnboardingStep3Event

    public data object AddExperienceClicked : OnboardingStep3Event

    public data object BackClicked : OnboardingStep3Event

    public data object NextClicked : OnboardingStep3Event
}
