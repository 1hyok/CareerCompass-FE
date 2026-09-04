package com.careercompass.core.ui.component

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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.careercompass.core.ui.R
import com.careercompass.core.ui.theme.CareerCompassTheme

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
 * 하단 탭 바의 테스트 태그.
 *
 * 앱 셸이 인셋을 한 번만 적용하는지(#145) 계측으로 재려면 「콘텐츠가 어디서 끝나고 탭 바가 어디서 시작하는가」를
 * 화면 밖에서 잡을 수 있어야 한다. 탭 라벨 텍스트로 찾으면 안쪽 여백만큼 어긋나므로 바 자체에 태그를 둔다.
 */
internal const val CAREER_COMPASS_BOTTOM_BAR_TAG: String = "careercompass_bottom_bar"

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

    Box(modifier = modifier.testTag(CAREER_COMPASS_BOTTOM_BAR_TAG)) {
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
