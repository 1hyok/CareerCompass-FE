package com.cambridge.feature.onboarding.presentation.biometric

import android.os.Bundle
import androidx.activity.compose.LocalActivity
import androidx.biometric.BiometricPrompt
import androidx.biometric.biometricClientCallbackOf
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.performClick
import androidx.fragment.app.FragmentActivity
import com.cambridge.core.ui.theme.CareerCompassTheme
import com.cambridge.feature.onboarding.presentation.reporting.RecordingErrorReporter
import com.careercompass.core.domain.testing.FakeAuthRepository
import com.careercompass.core.domain.testing.FakeUserProfileRepository
import com.careercompass.core.domain.usecase.auth.ResolveSessionEntryUseCase
import com.careercompass.core.model.user.UserProfile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * 지문 프롬프트와 화면 재생성이 만나는 자리의 회귀 가드(#157).
 *
 * 소셜 로그인은 컴포지션이 죽으면 진행 중이던 코루틴만 조용히 취소돼 잠금이 남았다(#147). 지문은 구조가 다르다 —
 * 결과를 코루틴이 아니라 `BiometricPrompt` 콜백으로 받고, 그 콜백은 **액티비티 수명의 `BiometricViewModel`** 이
 * 붙들고 있어 컴포지션보다 오래 산다. 그래서 여기서 확인해야 할 것은 두 가지로 갈린다.
 *
 * - **프롬프트가 떠 있는 동안의 재생성**: 늦게 온 결과가 살아남은 ViewModel 로 가는가. (간다 — 결함 없음)
 * - **프롬프트를 띄우지도 못하는 창**: 상태가 저장된 뒤에는 `authenticate()` 가 콜백 없이 돌아간다. 이때 진행
 *   표시를 먼저 켜 두면 되돌릴 사람이 없다. (그래서 시작을 알리기 전에 막는다)
 *
 * 실제 센서를 띄울 수 없으므로 결과는 라이브러리에 등록된 클라이언트 콜백을 직접 불러 흉내 낸다
 * ([biometricClientCallbackOf]) — 콜백이 「어느 수명에 매여 있는가」가 이 테스트의 물음이라 그 지점을 직접 봐야 한다.
 * 컴포지션은 실제로 죽이고 ViewModel 은 살려 둔다(설정 변경 재생성의 모양, `SocialLoginLauncherTest` 와 같은 방식).
 */
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], qualifiers = "w360dp-h800dp")
class BiometricPromptLauncherRecreateTest {
    @get:Rule
    val composeRule = createComposeRule()

    private val authRepository =
        FakeAuthRepository(loggedIn = true, biometricEnabled = true, accessToken = "access", refreshToken = "refresh")
    private val userProfileRepository = FakeUserProfileRepository(initialProfile = profile())
    private val reporter = RecordingErrorReporter()

    /** 지문 프롬프트를 요구하는 [FragmentActivity] 호스트. 컴포즈 규칙의 호스트와 따로 두어 수명을 직접 굴린다. */
    private val hostController = Robolectric.buildActivity(FragmentActivity::class.java).setup()
    private val host: FragmentActivity = hostController.get()

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `프롬프트가 떠 있는 동안 컴포지션이 죽어도 결과는 살아남은 ViewModel 로 간다`() {
        val viewModel = createViewModel()
        var attached by mutableStateOf(true)
        setContent(viewModel) { attached }

        biometricButton().performClick()
        composeRule.waitForIdle()
        assertTrue(viewModel.uiState.value.isAuthenticating)

        // 설정 변경 재생성 — 프롬프트는 액티비티에 붙어 그대로 떠 있고, 화면 컴포지션만 사라진다.
        val callback = biometricClientCallbackOf(host)
        attached = false
        composeRule.waitForIdle()
        assertTrue(viewModel.uiState.value.isAuthenticating)

        cancel(callback)

        val state = viewModel.uiState.value
        assertFalse(state.isAuthenticating)
        // 사용자가 닫은 것이라 안내도 기록도 없다.
        assertNull(state.failure)
        assertNull(state.pendingNavigation)
        assertTrue(reporter.failures.isEmpty())
    }

    @Test
    fun `재생성된 화면이 붙은 뒤에 온 결과도 같은 ViewModel 로 간다`() {
        val viewModel = createViewModel()
        var attached by mutableStateOf(true)
        setContent(viewModel) { attached }

        biometricButton().performClick()
        composeRule.waitForIdle()

        attached = false
        composeRule.waitForIdle()
        attached = true
        composeRule.waitForIdle()

        // 새 컴포지션이 만든 `BiometricPrompt` 가 콜백을 갈아 끼운 뒤에도 결과는 같은 ViewModel 로 온다.
        cancel(biometricClientCallbackOf(host))

        val state = viewModel.uiState.value
        assertFalse(state.isAuthenticating)
        assertNull(state.failure)
        assertTrue(reporter.failures.isEmpty())
    }

    @Test
    fun `상태가 저장된 뒤의 프롬프트 요청은 진행 표시를 남기지 않는다`() {
        val viewModel = createViewModel()
        setContent(viewModel) { true }

        // 재생성이 시작돼 호스트가 상태를 저장한 창 — 이때 authenticate() 는 콜백 없이 돌아간다.
        hostController.pause().saveInstanceState(Bundle())
        assertTrue(host.supportFragmentManager.isStateSaved)

        biometricButton().performClick()
        composeRule.waitForIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isAuthenticating)
        // 사용자가 실패한 것이 아니라 화면이 재생성되는 것뿐이다 — 안내도 기록도 내지 않는다(#147 과 같은 규칙).
        assertNull(state.failure)
        assertTrue(reporter.failures.isEmpty())
    }

    private fun setContent(
        viewModel: BiometricLoginViewModel,
        attached: @Composable () -> Boolean,
    ) {
        composeRule.setContent {
            if (attached()) {
                CompositionLocalProvider(LocalActivity provides host) {
                    CareerCompassTheme {
                        BiometricLoginEntry(
                            onLoginSuccess = {},
                            onOnboardingRequired = {},
                            onOtherMethodLogin = {},
                            onSessionExpired = {},
                            viewModel = viewModel,
                        )
                    }
                }
            }
        }
        composeRule.waitForIdle()
    }

    private fun cancel(callback: BiometricPrompt.AuthenticationCallback) {
        callback.onAuthenticationError(BiometricPrompt.ERROR_USER_CANCELED, "사용자가 닫았다")
        composeRule.waitForIdle()
    }

    private fun biometricButton() = composeRule.onNodeWithContentDescription("지문으로 로그인")

    private fun createViewModel() =
        BiometricLoginViewModel(
            authRepository,
            userProfileRepository,
            ResolveSessionEntryUseCase(authRepository, userProfileRepository),
            reporter,
        )

    private fun profile() =
        UserProfile(
            id = 1L,
            name = "일혁",
            school = null,
            department = null,
            gpa = null,
            gradYear = null,
            jobInterests = emptyList(),
            tags = emptyList(),
            onboardingDone = true,
            completion = 10,
        )
}
