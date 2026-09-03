package com.cambridge.feature.feed.presentation.reporting

import com.cambridge.core.common.reporting.ERROR_REPORT_KEY_TYPE
import com.cambridge.core.domain.error.CoreDataFailure
import com.cambridge.feature.feed.presentation.RecordingErrorReporter
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.net.UnknownHostException

class FeedFailureReportingTest {
    private val reporter = RecordingErrorReporter()

    @Test
    fun `그 밖의 실패는 단계 속성과 함께 기록한다`() {
        val throwable = CoreDataFailure.ServerError("INTERNAL_ERROR", RuntimeException())

        reporter.recordFeedFailure(FeedFailureStage.FeedLoad, throwable)

        assertEquals(listOf("feed_load"), reporter.stages)
        assertEquals(
            CoreDataFailure.ServerError::class.java.name,
            reporter.records.single().second[ERROR_REPORT_KEY_TYPE],
        )
    }

    @Test
    fun `네트워크 단절은 예상된 상태라 기록하지 않는다`() {
        reporter.recordFeedFailure(FeedFailureStage.FeedLoad, CoreDataFailure.NetworkUnavailable(UnknownHostException()))

        assertTrue(reporter.records.isEmpty())
    }

    @Test
    fun `서버 점검 503 도 네트워크 단절과 같은 예상된 상태다`() {
        reporter.recordFeedFailure(
            FeedFailureStage.PostingDetail,
            CoreDataFailure.ServiceUnavailable("LLM_UNAVAILABLE", RuntimeException()),
        )

        assertTrue(reporter.records.isEmpty())
    }
}
