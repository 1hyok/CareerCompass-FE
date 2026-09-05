package com.careercompass.careercompass_fe.navigation

import kotlinx.coroutines.flow.StateFlow

/*
 * 테스트가 읽기 쉽도록 [MyTabPlaceholderIntent] 를 짧은 손잡이로 감싼다. 프로덕션의 진입점은
 * [MyTabPlaceholderViewModel.onIntent] 하나다(#252).
 */

internal val MyTabPlaceholderViewModel.state: StateFlow<MyTabPlaceholderUiState> get() = uiState

internal fun MyTabPlaceholderViewModel.onEvent(event: MyTabPlaceholderEvent) = onIntent(MyTabPlaceholderIntent.Screen(event))

internal fun MyTabPlaceholderViewModel.onBiometricAvailabilityChanged(canEnroll: Boolean) =
    onIntent(MyTabPlaceholderIntent.BiometricAvailabilityChanged(canEnroll))

internal fun MyTabPlaceholderViewModel.onEnrollPromptRequestConsumed() = onIntent(MyTabPlaceholderIntent.ConsumeEnrollPromptRequest)

internal fun MyTabPlaceholderViewModel.onBiometricEnrollSucceeded() = onIntent(MyTabPlaceholderIntent.BiometricEnrollSucceeded)

internal fun MyTabPlaceholderViewModel.onBiometricEnrollCancelled() = onIntent(MyTabPlaceholderIntent.BiometricEnrollCancelled)

internal fun MyTabPlaceholderViewModel.onBiometricEnrollFailed(cause: Throwable) =
    onIntent(MyTabPlaceholderIntent.BiometricEnrollFailed(cause))

internal fun MyTabPlaceholderViewModel.onSessionEndedConsumed() = onIntent(MyTabPlaceholderIntent.ConsumeSessionEnded)
