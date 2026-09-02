package com.cambridge.feature.onboarding.presentation.biometric

import com.cambridge.core.domain.testing.FakeAuthRepository
import com.cambridge.core.domain.testing.FakeUserProfileRepository
import com.cambridge.core.model.user.UserProfile
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

@OptIn(ExperimentalCoroutinesApi::class)
class BiometricLoginViewModelTest {
    private val authRepository = FakeAuthRepository(loggedIn = true, biometricEnabled = true)
    private val userProfileRepository = FakeUserProfileRepository(initialProfile = profile(name = "정일혁"))
    private val reporter = RecordingErrorReporter()

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun createViewModel() = BiometricLoginViewModel(authRepository, userProfileRepository, reporter)

    @Test
    fun `지문 활성 여부와 사용자 이름을 상태로 흘린다`() {
        val viewModel = createViewModel()

        assertTrue(viewModel.uiState.value.isBiometricEnabled)
        assertEquals("정일혁", viewModel.uiState.value.userName)

        authRepository.biometricEnabledState.value = false
        userProfileRepository.profileState.value = profile(name = null)

        assertFalse(viewModel.uiState.value.isBiometricEnabled)
        assertNull(viewModel.uiState.value.userName)
    }

    @Test
    fun `인증 성공은 서버 호출 없이 피드로 보낸다`() {
        val viewModel = createViewModel()
        viewModel.onAuthenticationStarted()
        assertTrue(viewModel.uiState.value.isAuthenticating)

        viewModel.onAuthenticationSucceeded()

        val state = viewModel.uiState.value
        assertEquals(BiometricDestination.Feed, state.pendingNavigation)
        assertFalse(state.isAuthenticating)
        assertEquals(0, authRepository.rotateTokenCalls)
        assertTrue(authRepository.socialLoginCalls.isEmpty())
    }

    @Test
    fun `사용자 취소는 표시도 기록도 하지 않는다`() {
        val viewModel = createViewModel()
        viewModel.onAuthenticationStarted()

        viewModel.onAuthenticationCancelled()

        val state = viewModel.uiState.value
        assertFalse(state.isAuthenticating)
        assertNull(state.failure)
        assertTrue(reporter.failures.isEmpty())
    }

    @Test
    fun `인증 실패는 사유를 표시하고 기록한다`() {
        val viewModel = createViewModel()
        viewModel.onAuthenticationStarted()

        viewModel.onAuthenticationFailed(BiometricFailureReason.Lockout, IllegalStateException("lockout"))

        val state = viewModel.uiState.value
        assertEquals(BiometricFailureReason.Lockout, state.failure)
        assertFalse(state.isAuthenticating)
        assertEquals(listOf("biometric_auth"), reporter.stages())

        viewModel.onFailureConsumed()
        assertNull(viewModel.uiState.value.failure)
    }

    @Test
    fun `다른 방법으로 로그인은 로그인 화면으로 보낸다`() {
        val viewModel = createViewModel()

        viewModel.onOtherMethodClicked()
        assertEquals(BiometricDestination.Login, viewModel.uiState.value.pendingNavigation)

        viewModel.onNavigationConsumed()
        assertNull(viewModel.uiState.value.pendingNavigation)
    }

    private fun profile(name: String?) =
        UserProfile(
            id = 1L,
            name = name,
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
