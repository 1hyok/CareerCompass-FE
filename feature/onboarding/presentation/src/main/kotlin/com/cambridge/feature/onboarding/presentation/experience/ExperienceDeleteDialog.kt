package com.cambridge.feature.onboarding.presentation.experience

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.cambridge.core.ui.component.CareerCompassButton
import com.cambridge.core.ui.component.CareerCompassButtonSize
import com.cambridge.core.ui.component.CareerCompassButtonVariant
import com.cambridge.core.ui.theme.CareerCompassTheme
import com.cambridge.feature.onboarding.presentation.R

/**
 * Step 3 카드 삭제 확인 다이얼로그 — 삭제는 서버 카드까지 지워 되돌릴 수 없다(F1-3).
 *
 * 목록에서 낙관적으로 먼저 빼기 때문에, 실수를 되돌릴 마지막 지점이 이 다이얼로그다.
 */
@Composable
public fun ExperienceDeleteDialog(
    state: ExperienceDeleteState,
    onEvent: (ExperienceDeleteEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    AlertDialog(
        onDismissRequest = { onEvent(ExperienceDeleteEvent.Dismissed) },
        confirmButton = {
            CareerCompassButton(
                text = stringResource(R.string.onboarding_experience_delete_confirm),
                onClick = { onEvent(ExperienceDeleteEvent.Confirmed) },
                variant = CareerCompassButtonVariant.Danger,
                size = CareerCompassButtonSize.Small,
            )
        },
        modifier = modifier,
        dismissButton = {
            CareerCompassButton(
                text = stringResource(R.string.onboarding_sheet_cancel),
                onClick = { onEvent(ExperienceDeleteEvent.Dismissed) },
                variant = CareerCompassButtonVariant.Ghost,
                size = CareerCompassButtonSize.Small,
            )
        },
        title = { Text(text = stringResource(R.string.onboarding_experience_delete_title)) },
        text = { Text(text = stringResource(R.string.onboarding_experience_delete_message, state.title)) },
        containerColor = CareerCompassTheme.colors.surface,
        titleContentColor = CareerCompassTheme.colors.onSurface,
        textContentColor = CareerCompassTheme.colors.onSurfaceVariant,
    )
}
