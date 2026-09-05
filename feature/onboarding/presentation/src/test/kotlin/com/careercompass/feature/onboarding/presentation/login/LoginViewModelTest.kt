package com.careercompass.feature.onboarding.presentation.login

import com.careercompass.core.domain.error.CoreAuthFailure
import com.careercompass.core.domain.testing.FakeAuthRepository
import com.careercompass.core.domain.usecase.auth.SocialLoginUseCase
import com.careercompass.core.model.auth.Session
import com.careercompass.core.model.auth.SocialProvider
import com.careercompass.feature.onboarding.presentation.reporting.ONBOARDING_REPORT_KEY_PROVIDER
import com.careercompass.feature.onboarding.presentation.reporting.RecordingErrorReporter
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.awaitCancellation
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

/**
 * 로그인의 **비동기 갈래** — SDK 토큰 단계와 서버 로그인, 그 사이의 취소·이탈. 순수 전이는 [LoginReducerTest] 가 본다.
 */
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

    private fun LoginViewModel.request(
        provider: SocialProvider,
        requestToken: suspend () -> Result<String>,
    ) = onIntent(LoginIntent.RequestSocialLogin(provider, requestToken))

    /** SDK 가 곧바로 토큰을 돌려주는 시도. 토큰 단계는 이 테스트들의 관심사가 아니다. */
    private fun LoginViewModel.attemptWith(
        provider: SocialProvider,
        token: String,
    ) = request(provider) { Result.success(token) }

    @Test
    fun `신규 사용자는 온보딩으로 보낸다`() {
        authRepository.session = session(isNewUser = true)
        val viewModel = createViewModel()

        viewModel.attemptWith(SocialProvider.Kakao, "kakao-token")

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

        viewModel.attemptWith(SocialProvider.Google, "google-id-token")

        assertEquals(LoginDestination.Feed, viewModel.uiState.value.pendingNavigation)
        assertEquals(SocialProvider.Google, authRepository.socialLoginCalls.single().provider)
    }

    @Test
    fun `서버 거절은 Rejected 로 표시하고 제공자와 함께 기록한다`() {
        authRepository.onSocialLogin = { _, _, _ -> Result.failure(CoreAuthFailure.SocialLoginRejected(IOException("401"))) }
        val viewModel = createViewModel()

        viewModel.attemptWith(SocialProvider.Kakao, "kakao-token")

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

        viewModel.attemptWith(SocialProvider.Google, "google-id-token")

        assertEquals(LoginFailureReason.Network, viewModel.uiState.value.failure)
    }

    @Test
    fun `알 수 없는 실패는 Unknown 으로 표시한다`() {
        authRepository.onSocialLogin = { _, _, _ -> Result.failure(IllegalStateException("boom")) }
        val viewModel = createViewModel()

        viewModel.attemptWith(SocialProvider.Kakao, "kakao-token")

        assertEquals(LoginFailureReason.Unknown, viewModel.uiState.value.failure)
    }

    @Test
    fun `SDK 단계의 사용자 취소는 표시도 기록도 하지 않는다`() {
        val viewModel = createViewModel()

        viewModel.request(SocialProvider.Kakao) { Result.failure(CoreAuthFailure.UserCancelledAuth()) }

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertNull(state.failure)
        assertTrue(reporter.failures.isEmpty())
        assertTrue(authRepository.socialLoginCalls.isEmpty())
    }

    @Test
    fun `SDK 단계의 다른 실패는 기록하고 사유를 표시한다`() {
        val viewModel = createViewModel()

        viewModel.request(SocialProvider.Google) { Result.failure(IOException("no network")) }

        assertEquals(LoginFailureReason.Network, viewModel.uiState.value.failure)
        assertEquals(listOf("social_token_request"), reporter.stages())
        assertEquals("google", reporter.failures.single().attributes[ONBOARDING_REPORT_KEY_PROVIDER])
    }

    @Test
    fun `SDK 진입점이 던진 예외는 앱을 죽이지 않고 실패로 끝난다`() {
        val viewModel = createViewModel()

        viewModel.request(SocialProvider.Kakao) { error("Kakao SDK is not initialized") }

        assertEquals(LoginFailureReason.Unknown, viewModel.uiState.value.failure)
        assertFalse(viewModel.uiState.value.isLoading)
    }

    @Test
    fun `빈 토큰은 서버에 보내지 않는다`() {
        val viewModel = createViewModel()

        viewModel.attemptWith(SocialProvider.Kakao, "  ")

        assertEquals(LoginFailureReason.Unknown, viewModel.uiState.value.failure)
        assertTrue(authRepository.socialLoginCalls.isEmpty())
    }

    @Test
    fun `시도가 도는 동안 들어온 두 번째 요청은 SDK 를 다시 열지 않는다`() {
        val viewModel = createViewModel()
        var sdkCalls = 0

        viewModel.request(SocialProvider.Kakao) {
            sdkCalls++
            awaitCancellation()
        }
        viewModel.request(SocialProvider.Google) {
            sdkCalls++
            awaitCancellation()
        }

        assertEquals(1, sdkCalls)
    }

    @Test
    fun `화면이 사라지면 진행 중인 토큰 요청을 끊고 조용히 잠금을 푼다`() {
        val viewModel = createViewModel()
        viewModel.request(SocialProvider.Kakao) { awaitCancellation() }
        assertTrue(viewModel.uiState.value.isLoading)

        viewModel.onIntent(LoginIntent.DetachLoginHost)

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertNull(state.failure)
        assertTrue(reporter.failures.isEmpty())
        assertTrue(authRepository.socialLoginCalls.isEmpty())
    }

    @Test
    fun `잠금이 풀린 뒤에는 다시 로그인할 수 있다`() {
        authRepository.session = session(isNewUser = false)
        val viewModel = createViewModel()
        viewModel.request(SocialProvider.Kakao) { awaitCancellation() }
        viewModel.onIntent(LoginIntent.DetachLoginHost)

        viewModel.attemptWith(SocialProvider.Kakao, "kakao-token")

        assertEquals(LoginDestination.Feed, viewModel.uiState.value.pendingNavigation)
    }

    @Test
    fun `토큰을 받은 뒤라면 화면이 사라져도 서버 로그인은 끝까지 간다`() {
        authRepository.session = session(isNewUser = false)
        val serverCall = CompletableDeferred<Unit>()
        authRepository.onSocialLogin = { _, _, _ ->
            serverCall.await()
            Result.success(session(isNewUser = false))
        }
        val viewModel = createViewModel()
        viewModel.attemptWith(SocialProvider.Kakao, "kakao-token")
        assertTrue(viewModel.uiState.value.isLoading)

        // 토큰 단계는 이미 끝났다 — 이탈 신호가 서버 로그인까지 끊어 버리면 멀쩡히 끝난 소셜 인증이 버려진다.
        viewModel.onIntent(LoginIntent.DetachLoginHost)
        assertTrue(viewModel.uiState.value.isLoading)
        serverCall.complete(Unit)

        assertEquals(LoginDestination.Feed, viewModel.uiState.value.pendingNavigation)
        assertFalse(viewModel.uiState.value.isLoading)
    }

    @Test
    fun `단발 신호는 소비 Intent 로 비워진다`() {
        authRepository.onSocialLogin = { _, _, _ -> Result.failure(IllegalStateException("boom")) }
        val viewModel = createViewModel()
        viewModel.attemptWith(SocialProvider.Kakao, "kakao-token")

        viewModel.onIntent(LoginIntent.ConsumeFailure)
        assertNull(viewModel.uiState.value.failure)

        authRepository.onSocialLogin = null
        viewModel.attemptWith(SocialProvider.Kakao, "kakao-token")
        viewModel.onIntent(LoginIntent.ConsumeNavigation)
        assertNull(viewModel.uiState.value.pendingNavigation)
    }

    private fun session(isNewUser: Boolean) = Session("access", "refresh", isNewUser = isNewUser, expiresInSeconds = 3600)
}
