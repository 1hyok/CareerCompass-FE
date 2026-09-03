package com.cambridge.careercompass_fe.navigation

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.toRoute
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
 * 다른 담당 모듈(foryou·editor·profile·notification)의 화면은 진입점이 생길 때까지 자리표시자다 — 마이 탭만
 * 예외적으로 세션 카드와 지문 로그인 스위치·로그아웃을 그린다([MyTabPlaceholderEntry]). 그 둘 말고는 세션을 끝낼
 * 방법도, 기기에 남은 지문 등록을 되돌릴 방법도 없어서다.
 *
 * @param pendingDeepLink 아직 적용하지 않은 딥링크. 피드 그래프 안에 있을 때만 이동하고 [onDeepLinkConsumed] 로 비운다 —
 *   로그인·온보딩 중에 받은 것은 인증을 마치고 피드 그래프에 들어온 순간 적용된다.
 * @param onSessionEnded 로그아웃·세션 만료로 인증 이전 상태가 됐을 때 시작 목적지를 다시 계산하게 한다.
 * @param onExitRequest 온보딩 첫 화면에서 뒤로 가기 — 앱을 나간다.
 */
@Composable
public fun AppNavigation(
    startDestination: AppStartDestination,
    pendingDeepLink: AppDeepLink?,
    onDeepLinkConsumed: () -> Unit,
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

    // 딥링크는 인증 게이트 뒤에서만 적용한다 — navDeepLink 로 NavHost 에 맡기면 시작 목적지가 로그인·온보딩이어도 상세가
    // 백스택에 올라 인증을 우회한다. 피드 그래프 안에 있을 때만 이동하고, 인증 흐름 중이면 navigateToMain 뒤 목적지가
    // 바뀌어 이 효과가 다시 돌 때 적용된다.
    val onDeepLinkConsumedState by rememberUpdatedState(onDeepLinkConsumed)
    LaunchedEffect(pendingDeepLink, currentDestination) {
        if (pendingDeepLink == null || !appState.isInMainRoot(currentDestination)) return@LaunchedEffect
        when (pendingDeepLink) {
            is AppDeepLink.PostingDetail -> {
                // 같은 상세가 이미 최상단이면 다시 쌓지 않는다(소비 전 재실행에도 안전). 다른 상세면 유사 공고 이동처럼 위에 쌓는다.
                if (navEntry?.isPostingDetail(pendingDeepLink.postingId) != true) {
                    navController.navigate(FeedRoute.PostingDetail(pendingDeepLink.postingId))
                }
            }
        }
        onDeepLinkConsumedState()
    }

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
                composable<Route.MyTab> { MyTabPlaceholderEntry(onSessionEnded = onSessionEnded) }
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

/** 이 백스택 항목이 [postingId] 의 공고 상세인가. */
private fun NavBackStackEntry.isPostingDetail(postingId: Long): Boolean =
    destination.hasRoute<FeedRoute.PostingDetail>() && toRoute<FeedRoute.PostingDetail>().postingId == postingId

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

private val TAB_ROUTES: Map<CareerCompassBottomTab, Any> =
    mapOf(
        CareerCompassBottomTab.Feed to FeedGraphRoute,
        CareerCompassBottomTab.Analysis to Route.AnalysisTab,
        CareerCompassBottomTab.Applications to Route.ApplicationsTab,
        CareerCompassBottomTab.My to Route.MyTab,
    )

/** 하단 탭을 그리는 화면 — 피드 홈과 자리표시자 탭. 피드 상세·원문·게시판 화면은 탭을 숨긴다. */
private val BOTTOM_BAR_ROUTES =
    setOf(
        FeedRoute.Home::class,
        Route.AnalysisTab::class,
        Route.ApplicationsTab::class,
        Route.MyTab::class,
    )
