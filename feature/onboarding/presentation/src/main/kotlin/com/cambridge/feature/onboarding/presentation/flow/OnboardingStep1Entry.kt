package com.cambridge.feature.onboarding.presentation.flow

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.cambridge.feature.onboarding.presentation.OnboardingStep1Event
import com.cambridge.feature.onboarding.presentation.OnboardingStep1Screen
import com.cambridge.feature.onboarding.presentation.OnboardingStep1UiState
import com.cambridge.feature.onboarding.presentation.R
import com.cambridge.feature.onboarding.presentation.basicinfo.GraduationDatePickerEvent
import com.cambridge.feature.onboarding.presentation.basicinfo.GraduationDatePickerSheet
import com.cambridge.feature.onboarding.presentation.basicinfo.SchoolPickerEvent
import com.cambridge.feature.onboarding.presentation.basicinfo.SchoolPickerSheet
import com.cambridge.feature.onboarding.presentation.flow.component.OnboardingFlowFailureHost
import com.cambridge.feature.onboarding.presentation.shared.component.OnboardingSheetHost
import com.cambridge.feature.onboarding.presentation.shared.model.OnboardingFieldError
import com.cambridge.feature.onboarding.presentation.shared.util.toMessage

/**
 * Step 1(기본 정보) 화면의 상태 배선. [viewModel] 은 그래프 스코프 [OnboardingViewModel] 이어야 한다 —
 * 단계마다 새로 만들면 진행 상태가 끊긴다.
 *
 * @param onSessionEnded 401 로 세션이 끝났다 — 앱 셸이 사유를 만료로 갈라 로그인 화면으로 보낸다(#211).
 */
@Composable
public fun OnboardingStep1Entry(
    viewModel: OnboardingViewModel,
    onNavigate: (OnboardingDestination) -> Unit,
    onBack: () -> Unit,
    onSessionEnded: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    ConsumePendingNavigation(
        destination = state.pendingNavigation,
        onNavigate = onNavigate,
        onConsumed = viewModel::onNavigationConsumed,
    )
    ConsumeSessionEnd(
        sessionEnded = state.sessionEnded,
        onSessionEnded = onSessionEnded,
        onConsumed = viewModel::onSessionEndedConsumed,
    )

    OnboardingFlowFailureHost(
        failure = state.failure,
        onDismiss = viewModel::onFailureConsumed,
        modifier = modifier,
    ) {
        OnboardingStep1Screen(
            state = state.step1.toUiState(isInputEnabled = state.isInputEnabled),
            onEvent = { event ->
                if (event == OnboardingStep1Event.BackClicked) onBack() else viewModel.onStep1Event(event)
            },
        )
    }

    state.schoolPicker?.let { picker ->
        OnboardingSheetHost(onDismissRequest = { viewModel.onSchoolPickerEvent(SchoolPickerEvent.Dismissed) }) {
            SchoolPickerSheet(state = picker, onEvent = viewModel::onSchoolPickerEvent)
        }
    }
    state.graduationPicker?.let { picker ->
        OnboardingSheetHost(onDismissRequest = { viewModel.onGraduationPickerEvent(GraduationDatePickerEvent.Dismissed) }) {
            GraduationDatePickerSheet(state = picker, onEvent = viewModel::onGraduationPickerEvent)
        }
    }
}

@Composable
private fun OnboardingStep1FormState.toUiState(isInputEnabled: Boolean): OnboardingStep1UiState =
    OnboardingStep1UiState(
        name = name,
        school = school,
        major = major,
        gradePointAverage = gradePointAverage,
        graduationDate = graduationDate,
        isInputEnabled = isInputEnabled,
        nameError = nameError?.let { it.toMessage() },
        schoolError = schoolError?.let { it.toMessage() },
        majorError = majorError?.let { it.toMessage() },
        gradePointAverageError = gradePointAverageError?.let { gradePointAverageMessage(it) },
        graduationDateError = graduationDateError?.let { graduationDateMessage(it) },
    )

@Composable
private fun gradePointAverageMessage(error: OnboardingFieldError): String =
    when (error) {
        OnboardingFieldError.InvalidFormat -> stringResource(R.string.onboarding_step1_gpa_invalid)
        OnboardingFieldError.OutOfRange -> stringResource(R.string.onboarding_step1_gpa_out_of_range)
        else -> error.toMessage()
    }

@Composable
private fun graduationDateMessage(error: OnboardingFieldError): String =
    when (error) {
        OnboardingFieldError.InvalidFormat -> stringResource(R.string.onboarding_step1_graduation_invalid)
        OnboardingFieldError.OutOfRange -> stringResource(R.string.onboarding_step1_graduation_out_of_range)
        else -> error.toMessage()
    }
