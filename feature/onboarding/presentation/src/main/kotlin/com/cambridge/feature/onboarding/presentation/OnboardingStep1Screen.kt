package com.cambridge.feature.onboarding.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cambridge.core.ui.component.CareerCompassButton
import com.cambridge.core.ui.component.CareerCompassButtonSize
import com.cambridge.core.ui.component.CareerCompassTextField
import com.cambridge.core.ui.component.CareerCompassTextFieldSize
import com.cambridge.core.ui.theme.CareerCompassTheme

/**
 * Stateless first-step onboarding form.
 *
 * The host owns all values and validation. This composable only renders [state] and forwards user
 * intentions through [onEvent].
 */
@Composable
public fun OnboardingStep1Screen(
    state: OnboardingStep1UiState,
    onEvent: (OnboardingStep1Event) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = CareerCompassTheme.colors
    val spacing = CareerCompassTheme.spacing

    Column(
        modifier =
            modifier
                .fillMaxSize()
                .background(colors.subtleSurface),
    ) {
        OnboardingTopBar(onBackClick = { onEvent(OnboardingStep1Event.BackClicked) })
        OnboardingProgress(
            currentStep = state.currentStep,
            totalSteps = state.totalSteps,
        )
        Column(
            modifier =
                Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = spacing.large, vertical = spacing.xxLarge),
        ) {
            OnboardingHeader(
                currentStep = state.currentStep,
                totalSteps = state.totalSteps,
            )
            Spacer(modifier = Modifier.height(spacing.xxLarge))
            OnboardingFields(state = state, onEvent = onEvent)
        }
        OnboardingFooter(
            enabled = state.isNextEnabled,
            onNextClick = { onEvent(OnboardingStep1Event.NextClicked) },
        )
    }
}

@Composable
private fun OnboardingTopBar(onBackClick: () -> Unit) {
    val colors = CareerCompassTheme.colors
    val backDescription = stringResource(R.string.onboarding_step1_back)

    Box(
        modifier =
            Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .height(56.dp),
        contentAlignment = Alignment.CenterStart,
    ) {
        Box(
            modifier =
                Modifier
                    .padding(start = 4.dp)
                    .size(48.dp)
                    .clickable(role = Role.Button, onClick = onBackClick)
                    .semantics {
                        contentDescription = backDescription
                        role = Role.Button
                    },
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = stringResource(R.string.onboarding_step1_back_icon),
                modifier = Modifier.clearAndSetSemantics {},
                color = colors.onSurface,
                style =
                    CareerCompassTheme.typography.headline2.copy(
                        fontSize = 22.sp,
                        lineHeight = 33.sp,
                        fontWeight = FontWeight.Normal,
                    ),
            )
        }
    }
}

@Composable
private fun OnboardingProgress(
    currentStep: Int,
    totalSteps: Int,
) {
    val colors = CareerCompassTheme.colors
    val spacing = CareerCompassTheme.spacing
    val progressDescription =
        stringResource(
            R.string.onboarding_step1_progress_description,
            totalSteps,
            currentStep,
        )

    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .height(4.dp)
                .padding(horizontal = spacing.large)
                .semantics(mergeDescendants = true) {
                    contentDescription = progressDescription
                    progressBarRangeInfo =
                        ProgressBarRangeInfo(
                            current = currentStep.toFloat(),
                            range = 0f..totalSteps.toFloat(),
                            steps = (totalSteps - 1).coerceAtLeast(0),
                        )
                },
        horizontalArrangement = Arrangement.spacedBy(spacing.xSmall),
    ) {
        repeat(totalSteps) { index ->
            Box(
                modifier =
                    Modifier
                        .weight(1f)
                        .height(4.dp)
                        .background(
                            color =
                                if (index < currentStep) {
                                    colors.primaryEmphasis
                                } else {
                                    colors.outline
                                },
                            shape = CareerCompassTheme.shapes.pill,
                        ),
            )
        }
    }
}

@Composable
private fun OnboardingHeader(
    currentStep: Int,
    totalSteps: Int,
) {
    val colors = CareerCompassTheme.colors
    val spacing = CareerCompassTheme.spacing

    Column(verticalArrangement = Arrangement.spacedBy(spacing.small)) {
        Text(
            text =
                stringResource(
                    R.string.onboarding_step1_step_label,
                    currentStep,
                    totalSteps,
                ),
            color = colors.actionPrimary,
            style =
                CareerCompassTheme.typography.caption.copy(
                    fontSize = 11.sp,
                    lineHeight = 16.5.sp,
                    letterSpacing = 0.8.sp,
                    fontWeight = FontWeight.SemiBold,
                ),
        )
        Text(
            text = stringResource(R.string.onboarding_step1_title),
            modifier = Modifier.semantics { heading() },
            color = colors.onSurface,
            style =
                CareerCompassTheme.typography.headline1.copy(
                    lineHeight = 36.sp,
                    letterSpacing = (-0.3).sp,
                ),
        )
        Text(
            text = stringResource(R.string.onboarding_step1_description),
            color = colors.onSurfaceVariant,
            style = CareerCompassTheme.typography.bodyMedium,
        )
    }
}

@Composable
private fun OnboardingFields(
    state: OnboardingStep1UiState,
    onEvent: (OnboardingStep1Event) -> Unit,
) {
    val spacing = CareerCompassTheme.spacing

    Column(verticalArrangement = Arrangement.spacedBy(spacing.large)) {
        CareerCompassTextField(
            value = state.name,
            onValueChange = { onEvent(OnboardingStep1Event.NameChanged(it)) },
            label = stringResource(R.string.onboarding_step1_name_label),
            placeholder = stringResource(R.string.onboarding_step1_name_placeholder),
            errorMessage = state.nameError,
            isError = state.nameError != null,
            enabled = state.isInputEnabled,
            size = CareerCompassTextFieldSize.Large,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
        )
        CareerCompassTextField(
            value = state.school,
            onValueChange = {},
            label = stringResource(R.string.onboarding_step1_school_label),
            placeholder = stringResource(R.string.onboarding_step1_school_placeholder),
            errorMessage = state.schoolError,
            isError = state.schoolError != null,
            enabled = state.isInputEnabled,
            readOnly = true,
            size = CareerCompassTextFieldSize.Large,
            onClick = { onEvent(OnboardingStep1Event.SchoolPickerClicked) },
            trailingIcon = {
                FieldIndicator(text = stringResource(R.string.onboarding_step1_school_indicator))
            },
        )
        CareerCompassTextField(
            value = state.major,
            onValueChange = { onEvent(OnboardingStep1Event.MajorChanged(it)) },
            label = stringResource(R.string.onboarding_step1_major_label),
            placeholder = stringResource(R.string.onboarding_step1_major_placeholder),
            errorMessage = state.majorError,
            isError = state.majorError != null,
            enabled = state.isInputEnabled,
            size = CareerCompassTextFieldSize.Large,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
        )
        CareerCompassTextField(
            value = state.gradePointAverage,
            onValueChange = { onEvent(OnboardingStep1Event.GradePointAverageChanged(it)) },
            label = stringResource(R.string.onboarding_step1_gpa_label),
            placeholder = stringResource(R.string.onboarding_step1_gpa_placeholder),
            supportingText = stringResource(R.string.onboarding_step1_gpa_support),
            errorMessage = state.gradePointAverageError,
            isError = state.gradePointAverageError != null,
            enabled = state.isInputEnabled,
            size = CareerCompassTextFieldSize.Large,
            keyboardOptions =
                KeyboardOptions(
                    keyboardType = KeyboardType.Decimal,
                    imeAction = ImeAction.Next,
                ),
        )
        CareerCompassTextField(
            value = state.graduationDate,
            onValueChange = {},
            label = stringResource(R.string.onboarding_step1_graduation_label),
            placeholder = stringResource(R.string.onboarding_step1_graduation_placeholder),
            errorMessage = state.graduationDateError,
            isError = state.graduationDateError != null,
            enabled = state.isInputEnabled,
            readOnly = true,
            size = CareerCompassTextFieldSize.Large,
            onClick = { onEvent(OnboardingStep1Event.GraduationDatePickerClicked) },
            trailingIcon = {
                FieldIndicator(
                    text = stringResource(R.string.onboarding_step1_graduation_indicator),
                )
            },
        )
    }
}

@Composable
private fun FieldIndicator(text: String) {
    Text(
        text = text,
        modifier = Modifier.clearAndSetSemantics {},
        color = CareerCompassTheme.colors.mutedContent,
        style = CareerCompassTheme.typography.bodyLarge,
    )
}

@Composable
private fun OnboardingFooter(
    enabled: Boolean,
    onNextClick: () -> Unit,
) {
    val colors = CareerCompassTheme.colors
    val spacing = CareerCompassTheme.spacing

    Box(
        modifier =
            Modifier
                .fillMaxWidth()
                .background(colors.subtleSurface)
                .navigationBarsPadding()
                .padding(
                    start = spacing.large,
                    top = spacing.medium,
                    end = spacing.large,
                    bottom = spacing.large,
                ),
    ) {
        CareerCompassButton(
            text = stringResource(R.string.onboarding_step1_next),
            onClick = onNextClick,
            modifier = Modifier.fillMaxWidth(),
            size = CareerCompassButtonSize.Large,
            enabled = enabled,
        )
    }
}
