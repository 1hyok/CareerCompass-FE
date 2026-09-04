package com.careercompass.core.common.reporting

import com.careercompass.core.domain.error.CoreAuthFailure
import com.careercompass.core.domain.error.CoreDataFailure
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.EOFException
import java.io.IOException
import java.net.ConnectException
import java.net.ProtocolException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import java.net.UnknownServiceException
import javax.net.ssl.SSLHandshakeException
import javax.net.ssl.SSLPeerUnverifiedException

/**
 * 원인 분류와 세션 표본 규칙의 고정 테스트.
 *
 * 여기서 갈린 판정이 곧 「콘솔에 남느냐」다. 특히 [UnknownServiceException](cleartext 차단)처럼
 * 우리 설정 결함인 예외가 오프라인과 같은 값으로 접혀 사라졌던 회귀를 이 테스트가 막는다.
 */
class StagedFailureReportingTest {
    private class RecordingReporter : ErrorReporter {
        val records = mutableListOf<Map<String, String>>()

        override fun writeFailure(
            throwable: Throwable,
            attributes: Map<String, String>,
        ) {
            records += attributes
        }
    }

    private val reporter = RecordingReporter()

    @Test
    fun `설정과 프로토콜 계열은 결함으로 가른다`() {
        assertEquals(TransportFailureKind.Defect, SSLHandshakeException("handshake").transportFailureKind())
        assertEquals(TransportFailureKind.Defect, SSLPeerUnverifiedException("peer").transportFailureKind())
        assertEquals(TransportFailureKind.Defect, UnknownServiceException("CLEARTEXT not permitted").transportFailureKind())
        assertEquals(TransportFailureKind.Defect, ProtocolException("unexpected end of stream").transportFailureKind())
        assertEquals(TransportFailureKind.Defect, EOFException().transportFailureKind())
        assertEquals(TransportFailureKind.Defect, IOException("정체 불명").transportFailureKind())
    }

    @Test
    fun `전송이 닿지 못한 실패는 일시적으로 가른다`() {
        assertEquals(TransportFailureKind.Transient, UnknownHostException("api.example.com").transportFailureKind())
        assertEquals(TransportFailureKind.Transient, SocketTimeoutException("timeout").transportFailureKind())
        assertEquals(TransportFailureKind.Transient, ConnectException("refused").transportFailureKind())
    }

    @Test
    fun `도메인 사유로 접힌 원인을 풀어서 본다`() {
        assertEquals(
            TransportFailureKind.Defect,
            CoreDataFailure.NetworkUnavailable(UnknownServiceException("cleartext")).transportFailureKind(),
        )
        assertEquals(
            TransportFailureKind.Transient,
            CoreAuthFailure.NetworkUnavailable(SocketTimeoutException()).transportFailureKind(),
        )
    }

    @Test
    fun `전송 계층 실패가 아니면 갈래가 없다`() {
        assertNull(IllegalStateException("boom").transportFailureKind())
        assertNull(CoreDataFailure.ServerError("INTERNAL_ERROR", RuntimeException()).transportFailureKind())
    }

    @Test
    fun `설정 결함은 반복해도 매번 기록한다`() {
        repeat(3) {
            reporter.record(CoreDataFailure.NetworkUnavailable(UnknownServiceException("cleartext")))
        }

        assertEquals(3, reporter.records.size)
        assertEquals("defect", reporter.records.first()[ERROR_REPORT_KEY_TRANSPORT])
    }

    @Test
    fun `일시적 실패는 원인과 단계 조합마다 세션 첫 건만 남긴다`() {
        repeat(5) { reporter.record(CoreDataFailure.NetworkUnavailable(UnknownHostException()), stage = "feed_load") }
        reporter.record(CoreDataFailure.NetworkUnavailable(UnknownHostException()), stage = "posting_detail")
        reporter.record(CoreDataFailure.NetworkUnavailable(SocketTimeoutException()), stage = "feed_load")

        assertEquals(
            listOf("feed_load", "posting_detail", "feed_load"),
            reporter.records.map { it[STAGE_KEY] },
        )
        assertTrue(reporter.records.all { it[ERROR_REPORT_KEY_TRANSPORT] == "transient" })
    }

    @Test
    fun `세션은 리포터마다 따로 센다`() {
        reporter.record(CoreDataFailure.NetworkUnavailable(UnknownHostException()))
        val other = RecordingReporter()

        other.record(CoreDataFailure.NetworkUnavailable(UnknownHostException()))

        assertEquals(1, reporter.records.size)
        assertEquals(1, other.records.size)
    }

    @Test
    fun `서버가 알린 상태와 사용자 취소는 기록하지 않는다`() {
        reporter.record(CoreDataFailure.ServiceUnavailable("LLM_UNAVAILABLE", RuntimeException()))
        reporter.record(CoreAuthFailure.UserCancelledAuth())

        assertTrue(reporter.records.isEmpty())
    }

    @Test
    fun `전송 실패가 아닌 실패는 단계와 추가 속성만 남긴다`() {
        reporter.record(
            CoreDataFailure.ServerError("INTERNAL_ERROR", RuntimeException()),
            attributes = mapOf("auth_provider" to "kakao"),
        )

        val recorded = reporter.records.single()
        assertEquals("feed_load", recorded[STAGE_KEY])
        assertEquals("kakao", recorded["auth_provider"])
        assertNull(recorded[ERROR_REPORT_KEY_TRANSPORT])
    }

    private fun ErrorReporter.record(
        throwable: Throwable,
        stage: String = "feed_load",
        attributes: Map<String, String> = emptyMap(),
    ) = recordStagedFailure(stageKey = STAGE_KEY, stage = stage, throwable = throwable, attributes = attributes)

    private companion object {
        const val STAGE_KEY = "test_stage"
    }
}
