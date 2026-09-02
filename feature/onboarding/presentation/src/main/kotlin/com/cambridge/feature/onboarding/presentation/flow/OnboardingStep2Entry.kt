package com.cambridge.feature.onboarding.presentation.flow

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.cambridge.feature.onboarding.domain.model.JobOptionCatalog
import com.cambridge.feature.onboarding.presentation.OnboardingJobOption
import com.cambridge.feature.onboarding.presentation.OnboardingStep2Event
import com.cambridge.feature.onboarding.presentation.OnboardingStep2Screen
import com.cambridge.feature.onboarding.presentation.OnboardingStep2UiState
import com.cambridge.feature.onboarding.presentation.flow.component.OnboardingFlowFailureHost

/** Step 2(희망 직무·관심 분야) 화면의 상태 배선. [viewModel] 은 그래프 스코프 [OnboardingViewModel] 이어야 한다. */
@Composable
public fun OnboardingStep2Entry(
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
        OnboardingStep2Screen(
            state = state.step2.toUiState(isInputEnabled = state.isInputEnabled),
            onEvent = { event ->
                if (event == OnboardingStep2Event.BackClicked) onBack() else viewModel.onStep2Event(event)
            },
        )
    }
}

internal fun OnboardingStep2FormState.toUiState(isInputEnabled: Boolean): OnboardingStep2UiState =
    OnboardingStep2UiState(
        jobOptions = JobOptionCatalog.options.map { OnboardingJobOption(id = it.code, label = it.label) },
        selectedJobIds = selectedJobCodes.toSet(),
        interestInput = interestInput,
        interestTags = interestTags,
        isInputEnabled = isInputEnabled,
    )
