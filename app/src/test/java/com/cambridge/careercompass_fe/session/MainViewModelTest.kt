package com.cambridge.careercompass_fe.session

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
import org.junit.Assert.assertFalse
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

    private fun unauthorized() =
        Result.failure<UserProfile>(
            CoreDataFailure.Unauthorized("AUTH_INVALID", ApiException("AUTH_INVALID", null, "만료", status = 401)),
        )

    /**
     * 서버 조회를 [gate] 가 열릴 때까지 잡아 두는 fake. 성공 결과는 실제 리포지토리처럼 캐시([FakeUserProfileRepository.profileState])에도
     * 반영한다 — 다시 계산이 그 캐시를 읽는다.
     */
    private class GatedProfiles(
        initialProfile: UserProfile?,
        onboardingDoneHint: Boolean? = null,
    ) {
        val gate = CompletableDeferred<Result<UserProfile>>()
        var refreshCalls = 0
        val repository =
            FakeUserProfileRepository(initialProfile = initialProfile, onboardingDoneHint = onboardingDoneHint).apply {
                onRefreshProfile = {
                    refreshCalls += 1
                    gate.await().also { result -> result.getOrNull()?.let { fresh -> profileState.value = fresh } }
                }
            }
    }

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
    fun `캐시 프로필이 온보딩 완료면 서버 조회를 기다리지 않고 메인으로 시작한다`() {
        val profiles = GatedProfiles(profile(true))

        val viewModel = MainViewModel(FakeAuthRepository(loggedIn = true), profiles.repository, reporter)

        // 조회는 백그라운드로 한 번 나갔지만 아직 응답이 없다 — 목적지는 이미 확정됐다.
        assertEquals(AppStartDestination.Main, viewModel.destination)
        assertEquals(1, profiles.refreshCalls)
        assertFalse(profiles.gate.isCompleted)
    }

    @Test
    fun `로그인 힌트만으로도 완료를 알면 메인으로 시작한다`() {
        val profiles = GatedProfiles(initialProfile = null, onboardingDoneHint = true)

        val viewModel = MainViewModel(FakeAuthRepository(loggedIn = true), profiles.repository, reporter)

        assertEquals(AppStartDestination.Main, viewModel.destination)
        assertEquals(1, profiles.refreshCalls)
    }

    @Test
    fun `캐시 프로필이 온보딩 미완료면 서버 조회 없이 온보딩으로 시작한다`() {
        // strict — refreshProfile 이 불리면 실패한다. 캐시 읽기만 연다.
        val profiles = FakeUserProfileRepository.strict(profile(false)).apply { onLastKnownOnboardingDone = null }

        val viewModel = MainViewModel(FakeAuthRepository(loggedIn = true), profiles, reporter)

        assertEquals(AppStartDestination.Onboarding, viewModel.destination)
    }

    @Test
    fun `캐시도 힌트도 없으면 서버 조회를 기다려 완료 여부로 가른다`() {
        val done = GatedProfiles(initialProfile = null)
        val notDone = GatedProfiles(initialProfile = null)
        val doneViewModel = MainViewModel(FakeAuthRepository(loggedIn = true), done.repository, reporter)
        val notDoneViewModel = MainViewModel(FakeAuthRepository(loggedIn = true), notDone.repository, reporter)
        assertNull(doneViewModel.launch.value)
        assertNull(notDoneViewModel.launch.value)

        done.gate.complete(Result.success(profile(true)))
        notDone.gate.complete(Result.success(profile(false)))

        assertEquals(AppStartDestination.Main, doneViewModel.destination)
        assertEquals(AppStartDestination.Onboarding, notDoneViewModel.destination)
        // 서버로 확정한 목적지는 백그라운드 재확인이 없다.
        assertEquals(1, done.refreshCalls)
    }

    @Test
    fun `캐시가 없고 프로필 조회가 401 이면 로그인으로 보낸다`() {
        val profiles = FakeUserProfileRepository().apply { onRefreshProfile = { unauthorized() } }

        val viewModel = MainViewModel(FakeAuthRepository(loggedIn = true), profiles, reporter)

        assertEquals(AppStartDestination.Login, viewModel.destination)
    }

    @Test
    fun `캐시가 없고 프로필 조회가 네트워크로 실패하면 온보딩으로 보내고 기록한다`() {
        // 신규 사용자의 첫 프로필 조회가 네트워크로 실패한 경우 — 메인으로 추정하면 온보딩을 영영 건너뛴다.
        val profiles = FakeUserProfileRepository().apply { onRefreshProfile = { networkFailure() } }

        val viewModel = MainViewModel(FakeAuthRepository(loggedIn = true), profiles, reporter)

        assertEquals(AppStartDestination.Onboarding, viewModel.destination)
        assertEquals(1, reporter.recorded.size)
        assertEquals("start_profile", reporter.recorded.single()["app_stage"])
    }

    @Test
    fun `백그라운드 확인이 401 이면 세션을 지우고 로그인으로 다시 계산한다`() {
        val auth = FakeAuthRepository(loggedIn = true)
        val profiles = GatedProfiles(profile(true))
        val viewModel = MainViewModel(auth, profiles.repository, reporter)
        val first = requireNotNull(viewModel.launch.value)

        profiles.gate.complete(unauthorized())

        val second = requireNotNull(viewModel.launch.value)
        assertEquals(AppStartDestination.Login, second.destination)
        assertTrue(second.revision > first.revision)
        assertEquals(1, auth.clearSessionCalls)
        assertTrue(reporter.recorded.isEmpty())
    }

    @Test
    fun `백그라운드 확인 결과가 온보딩 미완료면 온보딩으로 다시 계산한다`() {
        val profiles = GatedProfiles(profile(true))
        val viewModel = MainViewModel(FakeAuthRepository(loggedIn = true), profiles.repository, reporter)
        val first = requireNotNull(viewModel.launch.value)

        profiles.gate.complete(Result.success(profile(false)))

        val second = requireNotNull(viewModel.launch.value)
        assertEquals(AppStartDestination.Onboarding, second.destination)
        assertTrue(second.revision > first.revision)
        // 온보딩으로 간 뒤에는 다시 확인하지 않는다 — 온보딩 그래프가 서버를 본다.
        assertEquals(1, profiles.refreshCalls)
    }

    @Test
    fun `백그라운드 확인이 네트워크로 실패하면 메인을 유지하고 한 번만 기록한다`() {
        val profiles = GatedProfiles(profile(true))
        val viewModel = MainViewModel(FakeAuthRepository(loggedIn = true), profiles.repository, reporter)
        val first = requireNotNull(viewModel.launch.value)

        profiles.gate.complete(networkFailure())

        assertEquals(first, viewModel.launch.value)
        assertEquals(1, reporter.recorded.size)
        assertEquals("start_profile", reporter.recorded.single()["app_stage"])
    }

    @Test
    fun `백그라운드 확인이 완료 프로필을 돌려주면 목적지와 revision 을 유지한다`() {
        val profiles = GatedProfiles(profile(true))
        val viewModel = MainViewModel(FakeAuthRepository(loggedIn = true), profiles.repository, reporter)
        val first = requireNotNull(viewModel.launch.value)

        profiles.gate.complete(Result.success(profile(true)))

        assertEquals(first, viewModel.launch.value)
        assertEquals(1, profiles.refreshCalls)
        assertTrue(reporter.recorded.isEmpty())
    }

    @Test
    fun `다시 계산이 시작되면 이전 백그라운드 확인 결과는 버린다`() {
        val auth = FakeAuthRepository(loggedIn = true)
        val profiles = GatedProfiles(profile(true))
        val viewModel = MainViewModel(auth, profiles.repository, reporter)
        assertEquals(AppStartDestination.Main, viewModel.destination)

        // 로그아웃 → 다시 계산. 그 뒤 옛 세션의 401 이 도착해도 세션 정리·재계산을 다시 하지 않는다.
        auth.loggedIn = false
        viewModel.refresh()
        val afterLogout = requireNotNull(viewModel.launch.value)
        profiles.gate.complete(unauthorized())

        assertEquals(AppStartDestination.Login, afterLogout.destination)
        assertEquals(afterLogout, viewModel.launch.value)
        assertEquals(0, auth.clearSessionCalls)
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
        val profiles = GatedProfiles(initialProfile = null)
        val viewModel = MainViewModel(FakeAuthRepository(loggedIn = true), profiles.repository, reporter)
        assertNull(viewModel.launch.value)

        viewModel.refresh()
        viewModel.refresh()
        profiles.gate.complete(Result.success(profile(true)))

        assertEquals(1, profiles.refreshCalls)
        assertEquals(AppStartDestination.Main, viewModel.destination)
    }
}
