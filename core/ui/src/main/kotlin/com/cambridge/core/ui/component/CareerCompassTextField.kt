package com.cambridge.core.ui.component

import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.disabled
import androidx.compose.ui.semantics.error
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cambridge.core.ui.theme.CareerCompassTheme

/** Input sizes defined by the shared component library and onboarding screen specification. */
public enum class CareerCompassTextFieldSize(
    internal val fieldHeight: Dp,
    internal val labelGap: Dp,
    internal val horizontalPadding: Dp,
    internal val fontSize: TextUnit,
    internal val lineHeight: TextUnit,
) {
    Standard(
        fieldHeight = 48.dp,
        labelGap = 6.dp,
        horizontalPadding = 14.dp,
        fontSize = 14.sp,
        lineHeight = 21.sp,
    ),
    Large(
        fieldHeight = 50.dp,
        labelGap = 8.dp,
        horizontalPadding = 16.dp,
        fontSize = 15.sp,
        lineHeight = 22.5.sp,
    ),
}

/**
 * CareerCompass labelled text input with focus, error, and disabled states.
 *
 * [label] must be non-blank so the field always exposes an accessibility name.
 *
 * Error state text must be supplied by the caller through [errorMessage] or [supportingText] so
 * accessibility output is localized with the surrounding screen. Supplying [onClick] turns a
 * [readOnly] field into a button-like picker while preserving the same visual contract.
 *
 * ### 큰 글꼴
 * [CareerCompassTextFieldSize.fieldHeight] 는 의도적으로 고정이다. 입력칸은 한 줄짜리라 글자가
 * 늘어날 세로가 없고, 글꼴 배율 2.0 에서도 입력 글자(28sp·행높이 42dp)가 48dp 안에 들어오는 것을
 * 골든으로 확인했다. 대신 라벨·플레이스홀더·읽기 전용 값은 가로로 넘칠 수 있어 말줄임을 붙인다 —
 * 글자 한복판이 잘려 나가면 무슨 칸인지 읽을 수 없다. 도움말·오류 문구는 줄 수 제한이 없어 접힌다.
 */
@Composable
public fun CareerCompassTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    placeholder: String = "",
    supportingText: String? = null,
    errorMessage: String? = null,
    leadingIcon: (@Composable () -> Unit)? = null,
    trailingIcon: (@Composable () -> Unit)? = null,
    isError: Boolean = false,
    enabled: Boolean = true,
    readOnly: Boolean = false,
    size: CareerCompassTextFieldSize = CareerCompassTextFieldSize.Standard,
    onClick: (() -> Unit)? = null,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
) {
    require(label.isNotBlank()) { "label must not be blank" }
    require(onClick == null || readOnly) {
        "onClick is only supported for read-only fields"
    }
    val displayedSupportingText =
        if (isError) {
            errorMessage ?: supportingText
        } else {
            supportingText
        }
    require(!isError || !displayedSupportingText.isNullOrBlank()) {
        "An error field requires a caller-provided errorMessage or supportingText"
    }

    val colors = CareerCompassTheme.colors
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()
    val borderColor =
        when {
            !enabled -> colors.subtleOutline
            isError -> colors.actionDanger
            isFocused -> colors.primaryEmphasis
            else -> colors.interactiveOutline
        }
    val borderWidth = if (enabled && (isError || isFocused)) 1.5.dp else 1.dp
    val containerColor = if (enabled) colors.surface else colors.disabledContainer
    val textColor = if (enabled) colors.onSurface else colors.disabledContent
    val labelColor = if (enabled) colors.onSurfaceVariant else colors.disabledContent
    val shape =
        when (size) {
            CareerCompassTextFieldSize.Standard -> CareerCompassTheme.shapes.control
            CareerCompassTextFieldSize.Large -> CareerCompassTheme.shapes.largeControl
        }
    val spacing = CareerCompassTheme.spacing
    val inputTextStyle =
        CareerCompassTheme.typography.bodyMedium.copy(
            color = textColor,
            fontSize = size.fontSize,
            lineHeight = size.lineHeight,
            fontWeight = FontWeight.Medium,
        )
    val fieldModifier =
        Modifier
            .fillMaxWidth()
            .height(size.fieldHeight)
            .clip(shape)
            .background(containerColor)
            .border(borderWidth, borderColor, shape)

    Column(modifier = modifier) {
        Text(
            text = label,
            modifier = Modifier.clearAndSetSemantics {},
            color = labelColor,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            style =
                CareerCompassTheme.typography.labelMedium.copy(
                    fontSize = 13.sp,
                    lineHeight = 19.5.sp,
                    fontWeight = FontWeight.Medium,
                ),
        )
        Spacer(modifier = Modifier.height(size.labelGap))
        if (onClick == null) {
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                modifier =
                    fieldModifier.semantics {
                        contentDescription = label
                        if (!enabled) disabled()
                        if (isError) error(checkNotNull(displayedSupportingText))
                    },
                enabled = enabled,
                readOnly = readOnly,
                singleLine = true,
                keyboardOptions = keyboardOptions,
                keyboardActions = keyboardActions,
                interactionSource = interactionSource,
                cursorBrush = SolidColor(colors.primaryEmphasis),
                textStyle = inputTextStyle,
                decorationBox = { innerTextField ->
                    TextFieldContent(
                        value = value,
                        placeholder = placeholder,
                        size = size,
                        leadingIcon = leadingIcon,
                        trailingIcon = trailingIcon,
                        iconColor = labelColor,
                        placeholderColor = colors.mutedContent,
                        content = innerTextField,
                    )
                },
            )
        } else {
            Box(
                modifier =
                    fieldModifier
                        .clickable(
                            interactionSource = interactionSource,
                            indication = LocalIndication.current,
                            enabled = enabled,
                            role = Role.Button,
                            onClick = onClick,
                        ).semantics {
                            contentDescription = label
                            if (!enabled) disabled()
                            if (isError) error(checkNotNull(displayedSupportingText))
                        },
            ) {
                TextFieldContent(
                    value = value,
                    placeholder = placeholder,
                    size = size,
                    leadingIcon = leadingIcon,
                    trailingIcon = trailingIcon,
                    iconColor = labelColor,
                    placeholderColor = colors.mutedContent,
                ) {
                    if (value.isNotEmpty()) {
                        Text(
                            text = value,
                            color = textColor,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            style = inputTextStyle,
                        )
                    }
                }
            }
        }

        if (displayedSupportingText != null) {
            Spacer(modifier = Modifier.height(spacing.xSmall))
            Text(
                text = displayedSupportingText,
                color = if (isError) colors.actionDanger else colors.mutedContent,
                style =
                    CareerCompassTheme.typography.bodyMedium.copy(
                        fontSize = 12.sp,
                        lineHeight = 18.sp,
                        fontWeight = FontWeight.Normal,
                    ),
            )
        }
    }
}

@Composable
private fun TextFieldContent(
    value: String,
    placeholder: String,
    size: CareerCompassTextFieldSize,
    leadingIcon: (@Composable () -> Unit)?,
    trailingIcon: (@Composable () -> Unit)?,
    iconColor: androidx.compose.ui.graphics.Color,
    placeholderColor: androidx.compose.ui.graphics.Color,
    content: @Composable () -> Unit,
) {
    val spacing = CareerCompassTheme.spacing

    Row(
        modifier =
            Modifier
                .fillMaxSize()
                .padding(horizontal = size.horizontalPadding),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (leadingIcon != null) {
            CompositionLocalProvider(LocalContentColor provides iconColor) {
                leadingIcon()
            }
            Spacer(modifier = Modifier.width(spacing.small))
        }

        Box(
            modifier = Modifier.weight(1f),
            contentAlignment = Alignment.CenterStart,
        ) {
            if (value.isEmpty() && placeholder.isNotEmpty()) {
                Text(
                    text = placeholder,
                    color = placeholderColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style =
                        CareerCompassTheme.typography.bodyMedium.copy(
                            fontSize = size.fontSize,
                            lineHeight = size.lineHeight,
                            fontWeight = FontWeight.Normal,
                        ),
                )
            }
            content()
        }

        if (trailingIcon != null) {
            Spacer(modifier = Modifier.width(spacing.small))
            CompositionLocalProvider(LocalContentColor provides iconColor) {
                trailingIcon()
            }
        }
    }
}
