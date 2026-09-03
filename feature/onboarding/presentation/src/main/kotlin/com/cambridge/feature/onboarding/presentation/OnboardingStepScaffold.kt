package com.cambridge.feature.onboarding.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cambridge.core.ui.component.CareerCompassButton
import com.cambridge.core.ui.component.CareerCompassButtonSize
import com.cambridge.core.ui.icon.CareerCompassIcons
import com.cambridge.core.ui.theme.CareerCompassTheme

/** Shared layout and accessibility chrome for every onboarding step. */
@Composable
internal fun OnboardingStepScaffold(
    currentStep: Int,
    totalSteps: Int,
    title: String,
    description: String,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
    footerContent: @Composable () -> Unit,
    content: @Composable ColumnScope.() -> Unit,
) {
    require(totalSteps > 0) { "totalSteps must be positive" }
    require(currentStep in 1..totalSteps) { "currentStep must be within 1..totalSteps" }

    val colors = CareerCompassTheme.colors
    val spacing = CareerCompassTheme.spacing

    Column(
        modifier =
            modifier
                .fillMaxSize()
                .background(colors.subtleSurface),
    ) {
        OnboardingTopBar(onBackClick = onBackClick)
        OnboardingProgress(
            currentStep = currentStep,
            totalSteps = totalSteps,
        )
        Column(
            modifier =
                Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = spacing.large, vertical = spacing.xxLarge),
        ) {
            OnboardingHeader(
                currentStep = currentStep,
                totalSteps = totalSteps,
                title = title,
                description = description,
            )
            Spacer(modifier = Modifier.height(spacing.xxLarge))
            content()
        }
        footerContent()
    }
}

/** Standard single-primary-action footer used by onboarding steps. */
@Composable
internal fun OnboardingPrimaryActionFooter(
    text: String,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = CareerCompassTheme.colors
    val spacing = CareerCompassTheme.spacing

    Box(
        modifier =
            modifier
                .fillMaxWidth()
                .background(colors.subtleSurface)
                .navigationBarsPadding()
                .padding(
                    start = spacing.large,
                    top = spacing.medium,
                    end = spacing.large,
                    bottom = spacing.large,
                ),
    ) {
        CareerCompassButton(
            text = text,
            onClick = onClick,
            modifier = Modifier.fillMaxWidth(),
            size = CareerCompassButtonSize.Large,
            enabled = enabled,
        )
    }
}

@Composable
private fun OnboardingTopBar(onBackClick: () -> Unit) {
    val colors = CareerCompassTheme.colors
    val backDescription = stringResource(R.string.onboarding_back)

    Box(
        modifier =
            Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .height(56.dp),
        contentAlignment = Alignment.CenterStart,
    ) {
        Box(
            modifier =
                Modifier
                    .padding(start = 4.dp)
                    .size(48.dp)
                    .clickable(role = Role.Button, onClick = onBackClick)
                    .semantics {
                        contentDescription = backDescription
                        role = Role.Button
                    },
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = CareerCompassIcons.ArrowBack,
                contentDescription = null,
                modifier = Modifier.size(24.dp),
                tint = colors.onSurface,
            )
        }
    }
}

@Composable
private fun OnboardingProgress(
    currentStep: Int,
    totalSteps: Int,
) {
    val colors = CareerCompassTheme.colors
    val spacing = CareerCompassTheme.spacing
    val progressDescription =
        stringResource(
            R.string.onboarding_progress_description,
            totalSteps,
            currentStep,
        )

    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .height(4.dp)
                .padding(horizontal = spacing.large)
                .semantics(mergeDescendants = true) {
                    contentDescription = progressDescription
                    progressBarRangeInfo =
                        ProgressBarRangeInfo(
                            current = currentStep.toFloat(),
                            range = 0f..totalSteps.toFloat(),
                            steps = (totalSteps - 1).coerceAtLeast(0),
                        )
                },
        horizontalArrangement = Arrangement.spacedBy(spacing.xSmall),
    ) {
        repeat(totalSteps) { index ->
            Box(
                modifier =
                    Modifier
                        .weight(1f)
                        .height(4.dp)
                        .background(
                            color =
                                if (index < currentStep) {
                                    colors.primaryEmphasis
                                } else {
                                    colors.outline
                                },
                            shape = CareerCompassTheme.shapes.pill,
                        ),
            )
        }
    }
}

@Composable
private fun OnboardingHeader(
    currentStep: Int,
    totalSteps: Int,
    title: String,
    description: String,
) {
    val colors = CareerCompassTheme.colors
    val spacing = CareerCompassTheme.spacing

    Column(verticalArrangement = Arrangement.spacedBy(spacing.small)) {
        Text(
            text =
                stringResource(
                    R.string.onboarding_step_label,
                    currentStep,
                    totalSteps,
                ),
            color = colors.actionPrimary,
            style =
                CareerCompassTheme.typography.caption.copy(
                    fontSize = 11.sp,
                    lineHeight = 16.5.sp,
                    letterSpacing = 0.8.sp,
                    fontWeight = FontWeight.SemiBold,
                ),
        )
        Text(
            text = title,
            modifier = Modifier.semantics { heading() },
            color = colors.onSurface,
            style =
                CareerCompassTheme.typography.headline1.copy(
                    lineHeight = 36.sp,
                    letterSpacing = (-0.3).sp,
                ),
        )
        Text(
            text = description,
            color = colors.onSurfaceVariant,
            style = CareerCompassTheme.typography.bodyMedium,
        )
    }
}
