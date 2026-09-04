package com.careercompass.core.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.careercompass.core.ui.R
import com.careercompass.core.ui.icon.CareerCompassIcons
import com.careercompass.core.ui.theme.CareerCompassTheme

/**
 * CareerCompass top application bar with optional navigation, subtitle, and actions.
 *
 * [title] must be non-blank. When provided, [subtitle] must also be non-blank.
 * Passing `null` for [onBackClick] removes the back control from the composition.
 *
 * ### 큰 글꼴
 * 높이 56dp 는 하한이다. 고정이던 시절 글꼴 배율 2.0 에서 제목 아래 부제가 가로로 반 잘려
 * 한글 받침이 통째로 사라졌다 — 읽을 수 없는 두 줄보다 조금 높은 상단바가 낫다. 기본 배율에서는
 * 뒤로가기 버튼(48dp)과 글자 두 줄(40dp)이 모두 56dp 안에 들어와 높이가 종전과 같다.
 *
 * [title] 과 [subtitle] 은 배율과 무관하게 각각 한 줄로 유지하고 넘치면 말줄임한다. 제목이 접히면
 * 상단바 높이가 제목 길이에 끌려다녀 화면마다 들쭉날쭉해진다 — 상단바가 차지하는 세로는 예측
 * 가능한 편이 낫다.
 */
@Composable
public fun CareerCompassTopAppBar(
    title: String,
    onBackClick: (() -> Unit)?,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    actions: (@Composable RowScope.() -> Unit)? = null,
) {
    require(title.isNotBlank()) { "title must not be blank" }
    require(subtitle == null || subtitle.isNotBlank()) {
        "subtitle must be null or non-blank"
    }

    val colors = CareerCompassTheme.colors
    val spacing = CareerCompassTheme.spacing

    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .heightIn(min = 56.dp)
                .background(colors.subtleSurface),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (onBackClick != null) {
            BackButton(onClick = onBackClick)
        }

        Column(
            modifier =
                Modifier
                    .weight(1f)
                    .padding(
                        start = if (onBackClick == null) spacing.large else spacing.xxSmall,
                        end = spacing.small,
                    ),
        ) {
            Text(
                text = title,
                color = colors.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                style = CareerCompassTheme.typography.headline4,
            )
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    color = colors.mutedContent,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = CareerCompassTheme.typography.caption,
                )
            }
        }

        if (actions != null) {
            CompositionLocalProvider(LocalContentColor provides colors.onSurface) {
                Row(
                    modifier = Modifier.padding(end = spacing.xxSmall),
                    verticalAlignment = Alignment.CenterVertically,
                    content = actions,
                )
            }
        }
    }
}

@Composable
private fun BackButton(onClick: () -> Unit) {
    val colors = CareerCompassTheme.colors
    val spacing = CareerCompassTheme.spacing
    val backDescription = stringResource(R.string.core_ui_back)

    Box(
        modifier =
            Modifier
                .padding(start = spacing.xxSmall)
                .size(48.dp)
                .clickable(
                    role = Role.Button,
                    onClick = onClick,
                ).semantics {
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
