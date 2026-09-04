package com.cambridge.feature.feed.presentation.shared.component

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.careercompass.core.ui.theme.CareerCompassTheme

/**
 * A 48dp icon button. [contentDescription] is the only accessible name, so it must be non-blank —
 * [icon] is drawn decoratively.
 */
@Composable
internal fun FeedIconButton(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    tint: Color = CareerCompassTheme.colors.onSurface,
) {
    require(contentDescription.isNotBlank()) { "contentDescription must not be blank" }

    Box(
        modifier =
            modifier
                .size(48.dp)
                .clickable(role = Role.Button, onClick = onClick)
                .semantics {
                    this.contentDescription = contentDescription
                    role = Role.Button
                },
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(FEED_ICON_SIZE),
            tint = tint,
        )
    }
}

/** 48dp 버튼 안에 홀로 놓이는 아이콘 크기. */
internal val FEED_ICON_SIZE = 24.dp

/** 검색 필드·필터 버튼·정렬 트리거처럼 글자나 입력칸 옆에 붙는 아이콘 크기. 24dp 는 그 자리에서 크다. */
internal val FEED_INLINE_ICON_SIZE = 20.dp
