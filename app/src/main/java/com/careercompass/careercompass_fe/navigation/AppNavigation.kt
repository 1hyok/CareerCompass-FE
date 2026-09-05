package com.careercompass.careercompass_fe.navigation

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import com.careercompass.careercompass_fe.R
import com.careercompass.careercompass_fe.session.AppStartDestination
import com.careercompass.careercompass_fe.session.SessionEndCause
import com.careercompass.core.ui.component.CareerCompassBottomBar
import com.careercompass.core.ui.component.CareerCompassBottomTab
import com.careercompass.core.ui.theme.CareerCompassTheme
import com.careercompass.feature.feed.presentation.navigation.FeedEntryRequest
import com.careercompass.feature.feed.presentation.navigation.FeedGraphRoute
import com.careercompass.feature.feed.presentation.navigation.FeedNavHost
import com.careercompass.feature.onboarding.presentation.navigation.OnboardingGraphRoute
import com.careercompass.feature.onboarding.presentation.navigation.OnboardingNavHost
import com.careercompass.feature.onboarding.presentation.navigation.OnboardingRoute

/** 계측 smoke(`ApiBoundarySmokeAndroidTest`)가 앱 시작 화면을 찾는 시맨틱 태그. */
internal const val APP_START_SEMANTICS_TAG = "careercompass_app_start"

/**
 * 앱 셸 — 하단 탭 Scaffold 와 최상위 NavHost.
 *
 * 온보딩과 피드는 각 피처가 소유하는 로컬 Navigation 3 스택이다(#259). 루트 Nav2 에는 host destination 하나씩만 남고,
 * 그 안의 push/pop 은 피처가 처리한다. 셸이 갖는 것은 시작 목적지 분기, 탭 전환, 로컬 스택 바닥에서의 back, 세션
 * 판정, 그리고 다른 담당 모듈의 자리표시자다. 루트 자체를 `NavDisplay` 로 바꾸는 것은 #260 이 한다.
 *
 * 시작 목적지가 인증 계열(로그인·지문·온보딩)이면 온보딩 host 에서, 메인이면 피드 host 에서 시작한다.
 * 하단 탭은 피드 홈과 자리표시자 탭에서만 보이고 상세·원문·게시판 화면에서는 숨긴다 — 피드의 Nav2 destination 은
 * 하나뿐이라 깊이는 피드 host 가 [FeatureStackBoundary.onAtRootChanged] 로 올려 준다.
 * 다른 담당 모듈(foryou·editor·profile·notification)의 화면은 진입점이 생길 때까지 자리표시자다 — 마이 탭만
 * 예외적으로 세션 카드와 지문 로그인 스위치·로그아웃을 그린다([MyTabPlaceholderScreen]). 그 둘 말고는 세션을 끝낼
 * 방법도, 기기에 남은 지문 등록을 되돌릴 방법도 없어서다.
 *
 * 세션이 왜 끝났는지는 화면이 아니라 여기서 [SessionEndCause] 로 갈라 셸에 넘긴다 — 401 을 만난 피드·온보딩 계열은
 * 만료, 마이 탭은 로그아웃이다. 안내를 보일지는 셸이 정하고, 로그인 화면은 [isSessionExpiryNoticeVisible] 이라는
 * 입력만 받는다(#128).
 *
 * @param isSessionExpiryNoticeVisible 로그인 화면에 「로그인이 만료됐다」를 알릴지. 셸이 켠다.
 * @param onSessionExpiryNoticeDismissed 그 안내를 닫았거나 다시 로그인을 시도했다 — 셸이 끈다.
 * @param pendingDeepLink 아직 적용하지 않은 딥링크. 피드 host 가 그려질 때만 반영되고 [onDeepLinkConsumed] 로 비운다 —
 *   로그인·온보딩 중에 받은 것은 인증을 마치고 피드 host 에 들어온 순간 적용된다.
 * @param onSessionEnded 로그아웃·세션 만료로 인증 이전 상태가 됐을 때 사유와 함께 시작 목적지를 다시 계산하게 한다.
 * @param onAuthSessionExpired 지문 확인 뒤 세션 검증이 만료를 알렸다 — 온보딩 스택이 스스로 로그인 화면으로 옮기므로
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

    // 피드 로컬 스택의 깊이는 Nav2 destination 에 안 보인다 — 피드 host 가 올려 주는 신호를 바텀바 판정에 합성한다.
    // 피드를 떠나면 host 가 true 로 되돌려 다른 탭 판정을 오염시키지 않는다(FeatureNavDisplay).
    var isFeedStackAtRoot by remember { mutableStateOf(true) }
    val showBottomBar = appState.shouldShowBottomBar(currentDestination, isFeedStackAtRoot)
    val currentTab = appState.currentTab(currentDestination)

    val navigateToMain: () -> Unit = {
        navController.navigate(FeedGraphRoute) {
            // 인증·온보딩 흐름 전체를 비우고 메인 진입 — 뒤로가기로 인증 화면에 돌아가지 않는다(앱 종료).
            popUpTo(0) { inclusive = true }
            launchSingleTop = true
        }
    }
    // 온보딩 완료의 「게시판 먼저 등록하기」 — 셸은 피드 로컬 스택에 push 할 수 없으므로 진입 요청으로 남겨 host 가
    // 그려질 때 반영하게 한다. 프로세스 재생성을 건너야 하므로 rememberSaveable 이다.
    var boardRegisterRequested by rememberSaveable { mutableStateOf(false) }
    val onboardingExternalActions =
        rememberOnboardingExternalActions(
            navigateToMain = navigateToMain,
            navigateToBoardRegister = {
                boardRegisterRequested = true
                navigateToMain()
            },
            onAuthSessionExpired = onAuthSessionExpired,
            // 온보딩 스택이 세션 종료를 알리는 경우도 401 하나뿐이다(`OnboardingExternalActions.onSessionEnded`).
            onSessionEnded = { onSessionEnded(SessionEndCause.Expired) },
        )
    val feedExternalActions =
        rememberFeedExternalActions(
            navController = navController,
            navigateToMyTab = { appState.navigateToTab(CareerCompassBottomTab.My) },
            // 피드 스택이 세션 종료를 알리는 경우는 401 하나뿐이다(`FeedExternalActions.onSessionEnded`).
            onSessionEnded = { onSessionEnded(SessionEndCause.Expired) },
        )

    // 로컬 스택 바닥에서의 back 은 루트 백스택 pop 으로 돌려준다. 온보딩은 루트에 더 걷어낼 화면이 없으면 앱을 나간다.
    val onboardingBoundary = rememberRootPopBoundary(navController, onRootEmpty = onExitRequest, onAtRootChanged = null)
    val feedBoundary =
        rememberRootPopBoundary(
            navController,
            onRootEmpty = null,
            onAtRootChanged = { isAtRoot -> isFeedStackAtRoot = isAtRoot },
        )

    // 딥링크는 인증 게이트 뒤에서만 적용한다 — navDeepLink 로 NavHost 에 맡기면 시작 목적지가 로그인·온보딩이어도 상세가
    // 백스택에 올라 인증을 우회한다. 피드 host 는 피드 그래프 안에서만 그려지므로 그 안에서만 반영되고, 인증 흐름 중이면
    // navigateToMain 뒤 host 가 그려질 때 적용된다. 온보딩의 게시판 등록 요청이 먼저다 — 사용자가 방금 고른 것이다.
    val pendingFeedEntry: FeedEntryRequest? =
        when {
            boardRegisterRequested -> FeedEntryRequest.BoardRegister
            pendingDeepLink is AppDeepLink.PostingDetail -> FeedEntryRequest.PostingDetail(pendingDeepLink.postingId)
            else -> null
        }
    val onPendingFeedEntryConsumed: () -> Unit = {
        if (boardRegisterRequested) boardRegisterRequested = false else onDeepLinkConsumed()
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
                // ── Navigation 3 로컬 스택을 가진 그래프 (#259) — 루트엔 host destination 하나씩만 둔다.
                composable<OnboardingGraphRoute> {
                    OnboardingNavHost(
                        startDestination = startDestination.toOnboardingStart(),
                        boundary = onboardingBoundary,
                        externalActions = onboardingExternalActions,
                        isSessionExpiryNoticeVisible = isSessionExpiryNoticeVisible,
                        onSessionExpiryNoticeDismissed = onSessionExpiryNoticeDismissed,
                    )
                }
                composable<FeedGraphRoute> {
                    FeedNavHost(
                        boundary = feedBoundary,
                        externalActions = feedExternalActions,
                        pendingEntry = pendingFeedEntry,
                        onPendingEntryConsumed = onPendingFeedEntryConsumed,
                    )
                }

                // ── 다른 담당 모듈의 자리표시자 — 진입점이 생기면 그 모듈의 host 로 바뀐다.
                composable<Route.AnalysisTab> { PlaceholderTabScreen(tab = CareerCompassBottomTab.Analysis) }
                composable<Route.ApplicationsTab> { PlaceholderTabScreen(tab = CareerCompassBottomTab.Applications) }
                composable<Route.MyTab> {
                    // 마이 탭에서 세션이 끝나는 길은 로그아웃 버튼뿐이다 — 사용자가 한 일이라 안내하지 않는다.
                    MyTabPlaceholderScreen(onSessionEnded = { onSessionEnded(SessionEndCause.LoggedOut) })
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

private fun AppStartDestination.toTopLevelRoute(): Any =
    when (this) {
        AppStartDestination.Login, AppStartDestination.BiometricLogin, AppStartDestination.Onboarding -> OnboardingGraphRoute
        AppStartDestination.Main -> FeedGraphRoute
    }

/** 온보딩 스택의 시작 화면. 메인으로 시작해도 로그아웃·세션 만료 뒤 스택에 들어올 수 있어 로그인으로 둔다. */
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

/**
 * 하단 탭을 그리는 자리표시자 탭. 피드는 이 목록이 아니라 로컬 스택 깊이로 판정한다 — 홈에서만 보이고 상세·원문·게시판
 * 화면은 탭을 숨긴다.
 */
private val BOTTOM_BAR_ROUTES =
    setOf(
        Route.AnalysisTab::class,
        Route.ApplicationsTab::class,
        Route.MyTab::class,
    )
