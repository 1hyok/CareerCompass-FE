package com.cambridge.core.ui

import android.content.res.Configuration
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.android.tools.screenshot.PreviewTest
import com.cambridge.core.ui.component.CareerCompassAnalyzingState
import com.cambridge.core.ui.component.CareerCompassBadge
import com.cambridge.core.ui.component.CareerCompassBadgeTone
import com.cambridge.core.ui.component.CareerCompassBottomBar
import com.cambridge.core.ui.component.CareerCompassBottomTab
import com.cambridge.core.ui.component.CareerCompassButton
import com.cambridge.core.ui.component.CareerCompassButtonSize
import com.cambridge.core.ui.component.CareerCompassButtonVariant
import com.cambridge.core.ui.component.CareerCompassCard
import com.cambridge.core.ui.component.CareerCompassEmptyState
import com.cambridge.core.ui.component.CareerCompassMaintenanceState
import com.cambridge.core.ui.component.CareerCompassNetworkErrorState
import com.cambridge.core.ui.component.CareerCompassPermissionDeniedState
import com.cambridge.core.ui.component.CareerCompassTopAppBar
import com.cambridge.core.ui.theme.CareerCompassTheme

@PreviewTest
@Preview(name = "Bottom bar - Feed", widthDp = 360, heightDp = 772)
@Composable
public fun CareerCompassBottomBarFeedPreview() {
    BottomBarPreview(selectedTab = CareerCompassBottomTab.Feed)
}

@PreviewTest
@Preview(name = "Bottom bar - Feed - Dark", uiMode = Configuration.UI_MODE_NIGHT_YES, widthDp = 360, heightDp = 772)
@Composable
public fun CareerCompassBottomBarFeedDarkPreview() {
    BottomBarPreview(selectedTab = CareerCompassBottomTab.Feed, darkTheme = true)
}

@PreviewTest
@Preview(name = "Bottom bar - Analysis", widthDp = 360, heightDp = 772)
@Composable
public fun CareerCompassBottomBarAnalysisPreview() {
    BottomBarPreview(selectedTab = CareerCompassBottomTab.Analysis)
}

@PreviewTest
@Preview(name = "Bottom bar - Applications", widthDp = 360, heightDp = 772)
@Composable
public fun CareerCompassBottomBarApplicationsPreview() {
    BottomBarPreview(selectedTab = CareerCompassBottomTab.Applications)
}

@PreviewTest
@Preview(name = "Bottom bar - My", widthDp = 360, heightDp = 772)
@Composable
public fun CareerCompassBottomBarMyPreview() {
    BottomBarPreview(selectedTab = CareerCompassBottomTab.My)
}

@PreviewTest
@Preview(name = "Top app bar - Back", widthDp = 360, heightDp = 772)
@Composable
public fun CareerCompassTopAppBarBackPreview() {
    TopAppBarPreview {
        CareerCompassTopAppBar(
            title = "공고 상세",
            onBackClick = {},
        )
    }
}

@PreviewTest
@Preview(name = "Top app bar - No back", widthDp = 360, heightDp = 772)
@Composable
public fun CareerCompassTopAppBarNoBackPreview() {
    TopAppBarPreview {
        CareerCompassTopAppBar(
            title = "맞춤 공고",
            onBackClick = null,
        )
    }
}

@PreviewTest
@Preview(name = "Top app bar - Subtitle", widthDp = 360, heightDp = 772)
@Composable
public fun CareerCompassTopAppBarSubtitlePreview() {
    TopAppBarPreview {
        CareerCompassTopAppBar(
            title = "지원서 작성",
            onBackClick = {},
            subtitle = "자동 저장됨",
        )
    }
}

@PreviewTest
@Preview(name = "Top app bar - Action", widthDp = 360, heightDp = 772)
@Composable
public fun CareerCompassTopAppBarActionPreview() {
    TopAppBarActionPreview(darkTheme = false)
}

@PreviewTest
@Preview(name = "Top app bar - Action - Dark", uiMode = Configuration.UI_MODE_NIGHT_YES, widthDp = 360, heightDp = 772)
@Composable
public fun CareerCompassTopAppBarActionDarkPreview() {
    TopAppBarActionPreview(darkTheme = true)
}

@Composable
private fun TopAppBarActionPreview(darkTheme: Boolean) {
    TopAppBarPreview(darkTheme = darkTheme) {
        CareerCompassTopAppBar(
            title = "지원서 작성",
            onBackClick = {},
            actions = {
                CareerCompassButton(
                    text = "저장",
                    onClick = {},
                    variant = CareerCompassButtonVariant.Ghost,
                    size = CareerCompassButtonSize.Small,
                )
            },
        )
    }
}

@PreviewTest
@Preview(name = "Card", widthDp = 360, heightDp = 772)
@Composable
public fun CareerCompassCardPreview() {
    CardPreview(darkTheme = false)
}

@PreviewTest
@Preview(name = "Card - Dark", uiMode = Configuration.UI_MODE_NIGHT_YES, widthDp = 360, heightDp = 772)
@Composable
public fun CareerCompassCardDarkPreview() {
    CardPreview(darkTheme = true)
}

@Composable
private fun CardPreview(darkTheme: Boolean) {
    ShellPreviewSurface(darkTheme = darkTheme) {
        Box(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(16.dp),
            contentAlignment = Alignment.Center,
        ) {
            CareerCompassCard(modifier = Modifier.fillMaxWidth()) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    CareerCompassBadge(
                        label = "채용",
                        tone = CareerCompassBadgeTone.Brand,
                    )
                    Text(
                        text = "2026 카카오 신입 개발자",
                        color = CareerCompassTheme.colors.onSurface,
                        style = CareerCompassTheme.typography.headline4,
                    )
                    Text(
                        text = "마감 D-7 · 적합도 88점",
                        color = CareerCompassTheme.colors.mutedContent,
                        style = CareerCompassTheme.typography.bodyMedium,
                    )
                }
            }
        }
    }
}

@PreviewTest
@Preview(name = "State - Network error", widthDp = 360, heightDp = 772)
@Composable
public fun CareerCompassNetworkErrorStatePreview() {
    NetworkErrorStatePreview(darkTheme = false)
}

@PreviewTest
@Preview(name = "State - Network error - Dark", uiMode = Configuration.UI_MODE_NIGHT_YES, widthDp = 360, heightDp = 772)
@Composable
public fun CareerCompassNetworkErrorStateDarkPreview() {
    NetworkErrorStatePreview(darkTheme = true)
}

@Composable
private fun NetworkErrorStatePreview(darkTheme: Boolean) {
    ShellPreviewSurface(darkTheme = darkTheme) {
        CareerCompassNetworkErrorState(
            onRetryClick = {},
            onOfflineClick = {},
        )
    }
}

@PreviewTest
@Preview(name = "State - Analyzing", widthDp = 360, heightDp = 772)
@Composable
public fun CareerCompassAnalyzingStatePreview() {
    ShellPreviewSurface {
        CareerCompassAnalyzingState(
            title = "AI가 분석 중이에요",
            description = "당신의 경험 카드와 공고를 비교하고 있어요",
            progress = 0.75f,
            progressLabel = "3/4 단계 · 약 10초 남음",
        )
    }
}

@PreviewTest
@Preview(name = "State - Empty", widthDp = 360, heightDp = 772)
@Composable
public fun CareerCompassEmptyStatePreview() {
    ShellPreviewSurface {
        CareerCompassEmptyState(
            title = "‘블록체인’에 해당하는 공고가 없어요",
            description = "관심 분야를 등록해 두면 새 공고가 올라올 때 알림을 보내드릴게요",
            actionText = "관심 분야에 추가",
            onActionClick = {},
        )
    }
}

@PreviewTest
@Preview(name = "State - Permission denied", widthDp = 360, heightDp = 772)
@Composable
public fun CareerCompassPermissionDeniedStatePreview() {
    PermissionDeniedStatePreview(darkTheme = false)
}

@PreviewTest
@Preview(name = "State - Permission denied - Dark", uiMode = Configuration.UI_MODE_NIGHT_YES, widthDp = 360, heightDp = 772)
@Composable
public fun CareerCompassPermissionDeniedStateDarkPreview() {
    PermissionDeniedStatePreview(darkTheme = true)
}

@Composable
private fun PermissionDeniedStatePreview(darkTheme: Boolean) {
    ShellPreviewSurface(darkTheme = darkTheme) {
        CareerCompassPermissionDeniedState(
            title = "알림 권한이 꺼져 있어요",
            description = "마감 알림과 새 공고를 받으려면 알림 권한이 필요해요",
            benefits =
                listOf(
                    "⏰ 마감 D-3, D-1 알림",
                    "✨ 적합도 80+ 신규 공고",
                    "📊 주간 진단 리포트",
                ),
            onOpenSettingsClick = {},
            onLaterClick = {},
        )
    }
}

@PreviewTest
@Preview(name = "State - Maintenance", widthDp = 360, heightDp = 772)
@Composable
public fun CareerCompassMaintenanceStatePreview() {
    ShellPreviewSurface {
        CareerCompassMaintenanceState(
            title = "서비스가 잠시 점검 중이에요",
            description = "AI 분석 서버를 업데이트하고 있어요\n예상 종료 시각 19:00",
            statusLabel = "● 점검 진행 중",
            onRefreshClick = {},
            onOfflineClick = null,
            contactLabel = "문의 · help@careercompass.app",
        )
    }
}

@Composable
private fun BottomBarPreview(
    selectedTab: CareerCompassBottomTab,
    darkTheme: Boolean = false,
) {
    ShellPreviewSurface(darkTheme = darkTheme) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.BottomCenter,
        ) {
            CareerCompassBottomBar(
                selectedTab = selectedTab,
                onTabClick = {},
            )
        }
    }
}

@Composable
private fun TopAppBarPreview(
    darkTheme: Boolean = false,
    content: @Composable () -> Unit,
) {
    ShellPreviewSurface(darkTheme = darkTheme) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.TopCenter,
        ) {
            content()
        }
    }
}

@Composable
private fun ShellPreviewSurface(
    darkTheme: Boolean = false,
    content: @Composable () -> Unit,
) {
    CareerCompassTheme(darkTheme = darkTheme) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = CareerCompassTheme.colors.subtleSurface,
            content = content,
        )
    }
}
