package com.careercompass.careercompass_fe.session

import com.careercompass.careercompass_fe.navigation.AppDeepLink
import com.careercompass.core.model.settings.ThemeMode

/*
 * 테스트가 읽기 쉽도록 [MainIntent] 와 [AppShellState] 의 필드를 짧은 손잡이로 감싼다. 프로덕션의 진입점은
 * [MainViewModel.onIntent] 하나이고 상태는 [MainViewModel.uiState] 하나다(#252).
 */

internal val MainViewModel.launch: AppShellLaunch? get() = uiState.value.launch

internal val MainViewModel.pendingDeepLink: AppDeepLink? get() = uiState.value.pendingDeepLink

internal val MainViewModel.themeMode: ThemeMode get() = uiState.value.themeMode

internal fun MainViewModel.onDeepLink(link: AppDeepLink?) = onIntent(MainIntent.DeepLinkReceived(link))

internal fun MainViewModel.consumeDeepLink() = onIntent(MainIntent.ConsumeDeepLink)

internal fun MainViewModel.onSessionEnded(cause: SessionEndCause) = onIntent(MainIntent.SessionEnded(cause))

internal fun MainViewModel.raiseSessionExpiryNotice() = onIntent(MainIntent.RaiseSessionExpiryNotice)

internal fun MainViewModel.consumeSessionExpiryNotice() = onIntent(MainIntent.ConsumeSessionExpiryNotice)
