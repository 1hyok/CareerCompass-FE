package com.cambridge.feature.onboarding.presentation.experience

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import com.cambridge.core.model.experience.ExperienceType
import com.cambridge.core.ui.component.CareerCompassButton
import com.cambridge.core.ui.component.CareerCompassButtonSize
import com.cambridge.core.ui.component.CareerCompassButtonVariant
import com.cambridge.core.ui.component.CareerCompassTag
import com.cambridge.core.ui.component.CareerCompassTextField
import com.cambridge.core.ui.component.CareerCompassTextFieldSize
import com.cambridge.core.ui.theme.CareerCompassTheme
import com.cambridge.feature.onboarding.presentation.R
import com.cambridge.feature.onboarding.presentation.shared.util.toMessage

/**
 * Step 3 「경험 추가」 시트의 본문. 시트 컨테이너는 호스트가 감싼다.
 *
 * 유형 칩을 바꾸면 [ExperienceEditorRules] 에 따라 필드 라벨과 필수 표시가 바뀐다.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
public fun ExperienceQuickAddSheet(
    state: ExperienceEditorState,
    onEvent: (ExperienceQuickAddEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = CareerCompassTheme.colors
    val spacing = CareerCompassTheme.spacing
    val selectedState = stringResource(R.string.onboarding_experience_type_selected_state)
    val unselectedState = stringResource(R.string.onboarding_experience_type_unselected_state)

    Column(
        modifier =
            modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = spacing.large, vertical = spacing.small),
        verticalArrangement = Arrangement.spacedBy(spacing.medium),
    ) {
        Text(
            text = stringResource(R.string.onboarding_experience_add_title),
            modifier = Modifier.semantics { heading() },
            color = colors.onSurface,
            style = CareerCompassTheme.typography.headline4,
        )
        Text(
            text = stringResource(R.string.onboarding_experience_type_section),
            color = colors.onSurfaceVariant,
            style = CareerCompassTheme.typography.labelMedium,
        )
        FlowRow(
            modifier = Modifier.fillMaxWidth().selectableGroup(),
            horizontalArrangement = Arrangement.spacedBy(spacing.small),
            verticalArrangement = Arrangement.spacedBy(spacing.small),
        ) {
            ExperienceType.entries.forEach { type ->
                val selected = type == state.type
                CareerCompassTag(
                    label = stringResource(type.labelResId()),
                    selected = selected,
                    onClick = { onEvent(ExperienceQuickAddEvent.TypeSelected(type)) },
                    enabled = state.isInputEnabled,
                    stateDescription = if (selected) selectedState else unselectedState,
                    role = Role.RadioButton,
                )
            }
        }
        CareerCompassTextField(
            value = state.title,
            onValueChange = { onEvent(ExperienceQuickAddEvent.TitleChanged(it)) },
            label = stringResource(R.string.onboarding_experience_title_label),
            placeholder = stringResource(R.string.onboarding_experience_title_placeholder),
            errorMessage = state.titleError?.let { it.toMessage() },
            isError = state.titleError != null,
            enabled = state.isInputEnabled,
            size = CareerCompassTextFieldSize.Large,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
        )
        Row(horizontalArrangement = Arrangement.spacedBy(spacing.small)) {
            CareerCompassTextField(
                value = state.startDate,
                onValueChange = { onEvent(ExperienceQuickAddEvent.StartDateChanged(it)) },
                label = stringResource(startDateLabelResId(state.type)),
                modifier = Modifier.weight(1f),
                placeholder = stringResource(R.string.onboarding_experience_date_placeholder),
                errorMessage = state.startDateError?.let { it.toMessage() },
                isError = state.startDateError != null,
                enabled = state.isInputEnabled,
                size = CareerCompassTextFieldSize.Large,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Next),
            )
            if (ExperienceEditorRules.hasEndDate(state.type)) {
                CareerCompassTextField(
                    value = state.endDate,
                    onValueChange = { onEvent(ExperienceQuickAddEvent.EndDateChanged(it)) },
                    label = stringResource(R.string.onboarding_experience_end_label),
                    modifier = Modifier.weight(1f),
                    placeholder = stringResource(R.string.onboarding_experience_date_placeholder),
                    errorMessage = state.endDateError?.let { it.toMessage() },
                    isError = state.endDateError != null,
                    enabled = state.isInputEnabled,
                    size = CareerCompassTextFieldSize.Large,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Next),
                )
            }
        }
        CareerCompassTextField(
            value = state.primary,
            onValueChange = { onEvent(ExperienceQuickAddEvent.PrimaryChanged(it)) },
            label = stringResource(primaryLabelResId(state.type)),
            errorMessage = state.primaryError?.let { it.toMessage() },
            isError = state.primaryError != null,
            enabled = state.isInputEnabled,
            size = CareerCompassTextFieldSize.Large,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
        )
        if (ExperienceEditorRules.hasSecondary(state.type)) {
            CareerCompassTextField(
                value = state.secondary,
                onValueChange = { onEvent(ExperienceQuickAddEvent.SecondaryChanged(it)) },
                label = stringResource(secondaryLabelResId(state.type)),
                errorMessage = state.secondaryError?.let { it.toMessage() },
                isError = state.secondaryError != null,
                enabled = state.isInputEnabled,
                size = CareerCompassTextFieldSize.Large,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth().padding(top = spacing.small),
            horizontalArrangement = Arrangement.spacedBy(spacing.small),
        ) {
            CareerCompassButton(
                text = stringResource(R.string.onboarding_sheet_cancel),
                onClick = { onEvent(ExperienceQuickAddEvent.Dismissed) },
                modifier = Modifier.weight(1f),
                variant = CareerCompassButtonVariant.Secondary,
                size = CareerCompassButtonSize.Large,
                enabled = state.isInputEnabled,
            )
            CareerCompassButton(
                text =
                    stringResource(
                        if (state.isSubmitting) R.string.onboarding_experience_submitting else R.string.onboarding_experience_submit,
                    ),
                onClick = { onEvent(ExperienceQuickAddEvent.Submitted) },
                modifier = Modifier.weight(1f),
                size = CareerCompassButtonSize.Large,
                enabled = state.isSubmitEnabled,
            )
        }
    }
}

/** 경험 유형의 화면 라벨 — Step 3 필터와 시트가 같은 문구를 쓴다. */
public fun ExperienceType.labelResId(): Int =
    when (this) {
        ExperienceType.Project -> R.string.onboarding_experience_type_project
        ExperienceType.Award -> R.string.onboarding_experience_type_award
        ExperienceType.Intern -> R.string.onboarding_experience_type_intern
        ExperienceType.Activity -> R.string.onboarding_experience_type_activity
        ExperienceType.Certificate -> R.string.onboarding_experience_type_certificate
    }

private fun startDateLabelResId(type: ExperienceType): Int =
    when {
        type == ExperienceType.Certificate -> R.string.onboarding_experience_acquired_label
        ExperienceEditorRules.isStartDateRequired(type) -> R.string.onboarding_experience_start_label_required
        else -> R.string.onboarding_experience_start_label
    }

private fun primaryLabelResId(type: ExperienceType): Int =
    when (type) {
        ExperienceType.Project -> R.string.onboarding_experience_project_role_label
        ExperienceType.Award -> R.string.onboarding_experience_award_rank_label
        ExperienceType.Intern -> R.string.onboarding_experience_intern_company_label
        ExperienceType.Activity -> R.string.onboarding_experience_activity_organization_label
        ExperienceType.Certificate -> R.string.onboarding_experience_certificate_issuer_label
    }

private fun secondaryLabelResId(type: ExperienceType): Int =
    when (type) {
        ExperienceType.Project -> R.string.onboarding_experience_project_summary_label
        ExperienceType.Award -> R.string.onboarding_experience_award_organizer_label
        ExperienceType.Intern -> R.string.onboarding_experience_intern_role_label
        ExperienceType.Activity -> R.string.onboarding_experience_activity_summary_label
        ExperienceType.Certificate -> R.string.onboarding_experience_activity_summary_label
    }
