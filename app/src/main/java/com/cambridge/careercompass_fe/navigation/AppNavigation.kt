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
import androidx.compose.ui.res.stringResource
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import com.cambridge.careercompass_fe.R
import com.cambridge.careercompass_fe.session.AppStartDestination
import com.cambridge.core.ui.component.CareerCompassBottomBar
import com.cambridge.core.ui.component.CareerCompassBottomTab
import com.cambridge.core.ui.theme.CareerCompassTheme
import com.cambridge.feature.feed.presentation.navigation.FeedGraphRoute
import com.cambridge.feature.feed.presentation.navigation.FeedRoute
import com.cambridge.feature.feed.presentation.navigation.feedNavGraph
import com.cambridge.feature.onboarding.presentation.navigation.OnboardingGraphRoute
import com.cambridge.feature.onboarding.presentation.navigation.OnboardingRoute
import com.cambridge.feature.onboarding.presentation.navigation.onboardingNavGraph

/** 계측 smoke(`ApiBoundarySmokeAndroidTest`)가 앱 시작 화면을 찾는 시맨틱 태그. */
internal const val APP_START_SEMANTICS_TAG = "careercompass_app_start"

/**
 * 앱 셸 — 하단 탭 Scaffold 와 최상위 NavHost.
 *
 * 시작 목적지가 인증 계열(로그인·지문·온보딩)이면 온보딩 그래프에서, 메인이면 피드 그래프에서 시작한다.
 * 하단 탭은 피드 홈과 자리표시자 탭에서만 보이고 상세·원문·게시판 화면에서는 숨긴다.
 * 다른 담당 모듈(foryou·editor·profile·notification)의 화면은 진입점이 생길 때까지 자리표시자다.
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
            mainRoot = FeedGraphRoute::class,
        )
    val navController = appState.navController
    val navEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navEntry?.destination
    val showBottomBar = appState.shouldShowBottomBar(currentDestination)
    val currentTab = appState.currentTab(currentDestination)

    val navigateToMain: () -> Unit = {
        navController.navigate(FeedGraphRoute) {
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
            navigateToBoardRegister = {
                navigateToMain()
                navController.navigate(FeedRoute.BoardRegister) { launchSingleTop = true }
            },
        )
    val feedNavActions =
        rememberFeedNavActions(
            navController = navController,
            navigateToMyTab = { appState.navigateToTab(CareerCompassBottomTab.My) },
            onSessionEnded = onSessionEnded,
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
                    startDestination = startDestination.toOnboardingStart(),
                    graphScopedParentEntry = { navController.onboardingGraphEntry() },
                    actions = onboardingNavActions,
                )
                feedNavGraph(actions = feedNavActions)
                composable<Route.AnalysisTab> { PlaceholderTabScreen(tab = CareerCompassBottomTab.Analysis) }
                composable<Route.ApplicationsTab> { PlaceholderTabScreen(tab = CareerCompassBottomTab.Applications) }
                composable<Route.MyTab> { PlaceholderTabScreen(tab = CareerCompassBottomTab.My) }
                composable<Route.NotificationsPlaceholder> {
                    PlaceholderScreen(
                        title = stringResource(R.string.placeholder_notifications_title),
                        onBackClick = { navController.popBackStack() },
                    )
                }
            }
        }
    }
}

private fun AppStartDestination.toTopLevelRoute(): Any =
    when (this) {
        AppStartDestination.Login, AppStartDestination.BiometricLogin, AppStartDestination.Onboarding -> OnboardingGraphRoute
        AppStartDestination.Main -> FeedGraphRoute
    }

/** 온보딩 그래프의 시작 화면. 메인으로 시작해도 로그아웃·세션 만료 뒤 그래프에 들어올 수 있어 로그인으로 둔다. */
private fun AppStartDestination.toOnboardingStart(): OnboardingRoute =
    when (this) {
        AppStartDestination.Login, AppStartDestination.Main -> OnboardingRoute.Login
        AppStartDestination.BiometricLogin -> OnboardingRoute.BiometricLogin
        AppStartDestination.Onboarding -> OnboardingRoute.Step1
    }

private val TAB_ROUTES =
    mapOf(
        CareerCompassBottomTab.Feed to FeedGraphRoute::class,
        CareerCompassBottomTab.Analysis to Route.AnalysisTab::class,
        CareerCompassBottomTab.Applications to Route.ApplicationsTab::class,
        CareerCompassBottomTab.My to Route.MyTab::class,
    )

/** 하단 탭을 그리는 화면 — 피드 홈과 자리표시자 탭. 피드 상세·원문·게시판 화면은 탭을 숨긴다. */
private val BOTTOM_BAR_ROUTES =
    setOf(
        FeedRoute.Home::class,
        Route.AnalysisTab::class,
        Route.ApplicationsTab::class,
        Route.MyTab::class,
    )
