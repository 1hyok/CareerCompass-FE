package com.cambridge.feature.onboarding.presentation.login.component

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
 * 소셜 로그인 버튼의 공통 껍데기.
 *
 * 디자인 시스템 버튼은 카카오 노랑 같은 벤더 색을 받지 않으므로, 이 컴포넌트가 컨테이너·글자 색을
 * 직접 갖는다. 어느 색을 넣을지는 provider 별 버튼([KakaoLoginButton] · [GoogleLoginButton])이
 * 브랜드 가이드에서 가져와 정한다 — 여기서는 그 값을 그리기만 한다.
 *
 * [text] 는 비어 있을 수 없고 그대로 접근성 이름이 된다. [leadingMark] 는 브랜드 마크 자리이며
 * 장식이라 자기 이름을 갖지 않는다 — 마크에 이름을 달면 버튼 이름과 이중으로 읽힌다. 큰 글꼴
 * 배율에서는 라벨을 자르는 대신 [SOCIAL_LOGIN_BUTTON_HEIGHT] 를 넘겨 자란다.
 */
@Composable
internal fun SocialLoginButton(
    text: String,
    onClick: () -> Unit,
    containerColor: Color,
    contentColor: Color,
    border: BorderStroke?,
    enabled: Boolean,
    modifier: Modifier = Modifier,
    leadingMark: @Composable () -> Unit,
) {
    require(text.isNotBlank()) { "text must not be blank" }

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
        Box(modifier = Modifier.clearAndSetSemantics {}) { leadingMark() }
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
