package com.careercompass.feature.onboarding.presentation.flow

import androidx.compose.material3.Text
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import com.careercompass.core.ui.failure.FailureSurface
import com.careercompass.core.ui.theme.CareerCompassTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * 온보딩 배너가 실패 표(#204)의 문장을 읽는지 본다(#236) — 같은 사실을 다른 화면과 다르게 말하던 회귀는 여기서만
 * 잡힌다.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class OnboardingFailureMessageTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun `연결 없음은 부품과 같은 문장을 읽는다`() {
        setMessage(OnboardingFailureReason.Network)

        composeRule.onNodeWithText("연결할 수 없어요. 인터넷 연결을 확인하고 다시 시도해 주세요").assertIsDisplayed()
    }

    @Test
    fun `점검은 서버 오류가 아니라 점검 문장을 읽는다`() {
        setMessage(OnboardingFailureReason.Maintenance)

        composeRule.onNodeWithText("서비스가 잠시 점검 중이에요. AI 분석 서버를 손보고 있어요.\n조금 뒤에 다시 열어 주세요").assertIsDisplayed()
    }

    @Test
    fun `상한은 문맥이 있으면 무엇이 몇 개까지인지 말하고 없으면 개수를 말하지 않는다`() {
        setMessage(OnboardingFailureReason.LimitExceeded(FailureSurface.ExperienceCard))
        composeRule.onNodeWithText("경험 카드를 더 만들 수 없어요. 최대 30개까지 만들 수 있어요").assertIsDisplayed()
    }

    @Test
    fun `문맥 없는 상한은 개수를 말하지 않는다`() {
        setMessage(OnboardingFailureReason.LimitExceeded(FailureSurface.Unspecified))
        composeRule.onNodeWithText("더 담을 수 없어요. 쓰지 않는 항목을 지우고 다시 시도해 주세요").assertIsDisplayed()
    }

    @Test
    fun `화면 고유 사유는 온보딩 문자열로 남는다`() {
        setMessage(OnboardingFailureReason.FileTooLarge)
        composeRule.onNodeWithText("파일은 10MB 이하만 올릴 수 있어요").assertIsDisplayed()
    }

    private fun setMessage(reason: OnboardingFailureReason) {
        composeRule.setContent {
            CareerCompassTheme {
                Text(text = reason.toMessage())
            }
        }
    }
}
