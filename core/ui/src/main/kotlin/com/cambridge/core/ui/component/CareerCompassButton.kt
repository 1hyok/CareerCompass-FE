package com.cambridge.core.ui.component

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.disabled
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cambridge.core.ui.theme.CareerCompassColors
import com.cambridge.core.ui.theme.CareerCompassTheme

/** Visual variants defined for CareerCompass buttons. */
public enum class CareerCompassButtonVariant {
    Primary,
    Secondary,
    Ghost,
    Dark,
    Danger,
}

/** Fixed button sizes from the CareerCompass component library. */
public enum class CareerCompassButtonSize(
    internal val height: Dp,
    internal val horizontalPadding: Dp,
    internal val fontSize: Int,
    internal val lineHeight: Double,
) {
    Small(
        height = 36.dp,
        horizontalPadding = 14.dp,
        fontSize = 13,
        lineHeight = 19.5,
    ),
    Medium(
        height = 44.dp,
        horizontalPadding = 18.dp,
        fontSize = 14,
        lineHeight = 21.0,
    ),
    Large(
        height = 52.dp,
        horizontalPadding = 22.dp,
        fontSize = 16,
        lineHeight = 24.0,
    ),
}

/**
 * CareerCompass action button.
 *
 * [text] must be non-blank. When provided, [contentDescription] must also be non-blank.
 *
 * [contentDescription] replaces the text announced for icon-assisted buttons when callers need
 * a more descriptive accessibility label.
 *
 * [leadingIcon] and [trailingIcon] receive [LocalContentColor] so the icon follows the variant's
 * content color without the caller repeating it. Both must render decorative icons
 * (`contentDescription = null`) — the button owns the accessible name.
 */
@Composable
public fun CareerCompassButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    variant: CareerCompassButtonVariant = CareerCompassButtonVariant.Primary,
    size: CareerCompassButtonSize = CareerCompassButtonSize.Medium,
    enabled: Boolean = true,
    leadingIcon: (@Composable () -> Unit)? = null,
    trailingIcon: (@Composable () -> Unit)? = null,
    contentDescription: String? = null,
) {
    require(text.isNotBlank()) { "text must not be blank" }
    require(contentDescription == null || contentDescription.isNotBlank()) {
        "contentDescription must be null or non-blank"
    }

    val colors = CareerCompassTheme.colors
    val shape = CareerCompassTheme.shapes.control
    val spacing = CareerCompassTheme.spacing
    val containerColor = buttonContainerColor(colors, variant, enabled)
    val contentColor = buttonContentColor(colors, variant, enabled)
    val accessibilityModifier =
        Modifier.semantics {
            role = Role.Button
            if (!enabled) disabled()
            if (contentDescription != null) {
                this.contentDescription = contentDescription
            }
        }

    Row(
        modifier =
            modifier
                .height(size.height)
                .clip(shape)
                .background(containerColor)
                .then(
                    if (variant == CareerCompassButtonVariant.Secondary) {
                        Modifier.border(BorderStroke(1.dp, colors.interactiveOutline), shape)
                    } else {
                        Modifier
                    },
                ).clickable(
                    enabled = enabled,
                    role = Role.Button,
                    onClick = onClick,
                ).then(accessibilityModifier)
                .padding(horizontal = size.horizontalPadding),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        CompositionLocalProvider(LocalContentColor provides contentColor) {
            if (leadingIcon != null) {
                leadingIcon()
                Spacer(modifier = Modifier.width(spacing.small))
            }

            Text(
                text = text,
                color = contentColor,
                maxLines = 1,
                style = size.textStyle(CareerCompassTheme.typography.labelMedium),
            )

            if (trailingIcon != null) {
                Spacer(modifier = Modifier.width(spacing.small))
                trailingIcon()
            }
        }
    }
}

private fun CareerCompassButtonSize.textStyle(baseStyle: TextStyle): TextStyle =
    baseStyle.copy(
        fontSize = fontSize.sp,
        lineHeight = lineHeight.sp,
        fontWeight = FontWeight.SemiBold,
    )

private fun buttonContainerColor(
    colors: CareerCompassColors,
    variant: CareerCompassButtonVariant,
    enabled: Boolean,
): Color {
    if (!enabled) return colors.disabledContainer

    return when (variant) {
        CareerCompassButtonVariant.Primary -> colors.actionPrimary
        CareerCompassButtonVariant.Secondary -> colors.surface
        CareerCompassButtonVariant.Ghost -> Color.Transparent
        CareerCompassButtonVariant.Dark -> colors.inverseSurface
        CareerCompassButtonVariant.Danger -> colors.actionDanger
    }
}

private fun buttonContentColor(
    colors: CareerCompassColors,
    variant: CareerCompassButtonVariant,
    enabled: Boolean,
): Color {
    if (!enabled) return colors.disabledContent

    return when (variant) {
        CareerCompassButtonVariant.Primary -> colors.onAction
        CareerCompassButtonVariant.Secondary -> colors.onSurface
        CareerCompassButtonVariant.Ghost -> colors.onSurface
        CareerCompassButtonVariant.Dark -> colors.inverseOnSurface
        CareerCompassButtonVariant.Danger -> colors.onAction
    }
}
