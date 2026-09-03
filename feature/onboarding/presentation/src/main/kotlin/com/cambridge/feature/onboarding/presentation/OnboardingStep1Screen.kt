package com.cambridge.feature.onboarding.presentation

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.cambridge.core.ui.component.CareerCompassTextField
import com.cambridge.core.ui.component.CareerCompassTextFieldSize
import com.cambridge.core.ui.icon.CareerCompassIcons
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
    OnboardingStepScaffold(
        currentStep = state.currentStep,
        totalSteps = state.totalSteps,
        title = stringResource(R.string.onboarding_step1_title),
        description = stringResource(R.string.onboarding_step1_description),
        onBackClick = { onEvent(OnboardingStep1Event.BackClicked) },
        modifier = modifier,
        footerContent = {
            OnboardingPrimaryActionFooter(
                text = stringResource(R.string.onboarding_step1_next),
                enabled = state.isNextEnabled,
                onClick = { onEvent(OnboardingStep1Event.NextClicked) },
            )
        },
    ) {
        OnboardingFields(state = state, onEvent = onEvent)
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
                Icon(
                    imageVector = CareerCompassIcons.ExpandMore,
                    contentDescription = null,
                    modifier = Modifier.size(INDICATOR_SIZE),
                )
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
                CalendarIndicator()
            },
        )
    }
}

@Composable
private fun CalendarIndicator() {
    val color = LocalContentColor.current

    Canvas(
        modifier =
            Modifier
                .size(INDICATOR_SIZE)
                .clearAndSetSemantics {},
    ) {
        val strokeWidth = 1.5.dp.toPx()
        val topInset = 5.dp.toPx()
        val sideInset = 2.dp.toPx()
        val bindingInset = 6.dp.toPx()

        drawRoundRect(
            color = color,
            topLeft = Offset(sideInset, topInset),
            size =
                Size(
                    width = size.width - sideInset * 2,
                    height = size.height - topInset - sideInset,
                ),
            cornerRadius = CornerRadius(2.dp.toPx()),
            style = Stroke(width = strokeWidth),
        )
        drawLine(
            color = color,
            start = Offset(sideInset, 9.dp.toPx()),
            end = Offset(size.width - sideInset, 9.dp.toPx()),
            strokeWidth = strokeWidth,
        )
        drawLine(
            color = color,
            start = Offset(bindingInset, 2.dp.toPx()),
            end = Offset(bindingInset, 7.dp.toPx()),
            strokeWidth = strokeWidth,
        )
        drawLine(
            color = color,
            start = Offset(size.width - bindingInset, 2.dp.toPx()),
            end = Offset(size.width - bindingInset, 7.dp.toPx()),
            strokeWidth = strokeWidth,
        )
    }
}

/** 입력칸 오른쪽 표시(달력·펼침)의 크기. 둘이 나란히 놓이지는 않지만 같은 줄에서 같은 무게로 읽혀야 한다. */
private val INDICATOR_SIZE = 20.dp
