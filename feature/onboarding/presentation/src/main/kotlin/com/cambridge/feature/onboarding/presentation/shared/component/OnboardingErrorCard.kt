package com.cambridge.feature.onboarding.presentation.shared.component

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.cambridge.core.ui.theme.CareerCompassTheme
import com.cambridge.feature.onboarding.presentation.R

/**
 * Dismissible inline error banner shared by the login and biometric login screens.
 *
 * [message] must be non-blank. The message is announced politely when it appears and the
 * dismiss action keeps a 48dp touch target with an explicit accessible name.
 */
@Composable
internal fun OnboardingErrorCard(
    message: String,
    onDismissClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    require(message.isNotBlank()) { "message must not be blank" }

    val colors = CareerCompassTheme.colors
    val spacing = CareerCompassTheme.spacing
    val dismissDescription = stringResource(R.string.onboarding_error_dismiss_description)

    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .background(
                    color = colors.errorContainer,
                    shape = CareerCompassTheme.shapes.largeControl,
                ).padding(
                    start = spacing.large,
                    top = spacing.xSmall,
                    end = spacing.xSmall,
                    bottom = spacing.xSmall,
                ),
        horizontalArrangement = Arrangement.spacedBy(spacing.small),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = message,
            modifier =
                Modifier
                    .weight(1f)
                    .semantics { liveRegion = LiveRegionMode.Polite },
            color = colors.onErrorContainer,
            style = CareerCompassTheme.typography.bodyMedium,
        )
        Box(
            modifier =
                Modifier
                    .sizeIn(minWidth = DISMISS_TOUCH_TARGET, minHeight = DISMISS_TOUCH_TARGET)
                    .clip(CareerCompassTheme.shapes.control)
                    .clickable(role = Role.Button, onClick = onDismissClick)
                    .semantics(mergeDescendants = true) {
                        contentDescription = dismissDescription
                        role = Role.Button
                    }.padding(horizontal = spacing.medium),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = stringResource(R.string.onboarding_error_dismiss),
                color = colors.onErrorContainer,
                style =
                    CareerCompassTheme.typography.labelMedium.copy(
                        fontWeight = FontWeight.SemiBold,
                    ),
            )
        }
    }
}

private val DISMISS_TOUCH_TARGET = 48.dp
