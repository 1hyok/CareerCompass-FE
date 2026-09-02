package com.cambridge.careercompass_fe.session

import com.cambridge.core.common.reporting.ErrorReporter
import com.cambridge.core.domain.error.CoreDataFailure
import com.cambridge.core.domain.testing.FakeAuthRepository
import com.cambridge.core.domain.testing.FakeUserProfileRepository
import com.cambridge.core.domain.usecase.auth.ResolveSessionEntryUseCase
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

    private fun unauthorized() =
        Result.failure<UserProfile>(
            CoreDataFailure.Unauthorized("AUTH_INVALID", ApiException("AUTH_INVALID", null, "만료", status = 401)),
        )

    private val MainViewModel.destination: AppStartDestination? get() = launch.value?.destination

    /** 실제 배선과 같게 — 세션 진입 판정은 두 리포지토리를 받는 use case 가 한다. */
    private fun mainViewModel(
        authRepository: FakeAuthRepository,
        userProfileRepository: FakeUserProfileRepository,
    ) = MainViewModel(
        authRepository,
        userProfileRepository,
        ResolveSessionEntryUseCase(authRepository, userProfileRepository),
        reporter,
    )

    @Test
    fun `세션이 없으면 로그인으로 시작한다`() {
        val viewModel = mainViewModel(FakeAuthRepository(loggedIn = false), FakeUserProfileRepository.strict())

        assertEquals(AppStartDestination.Login, viewModel.destination)
    }

    @Test
    fun `세션이 있고 지문 로그인을 켰으면 지문 화면으로 시작한다`() {
        val viewModel =
            mainViewModel(FakeAuthRepository(loggedIn = true, biometricEnabled = true), FakeUserProfileRepository.strict())

        assertEquals(AppStartDestination.BiometricLogin, viewModel.destination)
    }

    @Test
    fun `온보딩을 마치지 않은 세션은 온보딩으로, 마친 세션은 메인으로 간다`() {
        val notDone = mainViewModel(FakeAuthRepository(loggedIn = true), FakeUserProfileRepository(profile(false)))
        val done = mainViewModel(FakeAuthRepository(loggedIn = true), FakeUserProfileRepository(profile(true)))

        assertEquals(AppStartDestination.Onboarding, notDone.destination)
        assertEquals(AppStartDestination.Main, done.destination)
    }

    // ── 시작 경로에서 네트워크를 빼는 부분 (#74) ────────────────────────────────

    @Test
    fun `캐시가 완료면 프로필 조회를 기다리지 않고 메인으로 확정한다`() {
        // 스플래시가 이 값까지만 붙잡히므로, 서버 응답을 기다리면 그만큼 콜드 스타트가 늘어난다.
        val gate = CompletableDeferred<Result<UserProfile>>()
        val profiles = FakeUserProfileRepository(profile(true)).apply { onRefreshProfile = { gate.await() } }

        val viewModel = mainViewModel(FakeAuthRepository(loggedIn = true), profiles)

        assertEquals(AppStartDestination.Main, viewModel.destination)
        gate.complete(Result.success(profile(true)))
    }

    @Test
    fun `캐시가 미완료면 프로필 조회 없이 온보딩으로 보낸다`() {
        // 온보딩 진입 판정이 서버를 다시 확인하므로 그 사이 완료된 사용자는 거기서 피드로 간다.
        var refreshCalls = 0
        val profiles =
            FakeUserProfileRepository(profile(false)).apply {
                onRefreshProfile = {
                    refreshCalls += 1
                    Result.success(profile(false))
                }
            }

        val viewModel = mainViewModel(FakeAuthRepository(loggedIn = true), profiles)

        assertEquals(AppStartDestination.Onboarding, viewModel.destination)
        assertEquals(0, refreshCalls)
    }

    @Test
    fun `캐시로 메인에 들어간 뒤 서버가 미완료라고 하면 온보딩으로 다시 계산한다`() {
        // 캐시(profile(true))는 그대로 두고 서버만 미완료를 돌려준다 — 실제로 캐시 쓰기가 실패한 상황과 같다.
        // 확인이 한 번으로 끝나지 않으면 재계산이 또 메인으로 가서 또 확인을 걸어 영원히 돈다.
        var refreshCalls = 0
        val profiles =
            FakeUserProfileRepository(profile(true)).apply {
                onRefreshProfile = {
                    refreshCalls += 1
                    Result.success(profile(false))
                }
            }

        val viewModel = mainViewModel(FakeAuthRepository(loggedIn = true), profiles)

        // 백그라운드 확인이 캐시를 뒤집었다 — 서버가 확정한 답이라 화면을 옮긴다.
        assertEquals(AppStartDestination.Onboarding, viewModel.destination)
        // 회귀 가드: 확인은 정확히 한 번이다. 0903 에 여기서 무한 재귀가 나 테스트 워커가 코루틴
        // 1억 4천만 개를 만들며 CPU 를 태웠고, 빌드가 끝나지 않았다.
        assertEquals(1, refreshCalls)
    }

    @Test
    fun `캐시로 메인에 들어간 뒤 세션이 만료됐으면 세션을 정리하고 로그인으로 간다`() {
        val auth = FakeAuthRepository(loggedIn = true)
        val profiles = FakeUserProfileRepository(profile(true)).apply { onRefreshProfile = { unauthorized() } }

        val viewModel = mainViewModel(auth, profiles)

        assertEquals(AppStartDestination.Login, viewModel.destination)
        assertTrue(auth.clearSessionCalls > 0)
    }

    @Test
    fun `캐시로 메인에 들어간 뒤 서버 확인이 실패하면 목적지를 유지하고 기록만 남긴다`() {
        // 오프라인 시작 — 이미 캐시로 들어와 있으므로 화면을 흔들 이유가 없다.
        val profiles = FakeUserProfileRepository(profile(true)).apply { onRefreshProfile = { networkFailure() } }

        val viewModel = mainViewModel(FakeAuthRepository(loggedIn = true), profiles)

        assertEquals(AppStartDestination.Main, viewModel.destination)
        assertEquals(1, reporter.recorded.size)
        assertEquals("start_profile", reporter.recorded.single()["app_stage"])
    }

    // ── 프로필도 힌트도 없을 때만 서버를 기다린다 ──────────────────────────────

    @Test
    fun `아무것도 모르면 프로필 조회를 기다린다`() {
        val gate = CompletableDeferred<Result<UserProfile>>()
        val profiles = FakeUserProfileRepository().apply { onRefreshProfile = { gate.await() } }

        val viewModel = mainViewModel(FakeAuthRepository(loggedIn = true), profiles)

        assertNull(viewModel.launch.value)
        gate.complete(Result.success(profile(true)))
        assertEquals(AppStartDestination.Main, viewModel.destination)
    }

    @Test
    fun `프로필 조회가 401 이면 로그인으로 보낸다`() {
        val profiles = FakeUserProfileRepository().apply { onRefreshProfile = { unauthorized() } }

        val viewModel = mainViewModel(FakeAuthRepository(loggedIn = true), profiles)

        assertEquals(AppStartDestination.Login, viewModel.destination)
    }

    @Test
    fun `완료 여부를 전혀 모르는데 조회도 실패하면 메인이 아니라 온보딩으로 보낸다`() {
        // 신규 사용자의 첫 프로필 조회가 네트워크로 실패한 경우 — 메인으로 추정하면 온보딩을 영영 건너뛴다.
        val unknown = FakeUserProfileRepository().apply { onRefreshProfile = { networkFailure() } }

        val viewModel = mainViewModel(FakeAuthRepository(loggedIn = true), unknown)

        assertEquals(AppStartDestination.Onboarding, viewModel.destination)
        assertEquals("start_profile", reporter.recorded.single()["app_stage"])
    }

    @Test
    fun `로그인 힌트만 있어도 기다리지 않고 메인으로 간다`() {
        val hintDone =
            FakeUserProfileRepository().apply {
                onboardingDoneHint = true
                onRefreshProfile = { networkFailure() }
            }

        val viewModel = mainViewModel(FakeAuthRepository(loggedIn = true), hintDone)

        assertEquals(AppStartDestination.Main, viewModel.destination)
    }

    // ── 재계산 ────────────────────────────────────────────────────────────────

    @Test
    fun `다시 계산하면 목적지가 같아도 revision 이 올라 NavHost 가 새로 만들어진다`() {
        val viewModel = mainViewModel(FakeAuthRepository(loggedIn = false), FakeUserProfileRepository.strict())
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
        val viewModel = mainViewModel(FakeAuthRepository(loggedIn = true), profiles)
        assertNull(viewModel.launch.value)

        viewModel.refresh()
        viewModel.refresh()
        gate.complete(Result.success(profile(true)))

        assertEquals(1, refreshCalls)
        assertEquals(AppStartDestination.Main, viewModel.destination)
    }
}
