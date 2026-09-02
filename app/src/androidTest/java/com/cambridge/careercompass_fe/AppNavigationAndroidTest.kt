package com.cambridge.careercompass_fe

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.cambridge.core.domain.testing.FakeAuthRepository
import com.cambridge.core.domain.testing.FakeUserProfileRepository
import com.cambridge.core.model.user.JobInterest
import com.cambridge.core.model.user.UserProfile
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import javax.inject.Inject

/**
 * 앱 셸의 시작 목적지 분기와 그래프 배선을 fake 세션으로 확인한다 — 실제 서버·OAuth 없이.
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

        composeRule.onNodeWithText("안녕하세요, 정일혁님").assertIsDisplayed()
        composeRule.onNodeWithText("피드").assertIsDisplayed()
        composeRule.onNodeWithText("마이").performClick()
        composeRule.onNodeWithText("마이 탭을 준비하고 있어요").assertIsDisplayed()
    }

    @Test
    fun withSessionButOnboardingNotDone_startsAtOnboardingStep1() {
        fakeAuthRepository.loggedIn = true
        fakeUserProfileRepository.profileState.value = profile(onboardingDone = false)

        scenario = ActivityScenario.launch(MainActivity::class.java)

        composeRule.onNodeWithText("기본 정보를 알려주세요").assertIsDisplayed()
    }

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
}
