package com.cambridge.careercompass_fe.session

import com.cambridge.careercompass_fe.navigation.AppDeepLink
import com.cambridge.core.common.reporting.ErrorReporter
import com.cambridge.core.domain.error.CoreDataFailure
import com.cambridge.core.domain.testing.FakeAuthRepository
import com.cambridge.core.domain.testing.FakeUserProfileRepository
import com.cambridge.core.model.user.UserProfile
import com.cambridge.core.network.model.ApiException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
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

    private fun networkFailure() = Result.failure<UserProfile>(CoreDataFailure.NetworkUnavailable(UnknownHostException()))

    private val MainViewModel.destination: AppStartDestination? get() = launch.value?.destination

    @Test
    fun `세션이 없으면 로그인으로 시작한다`() {
        val viewModel = MainViewModel(FakeAuthRepository(loggedIn = false), FakeUserProfileRepository.strict(), reporter)

        assertEquals(AppStartDestination.Login, viewModel.destination)
    }

    @Test
    fun `세션이 있고 지문 로그인을 켰으면 지문 화면으로 시작한다`() {
        val viewModel =
            MainViewModel(FakeAuthRepository(loggedIn = true, biometricEnabled = true), FakeUserProfileRepository.strict(), reporter)

        assertEquals(AppStartDestination.BiometricLogin, viewModel.destination)
    }

    @Test
    fun `온보딩을 마치지 않은 세션은 온보딩으로, 마친 세션은 메인으로 간다`() {
        val notDone = MainViewModel(FakeAuthRepository(loggedIn = true), FakeUserProfileRepository(profile(false)), reporter)
        val done = MainViewModel(FakeAuthRepository(loggedIn = true), FakeUserProfileRepository(profile(true)), reporter)

        assertEquals(AppStartDestination.Onboarding, notDone.destination)
        assertEquals(AppStartDestination.Main, done.destination)
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

        assertEquals(AppStartDestination.Login, viewModel.destination)
    }

    @Test
    fun `프로필 조회가 네트워크로 실패하면 마지막으로 알려진 완료 여부로 판단한다`() {
        val cachedNotDone = FakeUserProfileRepository(profile(false)).apply { onRefreshProfile = { networkFailure() } }
        val hintDone =
            FakeUserProfileRepository().apply {
                onRefreshProfile = { networkFailure() }
                onboardingDoneHint = true
            }

        assertEquals(
            AppStartDestination.Onboarding,
            MainViewModel(FakeAuthRepository(loggedIn = true), cachedNotDone, reporter).destination,
        )
        assertEquals(AppStartDestination.Main, MainViewModel(FakeAuthRepository(loggedIn = true), hintDone, reporter).destination)
        assertEquals(2, reporter.recorded.size)
        assertEquals("start_profile", reporter.recorded.first()["app_stage"])
    }

    @Test
    fun `완료 여부를 전혀 모르면 메인이 아니라 온보딩으로 보낸다`() {
        // 신규 사용자의 첫 프로필 조회가 네트워크로 실패한 경우 — 메인으로 추정하면 온보딩을 영영 건너뛴다.
        val unknown = FakeUserProfileRepository().apply { onRefreshProfile = { networkFailure() } }

        val viewModel = MainViewModel(FakeAuthRepository(loggedIn = true), unknown, reporter)

        assertEquals(AppStartDestination.Onboarding, viewModel.destination)
    }

    @Test
    fun `다시 계산하면 목적지가 같아도 revision 이 올라 NavHost 가 새로 만들어진다`() {
        val viewModel = MainViewModel(FakeAuthRepository(loggedIn = false), FakeUserProfileRepository.strict(), reporter)
        val first = requireNotNull(viewModel.launch.value)

        viewModel.refresh()

        val second = requireNotNull(viewModel.launch.value)
        assertEquals(AppStartDestination.Login, second.destination)
        assertNotEquals(first.revision, second.revision)
        assertTrue(second.revision > first.revision)
    }

    @Test
    fun `계산이 진행 중이면 다시 계산 요청은 합류해 한 번만 계산한다`() {
        val gate = CompletableDeferred<Result<UserProfile>>()
        var refreshCalls = 0
        val profiles =
            FakeUserProfileRepository().apply {
                onRefreshProfile = {
                    refreshCalls += 1
                    gate.await()
                }
            }
        val viewModel = MainViewModel(FakeAuthRepository(loggedIn = true), profiles, reporter)
        assertNull(viewModel.launch.value)

        viewModel.refresh()
        viewModel.refresh()
        gate.complete(Result.success(profile(true)))

        assertEquals(1, refreshCalls)
        assertEquals(AppStartDestination.Main, viewModel.destination)
    }

    @Test
    fun `딥링크는 소비할 때까지 보관하고, 계약에 맞지 않아 null 인 것은 무시한다`() {
        val viewModel = MainViewModel(FakeAuthRepository(loggedIn = false), FakeUserProfileRepository.strict(), reporter)
        assertNull(viewModel.pendingDeepLink.value)

        viewModel.onDeepLink(AppDeepLink.PostingDetail(101))
        viewModel.onDeepLink(null)

        assertEquals(AppDeepLink.PostingDetail(101), viewModel.pendingDeepLink.value)
    }

    @Test
    fun `딥링크를 소비하면 비운다`() {
        val viewModel = MainViewModel(FakeAuthRepository(loggedIn = false), FakeUserProfileRepository.strict(), reporter)
        viewModel.onDeepLink(AppDeepLink.PostingDetail(101))

        viewModel.consumeDeepLink()

        assertNull(viewModel.pendingDeepLink.value)
    }

    @Test
    fun `첫 시작 계산이 끝나기 전에 받은 딥링크는 계산이 끝나도 남는다`() {
        // 알림으로 콜드 스타트 — intent 의 딥링크는 세션·프로필 확인보다 먼저 실린다.
        val gate = CompletableDeferred<Result<UserProfile>>()
        val profiles = FakeUserProfileRepository().apply { onRefreshProfile = { gate.await() } }
        val viewModel = MainViewModel(FakeAuthRepository(loggedIn = true), profiles, reporter)
        viewModel.onDeepLink(AppDeepLink.PostingDetail(101))

        gate.complete(Result.success(profile(true)))

        assertEquals(AppStartDestination.Main, viewModel.destination)
        assertEquals(AppDeepLink.PostingDetail(101), viewModel.pendingDeepLink.value)
    }

    @Test
    fun `세션 종료로 다시 계산하면 소비되지 않은 딥링크를 버린다`() {
        // 로그아웃 뒤 다른 계정으로 로그인해도 남의 알림 공고가 열리지 않는다.
        val viewModel = MainViewModel(FakeAuthRepository(loggedIn = false), FakeUserProfileRepository.strict(), reporter)
        viewModel.onDeepLink(AppDeepLink.PostingDetail(101))

        viewModel.refresh()

        assertEquals(AppStartDestination.Login, viewModel.destination)
        assertNull(viewModel.pendingDeepLink.value)
    }
}
