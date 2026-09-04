package com.cambridge.careercompass_fe.navigation

import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.cambridge.careercompass_fe.R
import com.cambridge.core.ui.component.CareerCompassButton
import com.cambridge.core.ui.component.CareerCompassButtonSize
import com.cambridge.core.ui.component.CareerCompassButtonVariant
import com.cambridge.core.ui.component.CareerCompassCard
import com.cambridge.core.ui.component.CareerCompassEmptyState
import com.cambridge.core.ui.theme.CareerCompassTheme
import com.cambridge.feature.onboarding.presentation.biometric.BiometricEnrollPromptResult
import com.cambridge.feature.onboarding.presentation.biometric.rememberBiometricEnrollPrompt
import com.careercompass.core.model.settings.ThemeMode

/** 지문 로그인 스위치를 찾는 시맨틱 태그 — 라벨은 스위치의 토글 상태까지 병합하지 않아 계측 테스트가 이걸 쓴다. */
internal const val MY_TAB_BIOMETRIC_SWITCH_TAG = "my_tab_biometric_switch"

/** 화면 테마 줄을 찾는 시맨틱 태그 — 라벨과 현재 값이 한 줄에 병합돼 문구만으로는 집기 어렵다. */
internal const val MY_TAB_THEME_ROW_TAG = "my_tab_theme_row"

/**
 * 마이 탭 자리표시자의 진입점 — profile 모듈이 마이 탭을 인수하면 [Route.MyTab] 과 함께 통째로 지운다.
 *
 * 여기 있는 것은 세션 카드와 지문 로그인 스위치·로그아웃뿐이다. 알림 설정·경험 카드 같은 나머지 마이 탭 항목은
 * profile·notification 모듈 몫이라 자리표시자에 넣지 않는다. 지문 스위치가 예외인 이유는 켜는 길(#98)만 있고 끄는
 * 길이 없으면 기기에 남은 등록을 되돌릴 자리가 아예 없기 때문이다(#113).
 *
 * @param onSessionEnded 로그아웃이 끝났다 — 셸이 시작 목적지를 다시 계산해 로그인 화면으로 되돌린다(#82 의 경로).
 */
@Composable
internal fun MyTabPlaceholderEntry(
    onSessionEnded: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: MyTabPlaceholderViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    val sessionEnded = state.sessionEnded
    LaunchedEffect(sessionEnded) {
        if (!sessionEnded) return@LaunchedEffect
        viewModel.onSessionEndedConsumed()
        onSessionEnded()
    }

    // 켜는 방향은 #98 의 등록 프롬프트를 그대로 쓴다. null 이면 이 기기·호스트에서 지문을 등록할 수 없다는 뜻이다.
    val launchEnrollPrompt =
        rememberBiometricEnrollPrompt { result ->
            when (result) {
                BiometricEnrollPromptResult.Succeeded -> viewModel.onBiometricEnrollSucceeded()
                BiometricEnrollPromptResult.Cancelled -> viewModel.onBiometricEnrollCancelled()
                is BiometricEnrollPromptResult.Failed -> viewModel.onBiometricEnrollFailed(result.cause)
            }
        }
    val canEnrollBiometric = launchEnrollPrompt != null
    LaunchedEffect(canEnrollBiometric) {
        viewModel.onBiometricAvailabilityChanged(canEnrollBiometric)
    }

    val enrollPromptRequested = state.isEnrollPromptRequested
    LaunchedEffect(enrollPromptRequested) {
        if (!enrollPromptRequested) return@LaunchedEffect
        viewModel.onEnrollPromptRequestConsumed()
        // 등록할 수 없는 기기에서는 스위치가 잠겨 있어 요청이 오지 않는다. 그래도 오면 잠금이 풀리지 않으므로
        // 취소와 같게 되돌린다.
        val launch = launchEnrollPrompt
        if (launch != null) launch() else viewModel.onBiometricEnrollCancelled()
    }

    MyTabPlaceholderScreen(
        state = state,
        onEvent = viewModel::onEvent,
        modifier = modifier,
    )
}

/**
 * 세션 카드 + 「준비 중」 안내 + 로그아웃.
 *
 * 로그아웃 버튼은 진행 중에 꺼진다 — 확인 다이얼로그를 닫은 뒤에도 요청이 남아 있는 동안 다시 눌리지 않게.
 */
@Composable
internal fun MyTabPlaceholderScreen(
    state: MyTabPlaceholderUiState,
    onEvent: (MyTabPlaceholderEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = CareerCompassTheme.colors
    val spacing = CareerCompassTheme.spacing

    Column(
        modifier =
            modifier
                .fillMaxSize()
                .background(colors.subtleSurface)
                .padding(spacing.large),
        verticalArrangement = Arrangement.spacedBy(spacing.large),
    ) {
        CareerCompassCard(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = stringResource(R.string.placeholder_my_account_label),
                style = CareerCompassTheme.typography.caption,
                color = colors.mutedContent,
            )
            Spacer(modifier = Modifier.height(spacing.xxSmall))
            Text(
                text = state.name ?: stringResource(R.string.placeholder_my_name_unknown),
                style = CareerCompassTheme.typography.headline2,
                color = colors.onSurface,
            )
            Spacer(modifier = Modifier.height(spacing.xxSmall))
            Text(
                text = state.affiliation ?: stringResource(R.string.placeholder_my_affiliation_unknown),
                style = CareerCompassTheme.typography.bodyMedium,
                color = colors.onSurfaceVariant,
            )

            Spacer(modifier = Modifier.height(spacing.medium))
            HorizontalDivider(color = colors.subtleOutline)
            BiometricLoginSetting(state = state, onEvent = onEvent)
            ThemeModeSetting(state = state, onEvent = onEvent)
        }

        CareerCompassEmptyState(
            title = stringResource(R.string.placeholder_my_title),
            description = stringResource(R.string.placeholder_description),
            actionText = null,
            onActionClick = null,
            modifier = Modifier.weight(1f),
        )

        CareerCompassButton(
            text = stringResource(R.string.placeholder_my_logout),
            onClick = { onEvent(MyTabPlaceholderEvent.LogoutClicked) },
            modifier = Modifier.fillMaxWidth(),
            variant = CareerCompassButtonVariant.Secondary,
            size = CareerCompassButtonSize.Large,
            enabled = !state.isLoggingOut,
        )
    }

    if (state.isThemeDialogVisible) {
        ThemeModeDialog(selected = state.themeMode, onEvent = onEvent)
    }

    if (state.isLogoutDialogVisible) {
        AlertDialog(
            onDismissRequest = { onEvent(MyTabPlaceholderEvent.LogoutDismissed) },
            confirmButton = {
                CareerCompassButton(
                    text = stringResource(R.string.placeholder_my_logout_dialog_confirm),
                    onClick = { onEvent(MyTabPlaceholderEvent.LogoutConfirmed) },
                    variant = CareerCompassButtonVariant.Danger,
                    size = CareerCompassButtonSize.Small,
                )
            },
            dismissButton = {
                CareerCompassButton(
                    text = stringResource(R.string.placeholder_my_logout_dialog_cancel),
                    onClick = { onEvent(MyTabPlaceholderEvent.LogoutDismissed) },
                    variant = CareerCompassButtonVariant.Ghost,
                    size = CareerCompassButtonSize.Small,
                )
            },
            title = { Text(text = stringResource(R.string.placeholder_my_logout_dialog_title)) },
            text = { Text(text = stringResource(R.string.placeholder_my_logout_dialog_message)) },
            containerColor = colors.surface,
            titleContentColor = colors.onSurface,
            textContentColor = colors.onSurfaceVariant,
        )
    }
}

/**
 * 지문 빠른 로그인 스위치.
 *
 * 스위치가 그리는 값은 저장소의 등록 상태 하나뿐이라, 프롬프트를 취소했거나 등록이 실패하면 아무것도 되돌리지
 * 않아도 원래 자리에 남는다. 안내 문구는 등록할 수 없는 기기에서만 한 줄 붙는다 — 켜져 있는데 지문을 지운 기기는
 * 스위치를 끌 수 있어야 하므로 그때는 잠그지도 안내하지도 않는다.
 */
@Composable
private fun BiometricLoginSetting(
    state: MyTabPlaceholderUiState,
    onEvent: (MyTabPlaceholderEvent) -> Unit,
) {
    val colors = CareerCompassTheme.colors

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = stringResource(R.string.placeholder_my_biometric_label),
            style = CareerCompassTheme.typography.bodyMedium,
            color = colors.onSurface,
        )
        Switch(
            checked = state.isBiometricEnabled,
            onCheckedChange = { enabled -> onEvent(MyTabPlaceholderEvent.BiometricToggled(enabled)) },
            modifier = Modifier.testTag(MY_TAB_BIOMETRIC_SWITCH_TAG),
            enabled = state.isBiometricSwitchEnabled,
        )
    }
    if (state.isBiometricUnavailableNoticeVisible) {
        Text(
            text = stringResource(R.string.placeholder_my_biometric_unavailable),
            style = CareerCompassTheme.typography.caption,
            color = colors.mutedContent,
        )
    }
}

/**
 * 화면 테마 — 지금 값을 줄에 적고, 누르면 셋 중 하나를 고르는 다이얼로그를 연다.
 *
 * 스위치가 아닌 이유는 값이 셋이기 때문이다. 「시스템 따름」과 「밝게」는 지금 기기가 밝을 때 **같은 화면을
 * 그리지만 뜻이 다르다** — 앞은 기기가 어두워지면 따라 어두워지고 뒤는 그대로 밝다. Boolean 하나로 접으면 그
 * 차이를 되돌릴 자리가 없어진다.
 */
@Composable
private fun ThemeModeSetting(
    state: MyTabPlaceholderUiState,
    onEvent: (MyTabPlaceholderEvent) -> Unit,
) {
    val colors = CareerCompassTheme.colors

    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .clickable { onEvent(MyTabPlaceholderEvent.ThemeClicked) }
                .testTag(MY_TAB_THEME_ROW_TAG),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = stringResource(R.string.placeholder_my_theme_label),
            style = CareerCompassTheme.typography.bodyMedium,
            color = colors.onSurface,
        )
        Text(
            text = stringResource(state.themeMode.labelRes()),
            style = CareerCompassTheme.typography.bodyMedium,
            color = colors.onSurfaceVariant,
        )
    }
}

/** 세 값을 라디오로 고른다. 고르는 즉시 닫히므로 확인 버튼을 두지 않는다 — 되돌리기가 같은 자리에서 한 번이다. */
@Composable
private fun ThemeModeDialog(
    selected: ThemeMode,
    onEvent: (MyTabPlaceholderEvent) -> Unit,
) {
    val colors = CareerCompassTheme.colors
    val spacing = CareerCompassTheme.spacing

    AlertDialog(
        onDismissRequest = { onEvent(MyTabPlaceholderEvent.ThemeDismissed) },
        confirmButton = {
            CareerCompassButton(
                text = stringResource(R.string.placeholder_my_theme_dialog_close),
                onClick = { onEvent(MyTabPlaceholderEvent.ThemeDismissed) },
                variant = CareerCompassButtonVariant.Ghost,
                size = CareerCompassButtonSize.Small,
            )
        },
        title = { Text(text = stringResource(R.string.placeholder_my_theme_dialog_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(spacing.xxSmall)) {
                ThemeMode.entries.forEach { mode ->
                    Row(
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .selectable(
                                    selected = mode == selected,
                                    role = Role.RadioButton,
                                    onClick = { onEvent(MyTabPlaceholderEvent.ThemeSelected(mode)) },
                                ),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        RadioButton(selected = mode == selected, onClick = null)
                        Spacer(modifier = Modifier.width(spacing.xSmall))
                        Text(
                            text = stringResource(mode.labelRes()),
                            style = CareerCompassTheme.typography.bodyMedium,
                            color = colors.onSurface,
                        )
                    }
                }
            }
        },
        containerColor = colors.surface,
        titleContentColor = colors.onSurface,
        textContentColor = colors.onSurfaceVariant,
    )
}

@StringRes
private fun ThemeMode.labelRes(): Int =
    when (this) {
        ThemeMode.System -> R.string.placeholder_my_theme_system
        ThemeMode.Light -> R.string.placeholder_my_theme_light
        ThemeMode.Dark -> R.string.placeholder_my_theme_dark
    }
