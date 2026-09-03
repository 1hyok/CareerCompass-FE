package com.cambridge.careercompass_fe

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.cambridge.core.domain.testing.FakeAuthRepository
import com.cambridge.core.domain.testing.FakePostingRepository
import com.cambridge.core.domain.testing.FakeUserProfileRepository
import com.cambridge.core.model.posting.PostingBoardRef
import com.cambridge.core.model.posting.PostingDetail
import com.cambridge.core.model.posting.PostingType
import com.cambridge.core.model.user.JobInterest
import com.cambridge.core.model.user.UserProfile
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.time.Instant
import javax.inject.Inject

/**
 * 앱 셸의 시작 목적지 분기·그래프 배선·딥링크 인증 게이트를 fake 세션으로 확인한다 — 실제 서버·OAuth 없이.
 *
 * `createEmptyComposeRule` 을 쓰는 이유: fake 의 세션·프로필 상태를 Activity 가 뜨기 **전에** 정해야 시작 목적지가
 * 그 상태로 계산된다. `createAndroidComposeRule` 은 규칙 시작 시점에 Activity 를 띄운다.
 */
@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class AppNavigationAndroidTest {
    @get:Rule(order = 0)
    val hiltRule = HiltAndroidRule(this)

    @get:Rule(order = 1)
    val composeRule = createEmptyComposeRule()

    @Inject
    lateinit var fakeAuthRepository: FakeAuthRepository

    @Inject
    lateinit var fakeUserProfileRepository: FakeUserProfileRepository

    @Inject
    lateinit var fakePostingRepository: FakePostingRepository

    private var scenario: ActivityScenario<MainActivity>? = null

    @Before
    fun inject() {
        hiltRule.inject()
    }

    @After
    fun close() {
        scenario?.close()
    }

    @Test
    fun withoutSession_startsAtLoginScreen() {
        fakeAuthRepository.loggedIn = false

        scenario = ActivityScenario.launch(MainActivity::class.java)

        composeRule.onNodeWithTag("careercompass_app_start", useUnmergedTree = true).assertIsDisplayed()
        composeRule.onNodeWithText("카카오로 시작하기").assertIsDisplayed()
        composeRule.onNodeWithText("Google로 시작하기").assertIsDisplayed()
    }

    @Test
    fun withSessionAndOnboardingDone_startsAtFeedWithBottomTabs() {
        fakeAuthRepository.loggedIn = true
        fakeUserProfileRepository.profileState.value = profile(onboardingDone = true)

        scenario = ActivityScenario.launch(MainActivity::class.java)

        composeRule.onNodeWithText("안녕하세요, 정일혁님", substring = true, useUnmergedTree = true).assertIsDisplayed()
        composeRule.onNodeWithText("피드").assertIsDisplayed()
        composeRule.onNodeWithText("마이").performClick()
        composeRule.onNodeWithText("마이 탭을 준비하고 있어요").assertIsDisplayed()
    }

    /** 마이 탭 자리표시자의 로그아웃 — 확인 다이얼로그를 거쳐 세션이 끝나고 셸이 로그인 화면으로 되돌린다. */
    @Test
    fun myTabLogout_returnsToLoginScreen() {
        fakeAuthRepository.loggedIn = true
        fakeUserProfileRepository.profileState.value = profile(onboardingDone = true)

        scenario = ActivityScenario.launch(MainActivity::class.java)

        composeRule.onNodeWithText("마이").performClick()
        composeRule.onNodeWithText("정일혁").assertIsDisplayed()
        composeRule.onNodeWithText("로그아웃").performClick()
        composeRule.onNodeWithText("네, 로그아웃").performClick()

        // 로그아웃 → 시작 목적지 재계산 → NavHost 재생성까지가 비동기다.
        composeRule.waitUntil(timeoutMillis = LOGOUT_TIMEOUT_MILLIS) {
            composeRule
                .onAllNodesWithText("카카오로 시작하기")
                .fetchSemanticsNodes(atLeastOneRootRequired = false)
                .isNotEmpty()
        }
        composeRule.onNodeWithText("카카오로 시작하기").assertIsDisplayed()
        assertEquals(1, fakeAuthRepository.logoutCalls)
    }

    /**
     * 지문을 등록할 수 없는 기기(관리형 에뮬레이터엔 등록된 지문이 없다) — 스위치는 꺼진 채 잠기고 이유가 한 줄 붙는다.
     */
    @Test
    fun myTabBiometricSwitch_withoutDeviceBiometrics_isOffAndDisabled() {
        fakeAuthRepository.loggedIn = true
        fakeUserProfileRepository.profileState.value = profile(onboardingDone = true)

        scenario = ActivityScenario.launch(MainActivity::class.java)

        composeRule.onNodeWithText("마이").performClick()
        composeRule.onNodeWithText("지문 로그인").assertIsDisplayed()
        composeRule.onNodeWithTag(BIOMETRIC_SWITCH_TAG, useUnmergedTree = true).assertIsOff().assertIsNotEnabled()
        composeRule.onNodeWithText(BIOMETRIC_UNAVAILABLE_TEXT).assertIsDisplayed()
    }

    /**
     * 끄는 경로(#113) — 이 기기에 등록이 남아 있으면 지문을 쓸 수 없는 기기에서도 스위치가 열려 있고, 끄면 등록
     * 기록이 지워진다. 켜는 방향은 실기 지문이 있어야 프롬프트가 뜨므로 계측이 아니라 ViewModel 테스트가 덮는다.
     */
    @Test
    fun myTabBiometricSwitch_turnsOffQuickLogin() {
        fakeAuthRepository.loggedIn = true
        fakeUserProfileRepository.profileState.value = profile(onboardingDone = true)

        scenario = ActivityScenario.launch(MainActivity::class.java)

        composeRule.onNodeWithText("마이").performClick()
        // 등록 상태를 마이 탭에 들어온 뒤에 켠다 — 처음부터 켜져 있으면 시작 목적지가 지문 화면이라 여기 못 온다.
        fakeAuthRepository.biometricEnabledState.value = true
        composeRule.waitUntil(timeoutMillis = BIOMETRIC_TIMEOUT_MILLIS) {
            composeRule
                .onAllNodesWithText(BIOMETRIC_UNAVAILABLE_TEXT)
                .fetchSemanticsNodes(atLeastOneRootRequired = false)
                .isEmpty()
        }
        composeRule
            .onNodeWithTag(BIOMETRIC_SWITCH_TAG, useUnmergedTree = true)
            .assertIsOn()
            .assertIsEnabled()
            .performClick()

        composeRule.waitUntil(timeoutMillis = BIOMETRIC_TIMEOUT_MILLIS) { !fakeAuthRepository.biometricEnabledState.value }
        composeRule.onNodeWithTag(BIOMETRIC_SWITCH_TAG, useUnmergedTree = true).assertIsOff()
        assertEquals(1, fakeAuthRepository.setBiometricEnabledCalls)
    }

    @Test
    fun withSessionButOnboardingNotDone_startsAtOnboardingStep1() {
        fakeAuthRepository.loggedIn = true
        fakeUserProfileRepository.profileState.value = profile(onboardingDone = false)

        scenario = ActivityScenario.launch(MainActivity::class.java)

        composeRule.onNodeWithText("기본 정보를 알려주세요").assertIsDisplayed()
    }

    @Test
    fun withSessionAndDeepLink_opensPostingDetail() {
        fakeAuthRepository.loggedIn = true
        fakeUserProfileRepository.profileState.value = profile(onboardingDone = true)
        fakePostingRepository.details += postingDetail(id = DEEP_LINK_POSTING_ID, title = DEEP_LINK_POSTING_TITLE)

        scenario = ActivityScenario.launch<MainActivity>(postingDeepLinkIntent(DEEP_LINK_POSTING_ID))

        // 피드 홈에서 시작한 뒤 상세로 이동하고, 상세 ViewModel 이 fake 에서 공고를 읽어 제목을 그린다.
        composeRule.waitUntil(timeoutMillis = DEEP_LINK_TIMEOUT_MILLIS) {
            composeRule
                .onAllNodesWithText(DEEP_LINK_POSTING_TITLE, useUnmergedTree = true)
                .fetchSemanticsNodes(atLeastOneRootRequired = false)
                .isNotEmpty()
        }
        composeRule.onNodeWithText(DEEP_LINK_POSTING_TITLE, useUnmergedTree = true).assertIsDisplayed()
    }

    @Test
    fun withoutSessionAndDeepLink_staysOnLogin() {
        fakeAuthRepository.loggedIn = false
        fakePostingRepository.details += postingDetail(id = DEEP_LINK_POSTING_ID, title = DEEP_LINK_POSTING_TITLE)

        scenario = ActivityScenario.launch<MainActivity>(postingDeepLinkIntent(DEEP_LINK_POSTING_ID))

        // 인증 게이트 — 세션이 없으면 딥링크가 있어도 로그인 화면이며, 상세가 백스택에 오르지 않는다.
        composeRule.onNodeWithText("카카오로 시작하기").assertIsDisplayed()
        composeRule.onNodeWithText(DEEP_LINK_POSTING_TITLE, useUnmergedTree = true).assertDoesNotExist()
    }

    /** 알림 모듈이 만들 intent 와 같은 모양 — `careercompass://postings/{id}` 를 VIEW 로 앱에 보낸다. */
    private fun postingDeepLinkIntent(postingId: Long): Intent =
        Intent(Intent.ACTION_VIEW, Uri.parse("careercompass://postings/$postingId"))
            .setClass(ApplicationProvider.getApplicationContext<Context>(), MainActivity::class.java)

    private fun postingDetail(
        id: Long,
        title: String,
    ) = PostingDetail(
        id = id,
        title = title,
        type = PostingType.Recruit,
        board = PostingBoardRef(id = 1, name = "취업정보 게시판"),
        url = "https://example.com/postings/$id",
        rawContent = "본문",
        dueDate = null,
        collectedAt = Instant.parse("2026-09-01T00:00:00Z"),
        isRead = false,
        isBookmarked = false,
        parsed = null,
        suitability = null,
        similar = emptyList(),
    )

    private fun profile(onboardingDone: Boolean) =
        UserProfile(
            id = 1,
            name = "정일혁",
            school = "건국대학교",
            department = "컴퓨터공학부",
            gpa = 3.87,
            gradYear = 2027,
            jobInterests = listOf(JobInterest("backend", 1)),
            tags = listOf("AI"),
            onboardingDone = onboardingDone,
            completion = 78,
        )

    private companion object {
        const val DEEP_LINK_POSTING_ID = 101L
        const val DEEP_LINK_POSTING_TITLE = "딥링크로 연 2026 하반기 공채"
        const val DEEP_LINK_TIMEOUT_MILLIS = 10_000L
        const val LOGOUT_TIMEOUT_MILLIS = 10_000L
        const val BIOMETRIC_TIMEOUT_MILLIS = 10_000L

        /** `MyTabPlaceholderScreen` 의 `MY_TAB_BIOMETRIC_SWITCH_TAG` — 라벨은 스위치의 토글 상태를 병합하지 않는다. */
        const val BIOMETRIC_SWITCH_TAG = "my_tab_biometric_switch"
        const val BIOMETRIC_UNAVAILABLE_TEXT = "이 기기에서는 지문 로그인을 켤 수 없어요"
    }
}
