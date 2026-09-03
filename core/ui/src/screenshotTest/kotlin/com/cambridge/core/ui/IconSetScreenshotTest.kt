package com.cambridge.core.ui

import android.content.res.Configuration
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.android.tools.screenshot.PreviewTest
import com.cambridge.core.ui.icon.CareerCompassIcons
import com.cambridge.core.ui.theme.CareerCompassTheme

/**
 * 아이콘 세트 전수 렌더.
 *
 * 아이콘은 화면 곳곳에 작게 흩어져 있어 화면 골든만으로는 모양이 무너져도 눈에 띄지 않는다. 세트 전체를
 * 한 장에 크게 모아 두면 경로를 고칠 때 그 한 장만 보면 된다. 다크 골든이 따로 있는 이유는 tint 다 —
 * 글리프를 걷어낸 목적이 테마 색을 받는 것이라 라이트·다크가 실제로 다른 색으로 그려지는지가 검증 대상이다.
 */
@PreviewTest
@Preview(name = "Icon set", widthDp = 360, heightDp = 320)
@Composable
public fun CareerCompassIconSetPreview() {
    IconSetPreview(darkTheme = false)
}

@PreviewTest
@Preview(name = "Icon set - Dark", uiMode = Configuration.UI_MODE_NIGHT_YES, widthDp = 360, heightDp = 320)
@Composable
public fun CareerCompassIconSetDarkPreview() {
    IconSetPreview(darkTheme = true)
}

/**
 * 아이콘이 글꼴 배율을 따라가지 않는지 확인하는 골든.
 *
 * 아이콘은 `Modifier.size(32.dp)`, 라벨은 `sp` 라 배율이 오르면 라벨만 커져야 한다. 배율을 따라
 * 커지면 안 되는 자리(하단 탭·상단바 글리프)가 실제로 안 커진다는 근거가 이 한 장에 다 들어 있다.
 * 캔버스는 라이트·다크와 같은 320dp 를 쓴다 — 아이콘이 안 커지는 덕에 배율 2.0 에서도 세트 전체가
 * 그 안에 들어온다. 그 «들어온다» 가 이 골든이 말하는 것이다.
 */
@PreviewTest
@Preview(name = "Icon set - Large font", widthDp = 360, heightDp = 320, fontScale = LARGE_FONT_SCALE)
@Composable
public fun CareerCompassIconSetLargeFontPreview() {
    IconSetPreview(darkTheme = false)
}

@Composable
private fun IconSetPreview(darkTheme: Boolean) {
    CareerCompassTheme(darkTheme = darkTheme) {
        Surface(color = CareerCompassTheme.colors.subtleSurface) {
            Column(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                iconRows().forEach { row ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        row.forEach { entry ->
                            IconSample(
                                modifier = Modifier.weight(1f),
                                label = entry.first,
                                icon = entry.second,
                            )
                        }
                        // 마지막 줄이 짧아도 앞줄과 열 폭이 어긋나지 않게 빈 칸을 채운다.
                        repeat(COLUMN_COUNT - row.size) {
                            Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun IconSample(
    modifier: Modifier,
    label: String,
    icon: ImageVector,
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(32.dp),
            tint = CareerCompassTheme.colors.onSurface,
        )
        Text(
            text = label,
            color = CareerCompassTheme.colors.mutedContent,
            style = CareerCompassTheme.typography.caption,
        )
    }
}

private const val COLUMN_COUNT: Int = 4

private fun iconRows(): List<List<Pair<String, ImageVector>>> =
    listOf(
        listOf(
            "Back" to CareerCompassIcons.ArrowBack,
            "Close" to CareerCompassIcons.Close,
            "Check" to CareerCompassIcons.Check,
            "Search" to CareerCompassIcons.Search,
        ),
        listOf(
            "Filter" to CareerCompassIcons.Filter,
            "More" to CareerCompassIcons.ExpandMore,
            "Less" to CareerCompassIcons.ExpandLess,
            "Share" to CareerCompassIcons.Share,
        ),
        listOf(
            "Mark" to CareerCompassIcons.BookmarkBorder,
            "Marked" to CareerCompassIcons.Bookmark,
            "Bell" to CareerCompassIcons.Notifications,
            "Add" to CareerCompassIcons.Add,
        ),
        listOf(
            "Edit" to CareerCompassIcons.Edit,
            "Dots" to CareerCompassIcons.MoreHorizontal,
            "Bullet" to CareerCompassIcons.Bullet,
        ),
    )
