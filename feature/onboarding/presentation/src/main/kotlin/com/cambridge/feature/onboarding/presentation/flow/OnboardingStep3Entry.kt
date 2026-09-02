package com.cambridge.feature.onboarding.presentation.flow

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.cambridge.core.model.experience.Experience
import com.cambridge.core.model.experience.ExperienceDetails
import com.cambridge.core.model.experience.ExperienceType
import com.cambridge.feature.onboarding.presentation.OnboardingExperience
import com.cambridge.feature.onboarding.presentation.OnboardingExperienceType
import com.cambridge.feature.onboarding.presentation.OnboardingStep3Event
import com.cambridge.feature.onboarding.presentation.OnboardingStep3Screen
import com.cambridge.feature.onboarding.presentation.OnboardingStep3UiState
import com.cambridge.feature.onboarding.presentation.R
import com.cambridge.feature.onboarding.presentation.experience.ExperienceQuickAddEvent
import com.cambridge.feature.onboarding.presentation.experience.ExperienceQuickAddSheet
import com.cambridge.feature.onboarding.presentation.experience.labelResId
import com.cambridge.feature.onboarding.presentation.flow.component.OnboardingFlowFailureHost
import com.cambridge.feature.onboarding.presentation.flow.component.OnboardingSheetHost
import java.time.LocalDate
import java.time.format.DateTimeFormatter

/** Step 3(경험) 화면의 상태 배선. [viewModel] 은 그래프 스코프 [OnboardingViewModel] 이어야 한다. */
@Composable
public fun OnboardingStep3Entry(
    viewModel: OnboardingViewModel,
    onNavigate: (OnboardingDestination) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    ConsumePendingNavigation(
        destination = state.pendingNavigation,
        onNavigate = onNavigate,
        onConsumed = viewModel::onNavigationConsumed,
    )

    OnboardingFlowFailureHost(
        failure = state.failure,
        onDismiss = viewModel::onFailureConsumed,
        modifier = modifier,
    ) {
        OnboardingStep3Screen(
            state = state.step3.toUiState(isInputEnabled = state.isInputEnabled),
            onEvent = { event ->
                if (event == OnboardingStep3Event.BackClicked) onBack() else viewModel.onStep3Event(event)
            },
        )
    }

    state.experienceEditor?.let { editor ->
        OnboardingSheetHost(onDismissRequest = { viewModel.onExperienceEditorEvent(ExperienceQuickAddEvent.Dismissed) }) {
            ExperienceQuickAddSheet(state = editor, onEvent = viewModel::onExperienceEditorEvent)
        }
    }
}

@Composable
private fun OnboardingStep3FormState.toUiState(isInputEnabled: Boolean): OnboardingStep3UiState =
    OnboardingStep3UiState(
        experienceTypes =
            ExperienceType.entries.map { type ->
                OnboardingExperienceType(id = type.wireValue, label = stringResource(type.labelResId()))
            },
        selectedExperienceTypeId = selectedType.wireValue,
        experiences = experiences.map { it.toUiModel() },
        isInputEnabled = isInputEnabled,
    )

@Composable
private fun Experience.toUiModel(): OnboardingExperience =
    OnboardingExperience(
        id = id.toString(),
        typeId = type.wireValue,
        title = title,
        period = periodText(),
        role = roleText(),
        tags = (details as? ExperienceDetails.Project)?.techs.orEmpty(),
    )

@Composable
private fun Experience.periodText(): String {
    val unknown = stringResource(R.string.onboarding_experience_period_unknown)
    return when (val details = details) {
        is ExperienceDetails.Certificate -> {
            details.acquiredYearMonth?.replace('-', '.') ?: startDate?.toYearMonthText() ?: unknown
        }

        is ExperienceDetails.Award -> {
            details.year?.toString() ?: startDate?.toYearMonthText() ?: unknown
        }

        else -> {
            val start = startDate?.toYearMonthText() ?: return unknown
            val end = endDate?.toYearMonthText() ?: stringResource(R.string.onboarding_experience_period_ongoing)
            stringResource(R.string.onboarding_experience_period_range, start, end)
        }
    }
}

@Composable
private fun Experience.roleText(): String =
    when (val details = details) {
        is ExperienceDetails.Project -> details.role ?: stringResource(R.string.onboarding_experience_role_fallback_project)
        is ExperienceDetails.Award -> details.rank
        is ExperienceDetails.Intern -> stringResource(R.string.onboarding_experience_role_intern, details.company, details.role)
        is ExperienceDetails.Activity -> details.role ?: details.organization
        is ExperienceDetails.Certificate -> details.issuer ?: stringResource(R.string.onboarding_experience_role_fallback_certificate)
    }

private val YEAR_MONTH_FORMATTER: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy.MM")

private fun LocalDate.toYearMonthText(): String = format(YEAR_MONTH_FORMATTER)
