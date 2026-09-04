package com.cambridge.feature.onboarding.presentation.reporting

import com.careercompass.core.common.reporting.ERROR_REPORT_KEY_TRANSPORT
import com.careercompass.core.common.reporting.ERROR_REPORT_KEY_TYPE
import com.careercompass.core.domain.error.CoreAuthFailure
import com.careercompass.core.model.auth.SocialProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException
import java.net.UnknownHostException
import javax.net.ssl.SSLHandshakeException
import kotlin.coroutines.cancellation.CancellationException

class OnboardingFailureReportingTest {
    private val reporter = RecordingErrorReporter()

    @Test
    fun `단계와 제공자를 속성으로 남긴다`() {
        reporter.recordOnboardingFailure(OnboardingFailureStage.SocialLogin, IOException("offline"), SocialProvider.Kakao)

        val recorded = reporter.failures.single()
        assertEquals("social_login", recorded.attributes[ONBOARDING_REPORT_KEY_STAGE])
        assertEquals("kakao", recorded.attributes[ONBOARDING_REPORT_KEY_PROVIDER])
        assertEquals(IOException::class.java.name, recorded.attributes[ERROR_REPORT_KEY_TYPE])
    }

    @Test
    fun `제공자가 없으면 속성을 넣지 않는다`() {
        reporter.recordOnboardingFailure(OnboardingFailureStage.SaveBasicInfo, IllegalStateException("boom"))

        val recorded = reporter.failures.single()
        assertEquals("save_basic_info", recorded.attributes[ONBOARDING_REPORT_KEY_STAGE])
        assertNull(recorded.attributes[ONBOARDING_REPORT_KEY_PROVIDER])
    }

    @Test
    fun `오프라인 로그인 재시도는 세션 표본 한 건만 남긴다`() {
        repeat(3) {
            reporter.recordOnboardingFailure(
                OnboardingFailureStage.SocialLogin,
                CoreAuthFailure.NetworkUnavailable(UnknownHostException()),
                SocialProvider.Kakao,
            )
        }

        assertEquals(listOf("social_login"), reporter.stages())
        assertEquals("transient", reporter.failures.single().attributes[ERROR_REPORT_KEY_TRANSPORT])
    }

    @Test
    fun `TLS 회귀는 네트워크 실패로 접혀 와도 매번 기록한다`() {
        repeat(2) {
            reporter.recordOnboardingFailure(
                OnboardingFailureStage.SocialLogin,
                CoreAuthFailure.NetworkUnavailable(SSLHandshakeException("cert")),
            )
        }

        assertEquals(2, reporter.failures.size)
        assertEquals("defect", reporter.failures.first().attributes[ERROR_REPORT_KEY_TRANSPORT])
    }

    @Test
    fun `사용자 취소와 코루틴 취소는 기록하지 않는다`() {
        reporter.recordOnboardingFailure(
            OnboardingFailureStage.SocialTokenRequest,
            CoreAuthFailure.UserCancelledAuth(),
            SocialProvider.Google,
        )
        reporter.recordOnboardingFailure(OnboardingFailureStage.SocialLogin, CancellationException("cancelled"))

        assertTrue(reporter.failures.isEmpty())
    }
}
