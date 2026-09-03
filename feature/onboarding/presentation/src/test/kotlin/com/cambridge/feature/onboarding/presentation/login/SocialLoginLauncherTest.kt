package com.cambridge.feature.onboarding.presentation.login

import android.app.Activity
import androidx.activity.compose.LocalActivity
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.junit4.v2.createComposeRule
import com.cambridge.core.domain.testing.FakeAuthRepository
import com.cambridge.core.domain.usecase.auth.SocialLoginUseCase
import com.cambridge.core.model.auth.SocialProvider
import com.cambridge.feature.onboarding.presentation.reporting.RecordingErrorReporter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * 재생성 경로 회귀 가드(#147).
 *
 * 컴포지션을 실제로 죽여서 확인한다 — 설정 변경(다크 모드·글꼴 크기·분할 화면 진입)으로 액티비티가 재생성되면
 * 로그인 화면의 컴포지션도 함께 죽는다. `rememberCoroutineScope()` 로 SDK 를 띄우던 예전 배선에서는 그 순간
 * 코루틴만 조용히 취소되고 진행 표시를 되돌릴 사람이 없어 로그인 버튼이 영영 잠겼다.
 *
 * Robolectric 의 화면 재구성은 ViewModel 을 살려 두는데, 이 버그가 나던 상황이 정확히 그 모양이다 — ViewModel 은
 * 그대로 있고 컴포지션만 죽는다. 그래서 여기서도 ViewModel 은 두고 컴포지션만 없앤다.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class SocialLoginLauncherTest {
    @get:Rule
    val composeRule = createComposeRule()

    private val authRepository = FakeAuthRepository()
    private val reporter = RecordingErrorReporter()

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `SDK 가 떠 있는 동안 컴포지션이 죽으면 잠금이 풀린다`() {
        val viewModel = LoginViewModel(SocialLoginUseCase(authRepository), reporter)
        var attached by mutableStateOf(true)
        composeRule.setContent {
            if (attached) {
                val launchLogin =
                    rememberSocialLoginLauncher(
                        onAttempt = viewModel::onSocialLoginRequested,
                        onHostDetached = viewModel::onLoginHostDetached,
                        // SDK 화면이 떠 있어 아직 아무 답도 오지 않은 상태.
                        tokenSource = { _, _ -> awaitCancellation() },
                    )
                LaunchedEffect(Unit) { launchLogin(SocialProvider.Kakao) }
            }
        }
        composeRule.waitForIdle()
        assertTrue(viewModel.uiState.value.isLoading)

        attached = false
        composeRule.waitForIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        // 사용자가 실패한 것이 아니라 화면이 다시 그려진 것뿐이다 — 안내도 기록도 내지 않는다.
        assertNull(state.failure)
        assertTrue(reporter.failures.isEmpty())
        assertTrue(authRepository.socialLoginCalls.isEmpty())
    }

    @Test
    fun `카카오와 구글 모두 호스트 Activity 를 실어 같은 발사대로 나간다`() {
        val sdkCalls = mutableListOf<Pair<Activity?, SocialProvider>>()
        val captured = mutableMapOf<SocialProvider, suspend () -> Result<String>>()
        var host: Activity? = null

        composeRule.setContent {
            host = LocalActivity.current
            val launchLogin =
                rememberSocialLoginLauncher(
                    onAttempt = { provider, requestToken -> captured[provider] = requestToken },
                    onHostDetached = {},
                    tokenSource = { activity, provider ->
                        sdkCalls += activity to provider
                        Result.success("$provider-token")
                    },
                )
            LaunchedEffect(Unit) { SocialProvider.entries.forEach(launchLogin) }
        }
        composeRule.waitForIdle()

        assertNotNull(host)
        val tokens = SocialProvider.entries.map { provider -> runBlocking { captured.getValue(provider)() }.getOrNull() }
        assertEquals(SocialProvider.entries.map { "$it-token" }, tokens)
        assertEquals(SocialProvider.entries.map { host to it }, sdkCalls)
    }

    @Test
    fun `Activity 호스트가 없으면 SDK 를 부르지 않고 설정 오류로 끝낸다`() {
        var sdkCalls = 0
        var requestToken: (suspend () -> Result<String>)? = null

        composeRule.setContent {
            CompositionLocalProvider(LocalActivity provides null) {
                val launchLogin =
                    rememberSocialLoginLauncher(
                        onAttempt = { _, request -> requestToken = request },
                        onHostDetached = {},
                        tokenSource = { _, _ ->
                            sdkCalls++
                            Result.success("must-not-happen")
                        },
                    )
                LaunchedEffect(Unit) { launchLogin(SocialProvider.Kakao) }
            }
        }
        composeRule.waitForIdle()

        val failure = runBlocking { requireNotNull(requestToken)() }.exceptionOrNull()

        assertEquals(0, sdkCalls)
        assertTrue(failure is IllegalStateException)
    }
}
