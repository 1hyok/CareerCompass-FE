package com.careercompass.feature.onboarding.presentation.biometric

import com.careercompass.core.domain.error.CoreDataFailure
import com.careercompass.core.domain.testing.FakeAuthRepository
import com.careercompass.core.domain.testing.FakeUserProfileRepository
import com.careercompass.core.model.user.UserProfile
import com.careercompass.feature.onboarding.presentation.reporting.RecordingErrorReporter
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
import java.net.UnknownHostException

@OptIn(ExperimentalCoroutinesApi::class)
class BiometricEnrollViewModelTest {
    private val authRepository = FakeAuthRepository(loggedIn = true, accessToken = "access", refreshToken = "refresh")
    private val userProfileRepository = FakeUserProfileRepository(initialProfile = profile())
    private val reporter = RecordingErrorReporter()

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun createViewModel() = BiometricEnrollViewModel(authRepository, userProfileRepository, reporter)

    @Test
    fun `제안 조건을 모두 만족하면 시트를 띄운다`() {
        val viewModel = createViewModel()

        viewModel.onOfferRequested(deviceCanEnroll = true)

        val state = viewModel.uiState.value
        assertTrue(state.isOffered)
        assertFalse(state.canProceed)
        assertTrue(reporter.failures.isEmpty())
    }

    @Test
    fun `기기가 지문을 쓸 수 없으면 묻지 않고 통과시킨다`() {
        val viewModel = createViewModel()

        viewModel.onOfferRequested(deviceCanEnroll = false)

        assertFalse(viewModel.uiState.value.isOffered)
        assertTrue(viewModel.uiState.value.canProceed)

        viewModel.onProceedConsumed()
        assertFalse(viewModel.uiState.value.canProceed)
    }

    @Test
    fun `이미 등록한 계정에는 묻지 않는다`() {
        authRepository.biometricEnabledState.value = true
        val viewModel = createViewModel()

        viewModel.onOfferRequested(deviceCanEnroll = true)

        assertFalse(viewModel.uiState.value.isOffered)
        assertTrue(viewModel.uiState.value.canProceed)
    }

    @Test
    fun `전에 나중에를 고른 계정에는 다시 묻지 않는다`() {
        authRepository.biometricEnrollDeclinedState.value = true
        val viewModel = createViewModel()

        viewModel.onOfferRequested(deviceCanEnroll = true)

        assertFalse(viewModel.uiState.value.isOffered)
        assertTrue(viewModel.uiState.value.canProceed)
    }

    @Test
    fun `프로필이 없으면 한 번 받아 온 뒤 판단한다`() {
        userProfileRepository.profileState.value = null
        var refreshCalls = 0
        userProfileRepository.onRefreshProfile = {
            refreshCalls += 1
            userProfileRepository.profileState.value = profile()
            Result.success(profile())
        }
        val viewModel = createViewModel()

        viewModel.onOfferRequested(deviceCanEnroll = true)

        assertTrue(viewModel.uiState.value.isOffered)
        assertEquals(1, refreshCalls)
    }

    @Test
    fun `프로필을 끝내 받지 못하면 기록만 남기고 통과시킨다`() {
        userProfileRepository.profileState.value = null
        userProfileRepository.onRefreshProfile = {
            Result.failure(CoreDataFailure.NetworkUnavailable(UnknownHostException("offline")))
        }
        val viewModel = createViewModel()

        viewModel.onOfferRequested(deviceCanEnroll = true)

        assertFalse(viewModel.uiState.value.isOffered)
        assertTrue(viewModel.uiState.value.canProceed)
        assertEquals(listOf("biometric_enroll"), reporter.stages())
        assertEquals(0, authRepository.registerBiometricCalls)
    }

    @Test
    fun `판정이 진행 중이면 다시 요청해도 합류한다`() {
        userProfileRepository.profileState.value = null
        val gate = CompletableDeferred<Result<UserProfile>>()
        var refreshCalls = 0
        userProfileRepository.onRefreshProfile = {
            refreshCalls += 1
            gate.await()
        }
        val viewModel = createViewModel()

        viewModel.onOfferRequested(deviceCanEnroll = true)
        viewModel.onOfferRequested(deviceCanEnroll = true)
        gate.complete(Result.success(profile()))

        assertEquals(1, refreshCalls)
    }

    @Test
    fun `지문 확인 성공 뒤 서버 등록까지 성공해야 켜진 것으로 본다`() {
        val viewModel = createViewModel()
        viewModel.onOfferRequested(deviceCanEnroll = true)
        viewModel.onAuthenticationStarted()
        assertTrue(viewModel.uiState.value.isRegistering)

        viewModel.onAuthenticationSucceeded()

        val state = viewModel.uiState.value
        assertEquals(1, authRepository.registerBiometricCalls)
        assertTrue(authRepository.biometricEnabledState.value)
        assertFalse(state.isOffered)
        assertFalse(state.isRegistering)
        assertTrue(state.canProceed)
        assertTrue(reporter.failures.isEmpty())
    }

    @Test
    fun `서버 등록이 실패하면 안내만 남기고 시트를 유지한다`() {
        authRepository.onRegisterBiometric = { Result.failure(CoreDataFailure.NetworkUnavailable(UnknownHostException())) }
        val viewModel = createViewModel()
        viewModel.onOfferRequested(deviceCanEnroll = true)

        viewModel.onAuthenticationSucceeded()

        val state = viewModel.uiState.value
        assertEquals(BiometricEnrollFailureReason.Registration, state.failure)
        assertTrue(state.isOffered)
        assertFalse(state.isRegistering)
        assertFalse(state.canProceed)
        assertFalse(authRepository.biometricEnabledState.value)
        assertEquals(listOf("biometric_enroll"), reporter.stages())

        viewModel.onFailureConsumed()
        assertNull(viewModel.uiState.value.failure)
    }

    @Test
    fun `등록이 진행 중이면 성공이 두 번 와도 서버는 한 번만 부른다`() {
        val gate = CompletableDeferred<Result<Unit>>()
        authRepository.onRegisterBiometric = { gate.await() }
        val viewModel = createViewModel()
        viewModel.onOfferRequested(deviceCanEnroll = true)

        viewModel.onAuthenticationSucceeded()
        viewModel.onAuthenticationSucceeded()
        gate.complete(Result.success(Unit))

        assertEquals(1, authRepository.registerBiometricCalls)
        assertTrue(viewModel.uiState.value.canProceed)
    }

    @Test
    fun `지문 확인 실패는 안내하고 기록하되 흐름을 막지 않는다`() {
        val viewModel = createViewModel()
        viewModel.onOfferRequested(deviceCanEnroll = true)
        viewModel.onAuthenticationStarted()

        viewModel.onAuthenticationFailed(IllegalStateException("lockout"))

        val state = viewModel.uiState.value
        assertEquals(BiometricEnrollFailureReason.Authentication, state.failure)
        assertFalse(state.isRegistering)
        assertTrue(state.isOffered)
        assertEquals(listOf("biometric_enroll"), reporter.stages())
        assertEquals(0, authRepository.registerBiometricCalls)

        // 실패 뒤에도 시트를 닫으면 원래 가던 화면으로 이어진다.
        viewModel.onDeclined()
        assertTrue(viewModel.uiState.value.canProceed)
    }

    @Test
    fun `프롬프트 취소는 시트를 그대로 두고 표시도 기록도 하지 않는다`() {
        val viewModel = createViewModel()
        viewModel.onOfferRequested(deviceCanEnroll = true)
        viewModel.onAuthenticationStarted()

        viewModel.onAuthenticationCancelled()

        val state = viewModel.uiState.value
        assertTrue(state.isOffered)
        assertFalse(state.isRegistering)
        assertNull(state.failure)
        assertFalse(state.canProceed)
        assertTrue(reporter.failures.isEmpty())
    }

    @Test
    fun `나중에는 기기에 기록하고 통과시킨다`() {
        val viewModel = createViewModel()
        viewModel.onOfferRequested(deviceCanEnroll = true)

        viewModel.onDeclined()

        assertEquals(1, authRepository.declineBiometricEnrollCalls)
        assertTrue(authRepository.biometricEnrollDeclinedState.value)
        assertFalse(viewModel.uiState.value.isOffered)
        assertTrue(viewModel.uiState.value.canProceed)
        assertEquals(0, authRepository.registerBiometricCalls)
    }

    @Test
    fun `거절 기록이 실패해도 흐름은 막지 않는다`() {
        authRepository.onDeclineBiometricEnroll = { Result.failure(IllegalStateException("프로필 없음")) }
        val viewModel = createViewModel()
        viewModel.onOfferRequested(deviceCanEnroll = true)

        viewModel.onDeclined()

        assertTrue(viewModel.uiState.value.canProceed)
        assertEquals(listOf("biometric_enroll"), reporter.stages())
    }

    private fun profile() =
        UserProfile(
            id = 1L,
            name = "정일혁",
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
