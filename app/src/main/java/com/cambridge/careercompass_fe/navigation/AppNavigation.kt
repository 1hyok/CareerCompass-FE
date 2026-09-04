package com.cambridge.careercompass_fe.navigation

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.consumeWindowInsets
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
import com.cambridge.careercompass_fe.session.SessionEndCause
import com.careercompass.core.ui.component.CareerCompassBottomBar
import com.careercompass.core.ui.component.CareerCompassBottomTab
import com.careercompass.core.ui.theme.CareerCompassTheme
import com.careercompass.feature.feed.presentation.navigation.FeedGraphRoute
import com.careercompass.feature.feed.presentation.navigation.FeedRoute
import com.careercompass.feature.feed.presentation.navigation.feedNavGraph
import com.careercompass.feature.onboarding.presentation.navigation.OnboardingGraphRoute
import com.careercompass.feature.onboarding.presentation.navigation.OnboardingRoute
import com.careercompass.feature.onboarding.presentation.navigation.onboardingNavGraph

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
 * 세션이 왜 끝났는지는 화면이 아니라 여기서 [SessionEndCause] 로 갈라 셸에 넘긴다 — 401 을 만난 피드·온보딩 계열은
 * 만료, 마이 탭은 로그아웃이다. 안내를 보일지는 셸이 정하고, 로그인 화면은 [isSessionExpiryNoticeVisible] 이라는
 * 입력만 받는다(#128).
 *
 * @param isSessionExpiryNoticeVisible 로그인 화면에 「로그인이 만료됐다」를 알릴지. 셸이 켠다.
 * @param onSessionExpiryNoticeDismissed 그 안내를 닫았거나 다시 로그인을 시도했다 — 셸이 끈다.
 * @param pendingDeepLink 아직 적용하지 않은 딥링크. 피드 그래프 안에 있을 때만 이동하고 [onDeepLinkConsumed] 로 비운다 —
 *   로그인·온보딩 중에 받은 것은 인증을 마치고 피드 그래프에 들어온 순간 적용된다.
 * @param onSessionEnded 로그아웃·세션 만료로 인증 이전 상태가 됐을 때 사유와 함께 시작 목적지를 다시 계산하게 한다.
 * @param onAuthSessionExpired 지문 확인 뒤 세션 검증이 만료를 알렸다 — 그래프가 스스로 로그인 화면으로 옮기므로
 *   재계산 없이 사유만 남긴다.
 * @param onExitRequest 온보딩 첫 화면에서 뒤로 가기 — 앱을 나간다.
 */
@Composable
public fun AppNavigation(
    startDestination: AppStartDestination,
    isSessionExpiryNoticeVisible: Boolean,
    onSessionExpiryNoticeDismissed: () -> Unit,
    pendingDeepLink: AppDeepLink?,
    onDeepLinkConsumed: () -> Unit,
    onSessionEnded: (SessionEndCause) -> Unit,
    onAuthSessionExpired: () -> Unit,
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
            onAuthSessionExpired = onAuthSessionExpired,
            // 온보딩 그래프가 세션 종료를 알리는 경우도 401 하나뿐이다(`OnboardingNavActions.onSessionEnded`).
            onSessionEnded = { onSessionEnded(SessionEndCause.Expired) },
        )
    val feedNavActions =
        rememberFeedNavActions(
            navController = navController,
            navigateToMyTab = { appState.navigateToTab(CareerCompassBottomTab.My) },
            // 피드 그래프가 세션 종료를 알리는 경우는 401 하나뿐이다(`FeedNavActions.onSessionEnded`).
            onSessionEnded = { onSessionEnded(SessionEndCause.Expired) },
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
                // `padding` 은 인셋을 **소비하지 않는다** — 자리만 비울 뿐이라, 아래 화면들이 다시 읽는
                // `WindowInsets.safeDrawing` 에는 여기서 이미 비운 시스템 바가 그대로 남아 있었다. 그래서 모든
                // 화면 아래에 내비게이션 바 높이만큼(3버튼 48dp) 죽은 여백이 생겼다(#145). 소비를 먼저 선언해
                // 자식이 「남은 인셋」만 보게 한다 — 인셋의 주인은 이 셸이고, 화면들은 셸 밖에서 단독으로 그려질
                // 때(스크린샷 골든)도 스스로를 지키도록 safeDrawing 을 그대로 둔다.
                modifier = Modifier.consumeWindowInsets(innerPadding).padding(innerPadding),
                navController = navController,
                startDestination = startDestination.toTopLevelRoute(),
            ) {
                onboardingNavGraph(
                    startDestination = startDestination.toOnboardingStart(),
                    graphScopedParentEntry = { navController.onboardingGraphEntry() },
                    actions = onboardingNavActions,
                    isSessionExpiryNoticeVisible = isSessionExpiryNoticeVisible,
                    onSessionExpiryNoticeDismissed = onSessionExpiryNoticeDismissed,
                )
                feedNavGraph(actions = feedNavActions)
                composable<Route.AnalysisTab> { PlaceholderTabScreen(tab = CareerCompassBottomTab.Analysis) }
                composable<Route.ApplicationsTab> { PlaceholderTabScreen(tab = CareerCompassBottomTab.Applications) }
                composable<Route.MyTab> {
                    // 마이 탭에서 세션이 끝나는 길은 로그아웃 버튼뿐이다 — 사용자가 한 일이라 안내하지 않는다.
                    MyTabPlaceholderEntry(onSessionEnded = { onSessionEnded(SessionEndCause.LoggedOut) })
                }
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
