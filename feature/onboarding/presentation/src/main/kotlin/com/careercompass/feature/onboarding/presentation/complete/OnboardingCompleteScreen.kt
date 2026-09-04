package com.careercompass.feature.onboarding.presentation.complete

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.careercompass.core.ui.component.CareerCompassButton
import com.careercompass.core.ui.component.CareerCompassButtonSize
import com.careercompass.core.ui.component.CareerCompassButtonVariant
import com.careercompass.core.ui.icon.CareerCompassIcons
import com.careercompass.core.ui.theme.CareerCompassTheme
import com.careercompass.feature.onboarding.presentation.R
import com.careercompass.feature.onboarding.presentation.shared.component.OnboardingCenteredLayout

/** Stateless completion screen shown once every onboarding step is done. */
@Composable
public fun OnboardingCompleteScreen(
    state: OnboardingCompleteUiState,
    onEvent: (OnboardingCompleteEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    OnboardingCenteredLayout(
        topContent = null,
        modifier = modifier,
        centerContent = { OnboardingCompleteMessage(userName = state.userName) },
        bottomContent = {
            CareerCompassButton(
                text = stringResource(R.string.onboarding_complete_view_feed),
                onClick = { onEvent(OnboardingCompleteEvent.ViewFeedClicked) },
                modifier = Modifier.fillMaxWidth(),
                variant = CareerCompassButtonVariant.Primary,
                size = CareerCompassButtonSize.Large,
            )
            CareerCompassButton(
                text = stringResource(R.string.onboarding_complete_register_board),
                onClick = { onEvent(OnboardingCompleteEvent.RegisterBoardClicked) },
                variant = CareerCompassButtonVariant.Ghost,
                size = CareerCompassButtonSize.Large,
            )
        },
    )
}

@Composable
private fun OnboardingCompleteMessage(userName: String?) {
    val colors = CareerCompassTheme.colors
    val spacing = CareerCompassTheme.spacing
    val message =
        if (userName != null) {
            stringResource(R.string.onboarding_complete_message_named, userName)
        } else {
            stringResource(R.string.onboarding_complete_message)
        }

    CompletionBadge()
    Spacer(modifier = Modifier.height(spacing.xxLarge))
    Text(
        text = stringResource(R.string.onboarding_complete_title),
        modifier = Modifier.semantics { heading() },
        color = colors.onSurface,
        textAlign = TextAlign.Center,
        style = CareerCompassTheme.typography.headline1,
    )
    Spacer(modifier = Modifier.height(spacing.small))
    Text(
        text = message,
        color = colors.onSurface,
        textAlign = TextAlign.Center,
        style = CareerCompassTheme.typography.bodyLarge,
    )
    Spacer(modifier = Modifier.height(spacing.small))
    Text(
        text = stringResource(R.string.onboarding_complete_description),
        color = colors.mutedContent,
        textAlign = TextAlign.Center,
        style = CareerCompassTheme.typography.bodyMedium,
    )
}

@Composable
private fun CompletionBadge() {
    val colors = CareerCompassTheme.colors

    Box(
        modifier =
            Modifier
                .size(COMPLETION_BADGE_SIZE)
                .background(color = colors.primary, shape = CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = CareerCompassIcons.Check,
            contentDescription = null,
            modifier = Modifier.size(COMPLETION_ICON_SIZE),
            tint = colors.onAction,
        )
    }
}

private val COMPLETION_BADGE_SIZE = 72.dp

/** 글리프였을 때 36sp 로 그리던 자리다. 벡터는 폰트 배율을 타지 않아 배지 안에서 크기가 고정된다. */
private val COMPLETION_ICON_SIZE = 40.dp
