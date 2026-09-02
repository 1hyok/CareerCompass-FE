package com.cambridge.careercompass_fe.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.cambridge.careercompass_fe.R
import com.cambridge.core.ui.component.CareerCompassButton
import com.cambridge.core.ui.component.CareerCompassButtonSize
import com.cambridge.core.ui.component.CareerCompassButtonVariant
import com.cambridge.core.ui.component.CareerCompassCard
import com.cambridge.core.ui.component.CareerCompassEmptyState
import com.cambridge.core.ui.theme.CareerCompassTheme

/**
 * 마이 탭 자리표시자의 진입점 — profile 모듈이 마이 탭을 인수하면 [Route.MyTab] 과 함께 통째로 지운다.
 *
 * 여기 있는 것은 세션 카드와 로그아웃뿐이다. 지문 로그인 끄기·알림 설정·경험 카드 같은 나머지 마이 탭 항목은
 * profile·notification 모듈 몫이라 자리표시자에 넣지 않는다.
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
