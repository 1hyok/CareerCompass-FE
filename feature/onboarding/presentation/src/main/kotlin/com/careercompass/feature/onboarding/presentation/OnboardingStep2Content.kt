package com.careercompass.feature.onboarding.presentation

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.careercompass.core.ui.component.CareerCompassTag
import com.careercompass.core.ui.component.CareerCompassTextField
import com.careercompass.core.ui.component.CareerCompassTextFieldSize
import com.careercompass.core.ui.theme.CareerCompassTheme

/** Stateless job-preference and interest-tag form for onboarding step two. */
@Composable
public fun OnboardingStep2Content(
    state: OnboardingStep2UiState,
    onEvent: (OnboardingStep2Event) -> Unit,
    modifier: Modifier = Modifier,
) {
    OnboardingStepScaffold(
        currentStep = state.currentStep,
        totalSteps = state.totalSteps,
        title = stringResource(R.string.onboarding_step2_title),
        description = stringResource(R.string.onboarding_step2_description),
        onBackClick = { onEvent(OnboardingStep2Event.BackClicked) },
        modifier = modifier,
        footerContent = {
            OnboardingPrimaryActionFooter(
                text = stringResource(R.string.onboarding_step2_next),
                enabled = state.isNextEnabled,
                onClick = { onEvent(OnboardingStep2Event.NextClicked) },
            )
        },
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(CareerCompassTheme.spacing.xxLarge)) {
            JobPreferenceSection(state = state, onEvent = onEvent)
            InterestTagSection(state = state, onEvent = onEvent)
        }
    }
}

@Composable
private fun JobPreferenceSection(
    state: OnboardingStep2UiState,
    onEvent: (OnboardingStep2Event) -> Unit,
) {
    val spacing = CareerCompassTheme.spacing

    Column(verticalArrangement = Arrangement.spacedBy(spacing.medium)) {
        Text(
            text = stringResource(R.string.onboarding_step2_job_label),
            color = CareerCompassTheme.colors.onSurface,
            style =
                CareerCompassTheme.typography.labelMedium.copy(
                    fontSize = 13.sp,
                    lineHeight = 19.5.sp,
                    fontWeight = FontWeight.Medium,
                ),
        )
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(spacing.small),
            verticalArrangement = Arrangement.spacedBy(spacing.small),
        ) {
            state.jobOptions.forEach { option ->
                val selected = option.id in state.selectedJobIds
                CareerCompassTag(
                    label = option.label,
                    selected = selected,
                    onClick = {
                        onEvent(OnboardingStep2Event.JobSelectionToggled(option.id))
                    },
                    enabled = state.isJobOptionEnabled(option.id),
                    stateDescription =
                        stringResource(
                            if (selected) {
                                R.string.onboarding_step2_selected_state
                            } else {
                                R.string.onboarding_step2_unselected_state
                            },
                        ),
                    role = Role.Checkbox,
                )
            }
        }
        Text(
            text =
                stringResource(
                    R.string.onboarding_step2_selected_count,
                    state.selectedJobIds.size,
                    state.maxJobSelections,
                ),
            color = CareerCompassTheme.colors.mutedContent,
            style =
                CareerCompassTheme.typography.caption.copy(
                    fontSize = 11.sp,
                    lineHeight = 16.5.sp,
                    fontWeight = FontWeight.Medium,
                ),
        )
    }
}

@Composable
private fun InterestTagSection(
    state: OnboardingStep2UiState,
    onEvent: (OnboardingStep2Event) -> Unit,
) {
    val spacing = CareerCompassTheme.spacing

    Column(verticalArrangement = Arrangement.spacedBy(spacing.medium)) {
        CareerCompassTextField(
            value = state.interestInput,
            onValueChange = { onEvent(OnboardingStep2Event.InterestInputChanged(it)) },
            label = stringResource(R.string.onboarding_step2_interest_label),
            placeholder = stringResource(R.string.onboarding_step2_interest_placeholder),
            enabled = state.isInputEnabled,
            size = CareerCompassTextFieldSize.Large,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            keyboardActions =
                KeyboardActions(
                    onDone = {
                        if (state.isInputEnabled && state.interestInput.isNotBlank()) {
                            onEvent(OnboardingStep2Event.InterestTagSubmitted)
                        }
                    },
                ),
        )
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(spacing.xSmall),
            verticalArrangement = Arrangement.spacedBy(spacing.xSmall),
        ) {
            state.interestTags.forEach { tag ->
                RemovableInterestTag(
                    tag = tag,
                    enabled = state.isInputEnabled,
                    onClick = { onEvent(OnboardingStep2Event.InterestTagRemoved(tag)) },
                )
            }
        }
    }
}

@Composable
private fun RemovableInterestTag(
    tag: String,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    val removeDescription = stringResource(R.string.onboarding_step2_remove_interest, tag)
    val colors = CareerCompassTheme.colors

    Surface(
        modifier =
            Modifier
                .clickable(
                    enabled = enabled,
                    role = Role.Button,
                    onClick = onClick,
                ).semantics { contentDescription = removeDescription },
        shape = CareerCompassTheme.shapes.pill,
        color = if (enabled) colors.successContainer else colors.disabledContainer,
        contentColor = if (enabled) colors.onSuccessContainer else colors.disabledContent,
    ) {
        Text(
            text = stringResource(R.string.onboarding_step2_interest_chip, tag),
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
            maxLines = 1,
            style =
                CareerCompassTheme.typography.caption.copy(
                    fontSize = 12.sp,
                    lineHeight = 18.sp,
                    fontWeight = FontWeight.SemiBold,
                ),
        )
    }
}
