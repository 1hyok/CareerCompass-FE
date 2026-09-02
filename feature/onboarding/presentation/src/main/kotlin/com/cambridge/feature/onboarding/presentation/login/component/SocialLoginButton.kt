package com.cambridge.feature.onboarding.presentation.login.component

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.disabled
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cambridge.core.ui.theme.CareerCompassTheme

/**
 * Full-width social sign-in button with provider-specific colors.
 *
 * The design-system button does not support vendor colors such as Kakao yellow, so this
 * component owns its container and content colors. [text] must be non-blank and is the
 * accessible name; [leadingIcon] is a decorative glyph and must be null or non-blank.
 * The button grows past [SOCIAL_LOGIN_BUTTON_HEIGHT] under large font scales instead of
 * clipping its label.
 */
@Composable
internal fun SocialLoginButton(
    text: String,
    onClick: () -> Unit,
    containerColor: Color,
    contentColor: Color,
    border: BorderStroke?,
    leadingIcon: String?,
    enabled: Boolean,
    modifier: Modifier = Modifier,
) {
    require(text.isNotBlank()) { "text must not be blank" }
    require(leadingIcon == null || leadingIcon.isNotBlank()) {
        "leadingIcon must be null or non-blank"
    }

    val colors = CareerCompassTheme.colors
    val spacing = CareerCompassTheme.spacing
    val shape = CareerCompassTheme.shapes.control
    val resolvedContainerColor = if (enabled) containerColor else colors.disabledContainer
    val resolvedContentColor = if (enabled) contentColor else colors.disabledContent
    val resolvedBorder =
        when {
            border == null -> null
            enabled -> border
            else -> BorderStroke(border.width, colors.subtleOutline)
        }
    val borderModifier =
        if (resolvedBorder != null) {
            Modifier.border(resolvedBorder, shape)
        } else {
            Modifier
        }

    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .heightIn(min = SOCIAL_LOGIN_BUTTON_HEIGHT)
                .clip(shape)
                .background(resolvedContainerColor)
                .then(borderModifier)
                .clickable(
                    enabled = enabled,
                    role = Role.Button,
                    onClick = onClick,
                ).semantics(mergeDescendants = true) {
                    role = Role.Button
                    if (!enabled) disabled()
                }.padding(horizontal = spacing.xLarge, vertical = spacing.small),
        horizontalArrangement = Arrangement.spacedBy(spacing.small, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (leadingIcon != null) {
            Text(
                text = leadingIcon,
                modifier = Modifier.clearAndSetSemantics {},
                color = resolvedContentColor,
                style =
                    CareerCompassTheme.typography.labelMedium.copy(
                        fontSize = 16.sp,
                        lineHeight = 24.sp,
                        fontWeight = FontWeight.Bold,
                    ),
            )
        }
        Text(
            text = text,
            color = resolvedContentColor,
            textAlign = TextAlign.Center,
            style =
                CareerCompassTheme.typography.labelMedium.copy(
                    fontSize = 16.sp,
                    lineHeight = 24.sp,
                    fontWeight = FontWeight.SemiBold,
                ),
        )
    }
}

/** Minimum height of a social login button; matches the large design-system button. */
internal val SOCIAL_LOGIN_BUTTON_HEIGHT: Dp = 52.dp
