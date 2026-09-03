package com.cambridge.core.ui.component

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.cambridge.core.ui.R
import com.cambridge.core.ui.theme.CareerCompassTheme

/** Destinations presented by [CareerCompassBottomBar]. */
public enum class CareerCompassBottomTab(
    @StringRes internal val labelResId: Int,
    internal val icon: ImageVector,
) {
    Feed(
        labelResId = R.string.core_ui_bottom_tab_feed,
        icon = Icons.Outlined.Home,
    ),
    Analysis(
        labelResId = R.string.core_ui_bottom_tab_analysis,
        icon = Icons.Outlined.Search,
    ),
    Applications(
        labelResId = R.string.core_ui_bottom_tab_applications,
        icon = Icons.Outlined.Edit,
    ),
    My(
        labelResId = R.string.core_ui_bottom_tab_my,
        icon = Icons.Outlined.Person,
    ),
}

/**
 * CareerCompass primary bottom navigation.
 *
 * [selectedTab] identifies the current destination. Selecting any tab invokes [onTabClick]
 * with that destination so navigation state remains owned by the caller.
 *
 * ### 큰 글꼴
 * 탭 글리프는 [ImageVector] 라 `dp` 로 그려진다 — 글꼴 배율을 따라가지 않아야 맞고, 실제로
 * 안 따라간다. 배율 2.0 골든에서 아이콘은 24dp 그대로이고 라벨만 커지며, 아이콘·라벨을 합친
 * 세로 56dp 가 Material 기본 탭바 높이 80dp 안에 남는다. 라벨 타이포를 `caption`
 * (11sp) 보다 키우면 이 여유가 사라지므로 함께 골든을 다시 본다.
 */
@Composable
public fun CareerCompassBottomBar(
    selectedTab: CareerCompassBottomTab,
    onTabClick: (CareerCompassBottomTab) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = CareerCompassTheme.colors
    val itemColors =
        NavigationBarItemDefaults.colors(
            selectedIconColor = colors.primary,
            selectedTextColor = colors.primary,
            unselectedIconColor = colors.mutedContent,
            unselectedTextColor = colors.mutedContent,
            indicatorColor = Color.Transparent,
        )

    Box(modifier = modifier) {
        NavigationBar(
            modifier = Modifier.fillMaxWidth(),
            containerColor = colors.surface,
            tonalElevation = 0.dp,
        ) {
            CareerCompassBottomTab.entries.forEach { tab ->
                val selected = tab == selectedTab

                NavigationBarItem(
                    selected = selected,
                    onClick = { onTabClick(tab) },
                    icon = {
                        Icon(
                            imageVector = tab.icon,
                            contentDescription = null,
                        )
                    },
                    modifier =
                        Modifier.semantics {
                            role = Role.Tab
                            this.selected = selected
                        },
                    label = {
                        Text(
                            text = stringResource(tab.labelResId),
                            style = CareerCompassTheme.typography.caption,
                        )
                    },
                    colors = itemColors,
                )
            }
        }

        HorizontalDivider(
            modifier = Modifier.align(Alignment.TopCenter),
            thickness = 1.dp,
            color = colors.subtleOutline,
        )
    }
}
