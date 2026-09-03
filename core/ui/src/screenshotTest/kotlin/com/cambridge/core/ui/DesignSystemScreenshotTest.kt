package com.cambridge.core.ui

import android.content.res.Configuration
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.FlowRowScope
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

/**
 * 컴포넌트 갤러리.
 *
 * 각 매트릭스는 라이트·다크에 더해 [LARGE_FONT_SCALE] 변형을 하나씩 갖는다 — 화면 골든이 잡는 것은
 * «이 화면의 이 배치» 지만, 여기서 깨지는 것은 모든 화면에서 깨진다.
 *
 * 큰 글꼴 변형만 `heightDp` 를 늘린다. 갤러리 캔버스는 단말 화면이 아니라 «컴포넌트를 전부 담는 판»
 * 이라, 높이를 그대로 두면 아래쪽 컴포넌트가 캔버스 밖으로 밀려 정작 봐야 할 것이 골든에 안 남는다.
 * 화면 골든(로그인·피드 등)은 반대로 단말 높이를 그대로 둔다 — 거기서는 잘리는 것 자체가 관측 대상이다.
 */
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

@PreviewTest
@Preview(name = "Button matrix - Large font", widthDp = 360, heightDp = 560, fontScale = LARGE_FONT_SCALE)
@Composable
public fun CareerCompassButtonMatrixLargeFontPreview() {
    ButtonMatrixPreview(darkTheme = false)
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
            GalleryRow {
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

@PreviewTest
@Preview(
    name = "Badge, tag and score matrix - Large font",
    widthDp = 360,
    heightDp = 400,
    fontScale = LARGE_FONT_SCALE,
)
@Composable
public fun CareerCompassIndicatorMatrixLargeFontPreview() {
    IndicatorMatrixPreview(darkTheme = false)
}

@Composable
private fun IndicatorMatrixPreview(darkTheme: Boolean) {
    DesignSystemPreviewSurface(darkTheme = darkTheme) {
        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                GalleryRow {
                    CareerCompassBadge(label = "Brand", tone = CareerCompassBadgeTone.Brand)
                    CareerCompassBadge(label = "Neutral", tone = CareerCompassBadgeTone.Neutral)
                    CareerCompassBadge(label = "Warning", tone = CareerCompassBadgeTone.Warning)
                }
                GalleryRow {
                    CareerCompassBadge(label = "Error", tone = CareerCompassBadgeTone.Error)
                    CareerCompassBadge(label = "Info", tone = CareerCompassBadgeTone.Info)
                    CareerCompassBadge(label = "Dark", tone = CareerCompassBadgeTone.Dark)
                }
            }
            GalleryRow {
                CareerCompassTag(label = "Default", selected = false, onClick = {})
                CareerCompassTag(label = "Selected", selected = true, onClick = {})
                CareerCompassTag(
                    label = "Disabled",
                    selected = false,
                    enabled = false,
                    onClick = {},
                )
            }
            GalleryRow {
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

@PreviewTest
@Preview(name = "Text field matrix - Large font", widthDp = 360, heightDp = 620, fontScale = LARGE_FONT_SCALE)
@Composable
public fun CareerCompassTextFieldMatrixLargeFontPreview() {
    TextFieldMatrixPreview(darkTheme = false)
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

/**
 * 갤러리의 «한 줄» — 자리가 모자라면 다음 줄로 접는다.
 *
 * 종전의 [androidx.compose.foundation.layout.Row] 는 배율 2.0 에서 세 번째 컴포넌트를 캔버스
 * 밖으로 밀어냈다(Disabled 태그가 «D» 로, Low 점수칩이 통째로). 갤러리는 «컴포넌트를 빠짐없이
 * 보여 주는 판» 이라, 못 보여 주면 골든이 지킬 것도 없다. 기본 배율에서는 종전과 같이 한 줄에
 * 다 들어가 그림이 바뀌지 않는다.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun GalleryRow(content: @Composable FlowRowScope.() -> Unit) {
    FlowRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        content = content,
    )
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
