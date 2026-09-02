package com.cambridge.feature.onboarding.presentation.login

import com.cambridge.core.domain.error.CoreAuthFailure
import com.cambridge.core.domain.testing.FakeAuthRepository
import com.cambridge.core.domain.usecase.auth.SocialLoginUseCase
import com.cambridge.core.model.auth.Session
import com.cambridge.core.model.auth.SocialProvider
import com.cambridge.feature.onboarding.presentation.reporting.ONBOARDING_REPORT_KEY_PROVIDER
import com.cambridge.feature.onboarding.presentation.reporting.RecordingErrorReporter
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
import java.io.IOException

@OptIn(ExperimentalCoroutinesApi::class)
class LoginViewModelTest {
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

    private fun createViewModel() = LoginViewModel(SocialLoginUseCase(authRepository), reporter)

    @Test
    fun `신규 사용자는 온보딩으로 보낸다`() {
        authRepository.session = session(isNewUser = true)
        val viewModel = createViewModel()

        viewModel.loginWithKakao("kakao-token")

        val state = viewModel.uiState.value
        assertEquals(LoginDestination.Onboarding, state.pendingNavigation)
        assertFalse(state.isLoading)
        assertNull(state.failure)
        assertEquals(SocialProvider.Kakao, authRepository.socialLoginCalls.single().provider)
        assertEquals("kakao-token", authRepository.socialLoginCalls.single().providerToken)
        assertEquals(1, authRepository.savedSessions.size)
    }

    @Test
    fun `기존 사용자는 피드로 보낸다`() {
        authRepository.session = session(isNewUser = false)
        val viewModel = createViewModel()

        viewModel.loginWithGoogle("google-id-token")

        assertEquals(LoginDestination.Feed, viewModel.uiState.value.pendingNavigation)
        assertEquals(SocialProvider.Google, authRepository.socialLoginCalls.single().provider)
    }

    @Test
    fun `서버 거절은 Rejected 로 표시하고 제공자와 함께 기록한다`() {
        authRepository.onSocialLogin = { _, _, _ -> Result.failure(CoreAuthFailure.SocialLoginRejected(IOException("401"))) }
        val viewModel = createViewModel()

        viewModel.loginWithKakao("kakao-token")

        val state = viewModel.uiState.value
        assertEquals(LoginFailureReason.Rejected, state.failure)
        assertFalse(state.isLoading)
        assertNull(state.pendingNavigation)
        assertEquals(listOf("social_login"), reporter.stages())
        assertEquals("kakao", reporter.failures.single().attributes[ONBOARDING_REPORT_KEY_PROVIDER])
    }

    @Test
    fun `네트워크 실패는 Network 로 표시한다`() {
        authRepository.onSocialLogin = { _, _, _ -> Result.failure(CoreAuthFailure.NetworkUnavailable(IOException("offline"))) }
        val viewModel = createViewModel()

        viewModel.loginWithGoogle("google-id-token")

        assertEquals(LoginFailureReason.Network, viewModel.uiState.value.failure)
    }

    @Test
    fun `알 수 없는 실패는 Unknown 으로 표시한다`() {
        authRepository.onSocialLogin = { _, _, _ -> Result.failure(IllegalStateException("boom")) }
        val viewModel = createViewModel()

        viewModel.loginWithKakao("kakao-token")

        assertEquals(LoginFailureReason.Unknown, viewModel.uiState.value.failure)
    }

    @Test
    fun `SDK 단계의 사용자 취소는 표시도 기록도 하지 않는다`() {
        val viewModel = createViewModel()
        viewModel.onSocialTokenRequestStarted()
        assertTrue(viewModel.uiState.value.isLoading)

        viewModel.onSocialTokenRequestFailed(SocialProvider.Kakao, CoreAuthFailure.UserCancelledAuth())

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertNull(state.failure)
        assertTrue(reporter.failures.isEmpty())
        assertTrue(authRepository.socialLoginCalls.isEmpty())
    }

    @Test
    fun `SDK 단계의 다른 실패는 기록하고 사유를 표시한다`() {
        val viewModel = createViewModel()

        viewModel.onSocialTokenRequestFailed(SocialProvider.Google, IOException("no network"))

        assertEquals(LoginFailureReason.Network, viewModel.uiState.value.failure)
        assertEquals(listOf("social_token_request"), reporter.stages())
        assertEquals("google", reporter.failures.single().attributes[ONBOARDING_REPORT_KEY_PROVIDER])
    }

    @Test
    fun `빈 토큰은 서버에 보내지 않는다`() {
        val viewModel = createViewModel()

        viewModel.loginWithKakao("  ")

        assertEquals(LoginFailureReason.Unknown, viewModel.uiState.value.failure)
        assertTrue(authRepository.socialLoginCalls.isEmpty())
    }

    @Test
    fun `단발 신호는 소비하면 비워진다`() {
        authRepository.onSocialLogin = { _, _, _ -> Result.failure(IllegalStateException("boom")) }
        val viewModel = createViewModel()
        viewModel.loginWithKakao("kakao-token")

        viewModel.onFailureConsumed()
        assertNull(viewModel.uiState.value.failure)

        authRepository.onSocialLogin = null
        viewModel.loginWithKakao("kakao-token")
        viewModel.onNavigationConsumed()
        assertNull(viewModel.uiState.value.pendingNavigation)
    }

    private fun session(isNewUser: Boolean) = Session("access", "refresh", isNewUser = isNewUser, expiresInSeconds = 3600)
}
