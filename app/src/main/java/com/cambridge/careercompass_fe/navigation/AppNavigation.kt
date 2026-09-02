package com.cambridge.careercompass_fe.navigation

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import com.cambridge.careercompass_fe.session.AppStartDestination
import com.cambridge.core.ui.component.CareerCompassBottomBar
import com.cambridge.core.ui.component.CareerCompassBottomTab
import com.cambridge.core.ui.theme.CareerCompassTheme

/** 계측 smoke(`ApiBoundarySmokeAndroidTest`)가 앱 시작 화면을 찾는 시맨틱 태그. */
internal const val APP_START_SEMANTICS_TAG = "careercompass_app_start"

/**
 * 앱 셸 — 하단 탭 Scaffold 와 최상위 NavHost.
 *
 * feature 그래프(온보딩·피드)는 각 모듈의 `navigation/` 확장으로 붙는다. 다른 담당 모듈의 탭은 진입점이
 * 생길 때까지 [PlaceholderTabScreen] 이다.
 *
 * @param onSessionEnded 로그아웃·세션 만료로 인증 이전 상태가 됐을 때 시작 목적지를 다시 계산하게 한다.
 */
@Composable
public fun AppNavigation(
    startDestination: AppStartDestination,
    onSessionEnded: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val appState =
        rememberAppState(
            tabRoutes = TAB_ROUTES,
            bottomBarRoutes = BOTTOM_BAR_ROUTES,
            mainRoot = Route.MainTab::class,
        )
    val navEntry by appState.navController.currentBackStackEntryAsState()
    val currentDestination = navEntry?.destination
    val showBottomBar = appState.shouldShowBottomBar(currentDestination)
    val currentTab = appState.currentTab(currentDestination)

    Surface(
        modifier = modifier.testTag(APP_START_SEMANTICS_TAG),
        color = CareerCompassTheme.colors.subtleSurface,
    ) {
        Scaffold(
            containerColor = CareerCompassTheme.colors.subtleSurface,
            contentWindowInsets = WindowInsets.systemBars.only(WindowInsetsSides.Horizontal + WindowInsetsSides.Bottom),
            bottomBar = {
                if (showBottomBar) {
                    CareerCompassBottomBar(selectedTab = currentTab, onTabClick = appState::navigateToTab)
                }
            },
        ) { innerPadding ->
            NavHost(
                modifier = Modifier.padding(innerPadding),
                navController = appState.navController,
                startDestination = startDestination.toRoute(),
            ) {
                composable<Route.MainTab> { PlaceholderTabScreen(tab = CareerCompassBottomTab.Feed) }
                composable<Route.AnalysisTab> { PlaceholderTabScreen(tab = CareerCompassBottomTab.Analysis) }
                composable<Route.ApplicationsTab> { PlaceholderTabScreen(tab = CareerCompassBottomTab.Applications) }
                composable<Route.MyTab> { PlaceholderTabScreen(tab = CareerCompassBottomTab.My) }
                composable<Route.AuthPlaceholder> { PlaceholderTabScreen(tab = CareerCompassBottomTab.Feed) }
            }
        }
    }
}

// 온보딩·피드 그래프가 붙기 전까지의 매핑. 그래프가 붙으면 Login/Biometric/Onboarding 은 온보딩 그래프 루트로,
// Main 은 피드 홈으로 바뀐다.
private fun AppStartDestination.toRoute(): Route =
    when (this) {
        AppStartDestination.Login, AppStartDestination.BiometricLogin, AppStartDestination.Onboarding -> Route.AuthPlaceholder
        AppStartDestination.Main -> Route.MainTab
    }

private val TAB_ROUTES =
    mapOf(
        CareerCompassBottomTab.Feed to Route.MainTab::class,
        CareerCompassBottomTab.Analysis to Route.AnalysisTab::class,
        CareerCompassBottomTab.Applications to Route.ApplicationsTab::class,
        CareerCompassBottomTab.My to Route.MyTab::class,
    )

private val BOTTOM_BAR_ROUTES = TAB_ROUTES.values.toSet()
