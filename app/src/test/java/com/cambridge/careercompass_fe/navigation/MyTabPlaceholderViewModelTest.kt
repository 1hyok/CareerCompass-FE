package com.cambridge.careercompass_fe.navigation

import com.cambridge.core.common.reporting.ErrorReporter
import com.cambridge.core.domain.testing.FakeAuthRepository
import com.cambridge.core.domain.testing.FakeUserProfileRepository
import com.cambridge.core.domain.usecase.auth.LogoutUseCase
import com.cambridge.core.model.user.UserProfile
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class MyTabPlaceholderViewModelTest {
    private class RecordingReporter : ErrorReporter {
        val recorded = mutableListOf<Map<String, String>>()

        override fun writeFailure(
            throwable: Throwable,
            attributes: Map<String, String>,
        ) {
            recorded += attributes
        }
    }

    private val reporter = RecordingReporter()
    private val authRepository = FakeAuthRepository(loggedIn = true)
    private val userProfileRepository = FakeUserProfileRepository()

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun viewModel() =
        MyTabPlaceholderViewModel(
            userProfileRepository = userProfileRepository,
            logout = LogoutUseCase(authRepository),
            errorReporter = reporter,
        )

    private fun profile(
        school: String? = "건국대학교",
        department: String? = "컴퓨터공학부",
    ) = UserProfile(
        id = 1,
        name = "정일혁",
        school = school,
        department = department,
        gpa = null,
        gradYear = null,
        jobInterests = emptyList(),
        tags = emptyList(),
        onboardingDone = true,
        completion = 40,
    )

    @Test
    fun `프로필 캐시의 이름과 소속을 그대로 흘린다`() {
        userProfileRepository.profileState.value = profile()

        val state = viewModel().state.value

        assertEquals("정일혁", state.name)
        assertEquals("건국대학교 · 컴퓨터공학부", state.affiliation)
    }

    @Test
    fun `학과만 아는 프로필은 아는 것만 잇는다`() {
        userProfileRepository.profileState.value = profile(school = null)

        assertEquals("컴퓨터공학부", viewModel().state.value.affiliation)
    }

    @Test
    fun `프로필을 모르면 이름과 소속이 비어 화면이 대체 문구를 쓴다`() {
        val state = viewModel().state.value

        assertNull(state.name)
        assertNull(state.affiliation)
    }

    @Test
    fun `확인하면 로그아웃하고 세션 종료를 알린다`() {
        val viewModel = viewModel()

        viewModel.onEvent(MyTabPlaceholderEvent.LogoutClicked)
        assertTrue(viewModel.state.value.isLogoutDialogVisible)

        viewModel.onEvent(MyTabPlaceholderEvent.LogoutConfirmed)

        assertEquals(1, authRepository.logoutCalls)
        assertFalse(authRepository.loggedIn)
        assertFalse(viewModel.state.value.isLogoutDialogVisible)
        assertFalse(viewModel.state.value.isLoggingOut)
        assertTrue(viewModel.state.value.sessionEnded)
    }

    @Test
    fun `취소하면 다이얼로그만 닫고 로그아웃하지 않는다`() {
        val viewModel = viewModel()

        viewModel.onEvent(MyTabPlaceholderEvent.LogoutClicked)
        viewModel.onEvent(MyTabPlaceholderEvent.LogoutDismissed)

        assertFalse(viewModel.state.value.isLogoutDialogVisible)
        assertEquals(0, authRepository.logoutCalls)
        assertFalse(viewModel.state.value.sessionEnded)
    }

    /** 서버 로그아웃이 실패해도 로컬 세션은 정리된 뒤다 — 사용자를 로그인된 화면에 붙잡아 두지 않는다. */
    @Test
    fun `로그아웃이 실패해도 세션 종료를 알리고 실패를 기록한다`() {
        authRepository.onLogout = { Result.failure(IllegalStateException("세션 정리 실패")) }
        val viewModel = viewModel()

        viewModel.onEvent(MyTabPlaceholderEvent.LogoutConfirmed)

        assertTrue(viewModel.state.value.sessionEnded)
        assertEquals("logout", reporter.recorded.single()["app_stage"])
    }

    @Test
    fun `로그아웃 중 다시 확인해도 요청은 한 번이다`() {
        val serverLogout = CompletableDeferred<Unit>()
        authRepository.onLogout = {
            serverLogout.await()
            Result.success(Unit)
        }
        val viewModel = viewModel()

        viewModel.onEvent(MyTabPlaceholderEvent.LogoutConfirmed)
        assertTrue(viewModel.state.value.isLoggingOut)
        viewModel.onEvent(MyTabPlaceholderEvent.LogoutConfirmed)
        serverLogout.complete(Unit)

        assertEquals(1, authRepository.logoutCalls)
        assertTrue(viewModel.state.value.sessionEnded)
    }

    @Test
    fun `세션 종료를 소비하면 알림이 되풀이되지 않는다`() {
        val viewModel = viewModel()
        viewModel.onEvent(MyTabPlaceholderEvent.LogoutConfirmed)

        viewModel.onSessionEndedConsumed()

        assertFalse(viewModel.state.value.sessionEnded)
    }
}
