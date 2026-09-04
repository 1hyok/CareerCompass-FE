package com.cambridge.careercompass_fe.navigation

import com.cambridge.core.common.reporting.ErrorReporter
import com.cambridge.core.domain.testing.FakeAppSettingsRepository
import com.cambridge.core.domain.testing.FakeAuthRepository
import com.cambridge.core.domain.testing.FakeUserProfileRepository
import com.cambridge.core.domain.usecase.auth.LogoutUseCase
import com.cambridge.core.model.settings.ThemeMode
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
    private val appSettingsRepository = FakeAppSettingsRepository()

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
            authRepository = authRepository,
            logout = LogoutUseCase(authRepository),
            errorReporter = reporter,
            appSettingsRepository = appSettingsRepository,
        )

    /** 기기가 지문을 등록할 수 있다고 화면이 알려 준 상태 — 실제 배선에서 스위치를 켤 수 있는 유일한 조건이다. */
    private fun viewModelOnEnrollableDevice() = viewModel().apply { onBiometricAvailabilityChanged(canEnroll = true) }

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

    // ── 지문 로그인 스위치 (#113) ──────────────────────────────────────────────

    @Test
    fun `스위치는 저장소의 등록 상태를 그대로 따른다`() {
        authRepository.biometricEnabledState.value = true

        assertTrue(viewModel().state.value.isBiometricEnabled)
    }

    /** 이 스위치가 있는 이유 — 끄는 경로가 여기 말고는 없다. */
    @Test
    fun `끄면 기기의 등록 기록을 지운다`() {
        authRepository.biometricEnabledState.value = true
        val viewModel = viewModelOnEnrollableDevice()

        viewModel.onEvent(MyTabPlaceholderEvent.BiometricToggled(enabled = false))

        assertEquals(1, authRepository.setBiometricEnabledCalls)
        assertFalse(authRepository.biometricEnabledState.value)
        assertFalse(viewModel.state.value.isBiometricEnabled)
        assertFalse(viewModel.state.value.isBiometricBusy)
    }

    @Test
    fun `켜면 등록 프롬프트를 요청하고 결과가 올 때까지 스위치를 잠근다`() {
        val viewModel = viewModelOnEnrollableDevice()

        viewModel.onEvent(MyTabPlaceholderEvent.BiometricToggled(enabled = true))

        assertTrue(viewModel.state.value.isEnrollPromptRequested)
        assertFalse(viewModel.state.value.isBiometricSwitchEnabled)
        // 지문을 확인하기 전에는 서버를 부르지 않는다 — 등록 흐름은 #98 과 같다.
        assertEquals(0, authRepository.registerBiometricCalls)
    }

    @Test
    fun `프롬프트를 띄우면 요청 신호가 내려간다`() {
        val viewModel = viewModelOnEnrollableDevice()
        viewModel.onEvent(MyTabPlaceholderEvent.BiometricToggled(enabled = true))

        viewModel.onEnrollPromptRequestConsumed()

        assertFalse(viewModel.state.value.isEnrollPromptRequested)
    }

    @Test
    fun `지문을 확인하면 서버에 등록해 켠다`() {
        val viewModel = viewModelOnEnrollableDevice()
        viewModel.onEvent(MyTabPlaceholderEvent.BiometricToggled(enabled = true))
        viewModel.onEnrollPromptRequestConsumed()

        viewModel.onBiometricEnrollSucceeded()

        assertEquals(1, authRepository.registerBiometricCalls)
        assertTrue(viewModel.state.value.isBiometricEnabled)
        assertTrue(viewModel.state.value.isBiometricSwitchEnabled)
    }

    @Test
    fun `프롬프트를 취소하면 등록하지 않고 스위치가 꺼진 자리로 돌아온다`() {
        val viewModel = viewModelOnEnrollableDevice()
        viewModel.onEvent(MyTabPlaceholderEvent.BiometricToggled(enabled = true))

        viewModel.onBiometricEnrollCancelled()

        assertEquals(0, authRepository.registerBiometricCalls)
        assertFalse(viewModel.state.value.isBiometricEnabled)
        assertTrue(viewModel.state.value.isBiometricSwitchEnabled)
        assertTrue(reporter.recorded.isEmpty())
    }

    @Test
    fun `지문 확인이 실패하면 스위치는 꺼진 자리에 남고 사유는 기록한다`() {
        val viewModel = viewModelOnEnrollableDevice()
        viewModel.onEvent(MyTabPlaceholderEvent.BiometricToggled(enabled = true))

        viewModel.onBiometricEnrollFailed(IllegalStateException("지문 확인 실패"))

        assertEquals(0, authRepository.registerBiometricCalls)
        assertFalse(viewModel.state.value.isBiometricEnabled)
        assertTrue(viewModel.state.value.isBiometricSwitchEnabled)
        assertEquals("biometric_toggle", reporter.recorded.single()["app_stage"])
    }

    @Test
    fun `서버 등록이 실패하면 스위치는 꺼진 자리에 남고 사유는 기록한다`() {
        authRepository.onRegisterBiometric = { Result.failure(IllegalStateException("등록 실패")) }
        val viewModel = viewModelOnEnrollableDevice()
        viewModel.onEvent(MyTabPlaceholderEvent.BiometricToggled(enabled = true))

        viewModel.onBiometricEnrollSucceeded()

        assertFalse(viewModel.state.value.isBiometricEnabled)
        assertTrue(viewModel.state.value.isBiometricSwitchEnabled)
        assertEquals("biometric_toggle", reporter.recorded.single()["app_stage"])
    }

    /** 「나중에」는 제안을 다시 하지 말라는 답일 뿐이다 — 직접 찾아온 이 경로까지 막지 않는다. */
    @Test
    fun `나중에로 넘긴 기록이 있어도 스위치로 켤 수 있다`() {
        authRepository.biometricEnrollDeclinedState.value = true
        val viewModel = viewModelOnEnrollableDevice()

        viewModel.onEvent(MyTabPlaceholderEvent.BiometricToggled(enabled = true))
        viewModel.onBiometricEnrollSucceeded()

        assertEquals(1, authRepository.registerBiometricCalls)
        assertTrue(viewModel.state.value.isBiometricEnabled)
        assertTrue(authRepository.biometricEnrollDeclinedState.value)
    }

    @Test
    fun `기기가 지문을 등록할 수 없으면 스위치는 꺼진 채 잠기고 이유를 안내한다`() {
        val viewModel = viewModel()

        viewModel.onBiometricAvailabilityChanged(canEnroll = false)

        assertFalse(viewModel.state.value.isBiometricEnabled)
        assertFalse(viewModel.state.value.isBiometricSwitchEnabled)
        assertTrue(viewModel.state.value.isBiometricUnavailableNoticeVisible)
    }

    /** 판정 전 한 프레임에 「쓸 수 없다」가 스쳤다 사라지지 않게 — 화면이 알려 주기 전에는 안내도 없다. */
    @Test
    fun `기기 판정 전에는 스위치가 잠기고 안내도 없다`() {
        val state = viewModel().state.value

        assertFalse(state.isBiometricSwitchEnabled)
        assertFalse(state.isBiometricUnavailableNoticeVisible)
    }

    /** 켜 둔 뒤 지문을 지운 기기 — 여기서도 잠그면 시작 목적지가 지문 화면인 채로 사용자가 갇힌다. */
    @Test
    fun `켜져 있으면 등록할 수 없는 기기에서도 끌 수 있다`() {
        authRepository.biometricEnabledState.value = true
        val viewModel = viewModel()

        viewModel.onBiometricAvailabilityChanged(canEnroll = false)

        assertTrue(viewModel.state.value.isBiometricSwitchEnabled)
        assertFalse(viewModel.state.value.isBiometricUnavailableNoticeVisible)

        viewModel.onEvent(MyTabPlaceholderEvent.BiometricToggled(enabled = false))

        assertFalse(authRepository.biometricEnabledState.value)
    }

    @Test
    fun `등록을 기다리는 동안 눌린 해제는 무시한다`() {
        val serverRegister = CompletableDeferred<Unit>()
        authRepository.onRegisterBiometric = {
            serverRegister.await()
            Result.success(Unit)
        }
        val viewModel = viewModelOnEnrollableDevice()
        viewModel.onEvent(MyTabPlaceholderEvent.BiometricToggled(enabled = true))
        viewModel.onBiometricEnrollSucceeded()

        viewModel.onEvent(MyTabPlaceholderEvent.BiometricToggled(enabled = false))
        serverRegister.complete(Unit)

        assertEquals(0, authRepository.setBiometricEnabledCalls)
        assertFalse(viewModel.state.value.isBiometricBusy)
    }

    @Test
    fun `저장된 테마를 그대로 되비춘다`() {
        appSettingsRepository.themeModeState.value = ThemeMode.Dark

        assertEquals(ThemeMode.Dark, viewModel().state.value.themeMode)
    }

    @Test
    fun `테마를 고르면 저장하고 다이얼로그를 닫는다`() {
        val viewModel = viewModel()
        viewModel.onEvent(MyTabPlaceholderEvent.ThemeClicked)
        assertTrue(viewModel.state.value.isThemeDialogVisible)

        viewModel.onEvent(MyTabPlaceholderEvent.ThemeSelected(ThemeMode.Light))

        assertFalse(viewModel.state.value.isThemeDialogVisible)
        assertEquals(ThemeMode.Light, appSettingsRepository.themeModeState.value)
        assertEquals(ThemeMode.Light, viewModel.state.value.themeMode)
    }

    @Test
    fun `테마 다이얼로그를 닫기만 하면 값이 그대로다`() {
        appSettingsRepository.themeModeState.value = ThemeMode.Dark
        val viewModel = viewModel()

        viewModel.onEvent(MyTabPlaceholderEvent.ThemeClicked)
        viewModel.onEvent(MyTabPlaceholderEvent.ThemeDismissed)

        assertFalse(viewModel.state.value.isThemeDialogVisible)
        assertEquals(ThemeMode.Dark, appSettingsRepository.themeModeState.value)
    }

    @Test
    fun `저장이 실패해도 화면은 저장소 값을 따른다`() {
        // 화면이 낙관적으로 갱신하지 않으므로, 실패하면 아무것도 되돌리지 않아도 원래 값이 남는다.
        val failing =
            object : com.cambridge.core.domain.settings.AppSettingsRepository {
                override val themeMode = appSettingsRepository.themeMode

                override suspend fun setThemeMode(mode: ThemeMode) = throw IllegalStateException("저장 실패")
            }
        val viewModel =
            MyTabPlaceholderViewModel(
                userProfileRepository = userProfileRepository,
                authRepository = authRepository,
                logout = LogoutUseCase(authRepository),
                errorReporter = reporter,
                appSettingsRepository = failing,
            )

        viewModel.onEvent(MyTabPlaceholderEvent.ThemeSelected(ThemeMode.Dark))

        assertEquals(ThemeMode.System, viewModel.state.value.themeMode)
        assertTrue(reporter.recorded.any { it.values.contains("theme_mode") })
    }
}
