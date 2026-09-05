package com.careercompass.feature.onboarding.presentation.biometric

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.careercompass.core.ui.component.CareerCompassButton
import com.careercompass.core.ui.component.CareerCompassButtonSize
import com.careercompass.core.ui.component.CareerCompassButtonVariant
import com.careercompass.core.ui.theme.CareerCompassTheme
import com.careercompass.feature.onboarding.presentation.R
import com.careercompass.feature.onboarding.presentation.shared.component.OnboardingErrorCard

/**
 * 지문 빠른 로그인을 켤지 한 번 묻는 시트의 본문 — stateless 층. 시트 컨테이너는 [BiometricEnrollGate] 가 감싼다.
 *
 * 「나중에」는 취소가 아니라 **다시 묻지 않겠다는 답**이다([BiometricEnrollViewModel]). 그래서 두 버튼 다 결론이고,
 * 둘 중 무엇을 골라도 원래 가던 화면으로 이어진다.
 *
 * @param onEnrollClick 프롬프트는 FragmentActivity 에 매여 있어 stateful 층이 띄운다 — Intent 가 아니라 콜백으로 남는
 *   유일한 상호작용이다(`docs/convention/mvi.md`).
 */
@Composable
public fun BiometricEnrollSheet(
    state: BiometricEnrollUiState,
    onIntent: (BiometricEnrollIntent) -> Unit,
    onEnrollClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = CareerCompassTheme.colors
    val spacing = CareerCompassTheme.spacing
    val errorMessage = state.failure?.toMessage()

    Column(
        modifier =
            modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = spacing.large, vertical = spacing.small),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(spacing.medium),
    ) {
        BiometricEnrollBadge()
        Text(
            text = stringResource(R.string.onboarding_biometric_enroll_title),
            modifier = Modifier.semantics { heading() },
            color = colors.onSurface,
            textAlign = TextAlign.Center,
            style = CareerCompassTheme.typography.headline4,
        )
        Text(
            text = stringResource(R.string.onboarding_biometric_enroll_description),
            color = colors.mutedContent,
            textAlign = TextAlign.Center,
            style = CareerCompassTheme.typography.bodyMedium,
        )
        if (errorMessage != null) {
            OnboardingErrorCard(
                message = errorMessage,
                onDismissClick = { onIntent(BiometricEnrollIntent.ConsumeFailure) },
            )
        }
        CareerCompassButton(
            text =
                stringResource(
                    if (state.isRegistering) {
                        R.string.onboarding_biometric_enroll_registering
                    } else {
                        R.string.onboarding_biometric_enroll_confirm
                    },
                ),
            onClick = onEnrollClick,
            modifier = Modifier.fillMaxWidth(),
            variant = CareerCompassButtonVariant.Primary,
            size = CareerCompassButtonSize.Large,
            enabled = state.isActionEnabled,
        )
        CareerCompassButton(
            text = stringResource(R.string.onboarding_biometric_enroll_later),
            onClick = { onIntent(BiometricEnrollIntent.Decline) },
            modifier = Modifier.fillMaxWidth(),
            variant = CareerCompassButtonVariant.Ghost,
            size = CareerCompassButtonSize.Large,
            enabled = state.isActionEnabled,
        )
    }
}

@Composable
private fun BiometricEnrollBadge() {
    val colors = CareerCompassTheme.colors
    val fontScale = LocalDensity.current.fontScale

    Box(
        modifier =
            Modifier
                .size(BADGE_SIZE)
                .background(color = colors.primaryContainer, shape = CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = stringResource(R.string.onboarding_biometric_action_icon),
            modifier = Modifier.clearAndSetSemantics {},
            fontSize = (BADGE_ICON_SIZE_SP / fontScale).sp,
            lineHeight = (BADGE_ICON_LINE_HEIGHT_SP / fontScale).sp,
        )
    }
}

private val BADGE_SIZE = 64.dp

private const val BADGE_ICON_SIZE_SP: Float = 28f

private const val BADGE_ICON_LINE_HEIGHT_SP: Float = 34f
