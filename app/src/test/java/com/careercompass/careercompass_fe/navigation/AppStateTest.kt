package com.careercompass.careercompass_fe.navigation

import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import com.careercompass.core.ui.component.CareerCompassBottomTab
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 루트 백스택의 모양 회귀 기준(#260) — Nav2 의 `popUpTo(피드) { saveState }` + `restoreState` 와 `popUpTo(0)` 이 만들던
 * 결과를 스택 모양으로 그대로 못박는다. 컴포지션이 필요 없어 Robolectric 없이 돈다.
 */
class AppStateTest {
    private fun stateOn(vararg keys: Route): AppState = AppState(NavBackStack<NavKey>(*keys))

    private fun AppState.shape(): List<String> = backStack.map { it::class.simpleName!! }

    @Test
    fun `탭 전환은 피드를 바닥에 남기고 다른 탭을 그 위 한 칸으로 교체한다`() {
        val state = stateOn(Route.Feed)

        state.navigateToTab(CareerCompassBottomTab.My)
        assertEquals(listOf("Feed", "MyTab"), state.shape())

        state.navigateToTab(CareerCompassBottomTab.Analysis)
        assertEquals(listOf("Feed", "AnalysisTab"), state.shape())

        state.navigateToTab(CareerCompassBottomTab.Feed)
        assertEquals(listOf("Feed"), state.shape())
    }

    @Test
    fun `다른 탭에서의 뒤로가기는 피드로 돌아가고 피드 바닥에서는 내리지 않는다`() {
        val state = stateOn(Route.Feed)
        state.navigateToTab(CareerCompassBottomTab.Applications)

        assertTrue(state.popBack())
        assertEquals(listOf("Feed"), state.shape())

        assertFalse(state.popBack())
        assertEquals(listOf("Feed"), state.shape())
    }

    @Test
    fun `피드 위의 알림 자리표시자는 두 번 쌓이지 않고 탭 전환이 걷어낸다`() {
        val state = stateOn(Route.Feed)

        state.navigateToNotifications()
        state.navigateToNotifications()
        assertEquals(listOf("Feed", "NotificationsPlaceholder"), state.shape())

        state.navigateToTab(CareerCompassBottomTab.My)
        assertEquals(listOf("Feed", "MyTab"), state.shape())
    }

    @Test
    fun `인증을 끝내면 루트가 피드 하나로 수렴해 뒤로가기로 인증 화면에 돌아가지 않는다`() {
        val state = stateOn(Route.Onboarding)

        state.navigateToMain()

        assertEquals(listOf("Feed"), state.shape())
        assertFalse(state.popBack())
    }

    @Test
    fun `온보딩 바닥에서는 루트를 내리지 않는다`() {
        val state = stateOn(Route.Onboarding)

        assertFalse(state.popBack())
        assertEquals(listOf("Onboarding"), state.shape())
    }

    @Test
    fun `바텀바는 피드 홈과 자리표시자 탭에서만 보인다`() {
        val state = stateOn(Route.Feed)

        assertTrue(state.shouldShowBottomBar(Route.Feed, isFeedStackAtRoot = true))
        assertFalse(state.shouldShowBottomBar(Route.Feed, isFeedStackAtRoot = false))
        assertTrue(state.shouldShowBottomBar(Route.MyTab, isFeedStackAtRoot = false))
        assertFalse(state.shouldShowBottomBar(Route.NotificationsPlaceholder, isFeedStackAtRoot = true))
        assertFalse(state.shouldShowBottomBar(Route.Onboarding, isFeedStackAtRoot = true))
        assertFalse(state.shouldShowBottomBar(null, isFeedStackAtRoot = true))
    }

    @Test
    fun `선택 탭은 루트 키를 따르고 탭이 아닌 화면에서는 피드다`() {
        val state = stateOn(Route.Feed)

        assertEquals(CareerCompassBottomTab.My, state.currentTab(Route.MyTab))
        assertEquals(CareerCompassBottomTab.Analysis, state.currentTab(Route.AnalysisTab))
        assertEquals(CareerCompassBottomTab.Feed, state.currentTab(Route.Feed))
        assertEquals(CareerCompassBottomTab.Feed, state.currentTab(Route.NotificationsPlaceholder))
    }
}
