package com.cambridge.careercompass_fe.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.remember
import androidx.navigation.NavDestination
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import com.cambridge.core.ui.component.CareerCompassBottomTab
import kotlin.reflect.KClass

/**
 * 앱 셸 내비게이션 상태 — 하단 탭 표시 여부와 선택 탭 판정, 탭 전환 규칙.
 *
 * 탭 ↔ 라우트 매핑은 [tabRoutes] 하나가 정본이다. 값은 `@Serializable data object` 라우트 **인스턴스**다 —
 * `KClass.objectInstance` 는 kotlin-reflect 가 있어야 동작해 런타임에서 쓰지 않는다.
 */
@Stable
public class AppState(
    public val navController: NavHostController,
    private val tabRoutes: Map<CareerCompassBottomTab, Any>,
    private val bottomBarRoutes: Set<KClass<*>>,
    private val mainRoot: KClass<*>,
) {
    public fun shouldShowBottomBar(currentDestination: NavDestination?): Boolean =
        currentDestination != null && bottomBarRoutes.any { route -> currentDestination.hasRoute(route) }

    public fun currentTab(currentDestination: NavDestination?): CareerCompassBottomTab =
        tabRoutes.entries
            .firstOrNull { (_, route) ->
                currentDestination?.hierarchy?.any { destination -> destination.hasRoute(route::class) } == true
            }?.key ?: CareerCompassBottomTab.Feed

    /** 탭 전환 — 메인 루트를 백스택에 남기고 탭 상태를 저장·복원한다. */
    public fun navigateToTab(tab: CareerCompassBottomTab) {
        val route = tabRoutes.getValue(tab)
        navController.navigate(route) {
            popUpTo(mainRoot) { saveState = true }
            launchSingleTop = true
            restoreState = true
        }
    }
}

@Composable
public fun rememberAppState(
    tabRoutes: Map<CareerCompassBottomTab, Any>,
    bottomBarRoutes: Set<KClass<*>>,
    mainRoot: KClass<*>,
    navController: NavHostController = rememberNavController(),
): AppState =
    remember(navController, tabRoutes, bottomBarRoutes, mainRoot) {
        AppState(navController, tabRoutes, bottomBarRoutes, mainRoot)
    }
