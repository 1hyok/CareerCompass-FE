package com.cambridge.core.ui

import android.content.res.Configuration
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.android.tools.screenshot.PreviewTest
import com.cambridge.core.ui.component.CareerCompassBadge
import com.cambridge.core.ui.component.CareerCompassBadgeTone
import com.cambridge.core.ui.component.CareerCompassButton
import com.cambridge.core.ui.component.CareerCompassButtonSize
import com.cambridge.core.ui.component.CareerCompassButtonVariant
import com.cambridge.core.ui.component.CareerCompassScoreChip
import com.cambridge.core.ui.component.CareerCompassScoreLevel
import com.cambridge.core.ui.component.CareerCompassTag
import com.cambridge.core.ui.component.CareerCompassTextField
import com.cambridge.core.ui.theme.CareerCompassTheme

@PreviewTest
@Preview(name = "Button matrix", widthDp = 360, heightDp = 460)
@Composable
public fun CareerCompassButtonMatrixPreview() {
    ButtonMatrixPreview(darkTheme = false)
}

@PreviewTest
@Preview(name = "Button matrix - Dark", uiMode = Configuration.UI_MODE_NIGHT_YES, widthDp = 360, heightDp = 460)
@Composable
public fun CareerCompassButtonMatrixDarkPreview() {
    ButtonMatrixPreview(darkTheme = true)
}

@Composable
private fun ButtonMatrixPreview(darkTheme: Boolean) {
    DesignSystemPreviewSurface(darkTheme = darkTheme) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            CareerCompassButtonVariant.entries.forEach { variant ->
                CareerCompassButton(
                    text = variant.name,
                    onClick = {},
                    modifier = Modifier.fillMaxWidth(),
                    variant = variant,
                    size = CareerCompassButtonSize.Medium,
                )
            }
            CareerCompassButton(
                text = "Disabled",
                onClick = {},
                modifier = Modifier.fillMaxWidth(),
                enabled = false,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                CareerCompassButton(
                    text = "Small",
                    onClick = {},
                    size = CareerCompassButtonSize.Small,
                )
                CareerCompassButton(
                    text = "Medium",
                    onClick = {},
                    size = CareerCompassButtonSize.Medium,
                )
                CareerCompassButton(
                    text = "Large",
                    onClick = {},
                    size = CareerCompassButtonSize.Large,
                )
            }
        }
    }
}

@PreviewTest
@Preview(name = "Badge, tag and score matrix", widthDp = 360, heightDp = 310)
@Composable
public fun CareerCompassIndicatorMatrixPreview() {
    IndicatorMatrixPreview(darkTheme = false)
}

@PreviewTest
@Preview(name = "Badge, tag and score matrix - Dark", uiMode = Configuration.UI_MODE_NIGHT_YES, widthDp = 360, heightDp = 310)
@Composable
public fun CareerCompassIndicatorMatrixDarkPreview() {
    IndicatorMatrixPreview(darkTheme = true)
}

@Composable
private fun IndicatorMatrixPreview(darkTheme: Boolean) {
    DesignSystemPreviewSurface(darkTheme = darkTheme) {
        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    CareerCompassBadge(label = "Brand", tone = CareerCompassBadgeTone.Brand)
                    CareerCompassBadge(label = "Neutral", tone = CareerCompassBadgeTone.Neutral)
                    CareerCompassBadge(label = "Warning", tone = CareerCompassBadgeTone.Warning)
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    CareerCompassBadge(label = "Error", tone = CareerCompassBadgeTone.Error)
                    CareerCompassBadge(label = "Info", tone = CareerCompassBadgeTone.Info)
                    CareerCompassBadge(label = "Dark", tone = CareerCompassBadgeTone.Dark)
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                CareerCompassTag(label = "Default", selected = false, onClick = {})
                CareerCompassTag(label = "Selected", selected = true, onClick = {})
                CareerCompassTag(
                    label = "Disabled",
                    selected = false,
                    enabled = false,
                    onClick = {},
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                CareerCompassScoreChip(
                    label = "적합도",
                    score = 88,
                    level = CareerCompassScoreLevel.High,
                )
                CareerCompassScoreChip(
                    label = "적합도",
                    score = 76,
                    level = CareerCompassScoreLevel.Mid,
                )
                CareerCompassScoreChip(
                    label = "적합도",
                    score = 42,
                    level = CareerCompassScoreLevel.Low,
                )
            }
        }
    }
}

@PreviewTest
@Preview(name = "Text field matrix", widthDp = 360, heightDp = 460)
@Composable
public fun CareerCompassTextFieldMatrixPreview() {
    TextFieldMatrixPreview(darkTheme = false)
}

@PreviewTest
@Preview(name = "Text field matrix - Dark", uiMode = Configuration.UI_MODE_NIGHT_YES, widthDp = 360, heightDp = 460)
@Composable
public fun CareerCompassTextFieldMatrixDarkPreview() {
    TextFieldMatrixPreview(darkTheme = true)
}

@Composable
private fun TextFieldMatrixPreview(darkTheme: Boolean) {
    DesignSystemPreviewSurface(darkTheme = darkTheme) {
        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            CareerCompassTextField(
                value = "",
                onValueChange = {},
                label = "기본",
                placeholder = "내용을 입력해 주세요",
            )
            CareerCompassTextField(
                value = "입력된 내용",
                onValueChange = {},
                label = "입력 완료",
                supportingText = "도움말 텍스트",
            )
            CareerCompassTextField(
                value = "잘못된 값",
                onValueChange = {},
                label = "오류",
                supportingText = "입력값을 확인해 주세요",
                isError = true,
            )
            CareerCompassTextField(
                value = "수정할 수 없음",
                onValueChange = {},
                label = "비활성",
                enabled = false,
            )
        }
    }
}

@Composable
private fun DesignSystemPreviewSurface(
    darkTheme: Boolean = false,
    content: @Composable () -> Unit,
) {
    CareerCompassTheme(darkTheme = darkTheme) {
        Surface(color = CareerCompassTheme.colors.subtleSurface) {
            Column(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
            ) {
                content()
            }
        }
    }
}
