package com.cambridge.careercompass_fe.session

import com.cambridge.core.common.reporting.ErrorReporter
import com.cambridge.core.domain.error.CoreDataFailure
import com.cambridge.core.domain.testing.FakeAuthRepository
import com.cambridge.core.domain.testing.FakeUserProfileRepository
import com.cambridge.core.model.user.UserProfile
import com.cambridge.core.network.model.ApiException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import java.net.UnknownHostException

@OptIn(ExperimentalCoroutinesApi::class)
class MainViewModelTest {
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

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun profile(onboardingDone: Boolean) =
        UserProfile(
            id = 1,
            name = "정일혁",
            school = null,
            department = null,
            gpa = null,
            gradYear = null,
            jobInterests = emptyList(),
            tags = emptyList(),
            onboardingDone = onboardingDone,
            completion = 10,
        )

    @Test
    fun `세션이 없으면 로그인으로 시작한다`() {
        val viewModel = MainViewModel(FakeAuthRepository(loggedIn = false), FakeUserProfileRepository.strict(), reporter)

        assertEquals(AppStartDestination.Login, viewModel.startDestination.value)
    }

    @Test
    fun `세션이 있고 지문 로그인을 켰으면 지문 화면으로 시작한다`() {
        val viewModel =
            MainViewModel(FakeAuthRepository(loggedIn = true, biometricEnabled = true), FakeUserProfileRepository.strict(), reporter)

        assertEquals(AppStartDestination.BiometricLogin, viewModel.startDestination.value)
    }

    @Test
    fun `온보딩을 마치지 않은 세션은 온보딩으로, 마친 세션은 메인으로 간다`() {
        val notDone = MainViewModel(FakeAuthRepository(loggedIn = true), FakeUserProfileRepository(profile(false)), reporter)
        val done = MainViewModel(FakeAuthRepository(loggedIn = true), FakeUserProfileRepository(profile(true)), reporter)

        assertEquals(AppStartDestination.Onboarding, notDone.startDestination.value)
        assertEquals(AppStartDestination.Main, done.startDestination.value)
    }

    @Test
    fun `프로필 조회가 401 이면 로그인으로 보낸다`() {
        val profiles =
            FakeUserProfileRepository().apply {
                onRefreshProfile = {
                    Result.failure(CoreDataFailure.Unauthorized("AUTH_INVALID", ApiException("AUTH_INVALID", null, "만료", status = 401)))
                }
            }

        val viewModel = MainViewModel(FakeAuthRepository(loggedIn = true), profiles, reporter)

        assertEquals(AppStartDestination.Login, viewModel.startDestination.value)
    }

    @Test
    fun `프로필 조회가 네트워크로 실패하면 캐시로 판단하고 캐시도 없으면 메인으로 보낸다`() {
        val cached =
            FakeUserProfileRepository(profile(false)).apply {
                onRefreshProfile = { Result.failure(CoreDataFailure.NetworkUnavailable(UnknownHostException())) }
            }
        val empty =
            FakeUserProfileRepository().apply {
                onRefreshProfile = { Result.failure(CoreDataFailure.NetworkUnavailable(UnknownHostException())) }
            }

        assertEquals(
            AppStartDestination.Onboarding,
            MainViewModel(FakeAuthRepository(loggedIn = true), cached, reporter).startDestination.value,
        )
        assertEquals(AppStartDestination.Main, MainViewModel(FakeAuthRepository(loggedIn = true), empty, reporter).startDestination.value)
        assertEquals(2, reporter.recorded.size)
        assertEquals("start_profile", reporter.recorded.first()["app_stage"])
    }
}
