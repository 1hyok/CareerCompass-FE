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
import com.cambridge.feature.onboarding.presentation.navigation.OnboardingGraphRoute
import com.cambridge.feature.onboarding.presentation.navigation.onboardingNavGraph

/** 계측 smoke(`ApiBoundarySmokeAndroidTest`)가 앱 시작 화면을 찾는 시맨틱 태그. */
internal const val APP_START_SEMANTICS_TAG = "careercompass_app_start"

/**
 * 앱 셸 — 하단 탭 Scaffold 와 최상위 NavHost.
 *
 * 시작 목적지가 인증 계열(로그인·지문·온보딩)이면 온보딩 그래프에서, 메인이면 피드 탭에서 시작한다.
 * 다른 담당 모듈의 탭은 진입점이 생길 때까지 [PlaceholderTabScreen] 이다.
 *
 * @param onSessionEnded 로그아웃·세션 만료로 인증 이전 상태가 됐을 때 시작 목적지를 다시 계산하게 한다.
 * @param onExitRequest 온보딩 첫 화면에서 뒤로 가기 — 앱을 나간다.
 */
@Composable
public fun AppNavigation(
    startDestination: AppStartDestination,
    onSessionEnded: () -> Unit,
    onExitRequest: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val appState =
        rememberAppState(
            tabRoutes = TAB_ROUTES,
            bottomBarRoutes = BOTTOM_BAR_ROUTES,
            mainRoot = Route.MainTab::class,
        )
    val navController = appState.navController
    val navEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navEntry?.destination
    val showBottomBar = appState.shouldShowBottomBar(currentDestination)
    val currentTab = appState.currentTab(currentDestination)

    val navigateToMain: () -> Unit = {
        navController.navigate(Route.MainTab) {
            // 인증·온보딩 흐름 전체를 비우고 메인 진입 — 뒤로가기로 인증 화면에 돌아가지 않는다(앱 종료).
            popUpTo(0) { inclusive = true }
            launchSingleTop = true
        }
    }
    val onboardingNavActions =
        rememberOnboardingNavActions(
            navController = navController,
            onExitRequest = onExitRequest,
            navigateToMain = navigateToMain,
            // 게시판 등록 화면은 피드 그래프(#64) 배선 뒤 그 라우트로 바꾼다. 그전까지는 메인으로 보낸다.
            navigateToBoardRegister = navigateToMain,
        )

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
                navController = navController,
                startDestination = startDestination.toTopLevelRoute(),
            ) {
                onboardingNavGraph(
                    startDestination =
                        if (startDestination == AppStartDestination.Main) {
                            com.cambridge.feature.onboarding.presentation.navigation.OnboardingRoute.Login
                        } else {
                            startDestination.toOnboardingStart()
                        },
                    graphScopedParentEntry = { navController.onboardingGraphEntry() },
                    actions = onboardingNavActions,
                )
                composable<Route.MainTab> { PlaceholderTabScreen(tab = CareerCompassBottomTab.Feed) }
                composable<Route.AnalysisTab> { PlaceholderTabScreen(tab = CareerCompassBottomTab.Analysis) }
                composable<Route.ApplicationsTab> { PlaceholderTabScreen(tab = CareerCompassBottomTab.Applications) }
                composable<Route.MyTab> { PlaceholderTabScreen(tab = CareerCompassBottomTab.My) }
            }
        }
    }
}

private fun AppStartDestination.toTopLevelRoute(): Any =
    when (this) {
        AppStartDestination.Login, AppStartDestination.BiometricLogin, AppStartDestination.Onboarding -> OnboardingGraphRoute
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
