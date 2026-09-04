package com.cambridge.feature.feed.presentation.shared.model

import com.cambridge.core.domain.error.CoreDataFailure
import com.cambridge.core.model.board.MAX_BOARDS
import com.cambridge.core.ui.failure.FailureKind
import com.cambridge.feature.feed.presentation.board.BoardDetectionFailure
import com.cambridge.feature.feed.presentation.board.BoardRegisterMessage
import com.cambridge.feature.feed.presentation.board.isRetryable
import com.cambridge.feature.feed.presentation.board.toLabel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import java.net.UnknownHostException

/**
 * 피드가 실패 표(`core:ui`)를 쓰기 시작한 뒤에도 **제 판정을 잃지 않았는지** 본다(#204).
 *
 * 표는 문구의 정본이지 판정의 정본이 아니다. 사유를 셋으로 가른 것은 사용자가 할 일이 달라서고
 * (#144 의 조건 초기화, #101 의 점검 전용 화면), 그 갈래가 표로 옮기며 뭉개지면 「연결이 끊긴
 * 사용자에게 조건을 지우라고 하는」 화면이 되돌아온다.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class FeedFailureCopyTest {
    private val resources = RuntimeEnvironment.getApplication().resources
    private val cause = RuntimeException("boom")

    @Test
    fun `피드 실패 사유가 실패 표의 행으로 이어진다`() {
        val network = CoreDataFailure.NetworkUnavailable(UnknownHostException("no dns"))
        val maintenance = CoreDataFailure.ServiceUnavailable("LLM_UNAVAILABLE", cause)
        val unknown = IllegalStateException("정체불명")

        assertEquals(FailureKind.NoConnection, network.toFeedFailureReason().failureKind)
        assertEquals(FailureKind.ServiceUnavailable, maintenance.toFeedFailureReason().failureKind)
        assertEquals(FailureKind.Unexpected, unknown.toFeedFailureReason().failureKind)
    }

    /** 문구를 표로 옮긴 것이 #144 의 판정을 건드리지 않았다는 확인. */
    @Test
    fun `표로 옮겨도 조건 초기화 판정은 그대로다`() {
        assertFalse(FeedFailureReason.NetworkUnavailable.isQueryAttributable)
        assertTrue(FeedFailureReason.Maintenance.isQueryAttributable)
        assertTrue(FeedFailureReason.Generic.isQueryAttributable)
    }

    @Test
    fun `게시판 등록 안내는 표의 문구를 쓴다`() {
        assertEquals(
            "이미 등록된 게시판이에요. 내 게시판 목록에서 확인해 주세요",
            BoardRegisterMessage.AlreadyRegistered.toLabel(resources),
        )
        assertTrue(BoardRegisterMessage.NetworkUnavailable.toLabel(resources).contains("연결"))
        assertTrue(BoardRegisterMessage.LimitReached(MAX_BOARDS).toLabel(resources).contains("$MAX_BOARDS"))
    }

    /** 서버가 말한 상한이 표의 기본값과 어긋나도 사용자에게는 서버가 말한 쪽이 참이다. */
    @Test
    fun `상한 안내는 도메인이 들고 온 개수를 그대로 말한다`() {
        val label = BoardRegisterMessage.LimitReached(limit = 7).toLabel(resources)

        assertTrue(label, label.contains("7"))
        assertFalse(label, label.contains("$MAX_BOARDS"))
    }

    /**
     * 감지 실패에도 같은 규칙을 적용한다(#204) — 사이트 쪽 사정으로 막힌 셋은 다시 보내도 같은 답이 온다.
     *
     * 주소가 목록 페이지가 아니었을 뿐인 [BoardDetectionFailure.Failed] 만 예외로 남긴다.
     */
    @Test
    fun `감지 실패도 답이 갈리는 것만 재시도를 준다`() {
        val retryable = BoardDetectionFailure.entries.filter { it.isRetryable }.toSet()

        assertEquals(setOf(BoardDetectionFailure.Failed), retryable)
    }

    /**
     * 이 화면에서만 나는 안내는 표를 타지 않는다 — 「구조를 분석하지 못했다」는 §9 의 어느 코드도 아니고
     * 게시판 등록의 단계가 만들어 내는 상태다.
     */
    @Test
    fun `화면 고유의 안내는 표에 넣지 않는다`() {
        assertEquals(
            "등록을 끝내는 중이에요. 잠시만 기다려 주세요",
            BoardRegisterMessage.SubmitInProgress.toLabel(resources),
        )
        assertEquals(
            "구조를 분석하지 못했어요. 잠시 후 다시 시도해 주세요",
            BoardRegisterMessage.DetectFailed.toLabel(resources),
        )
    }
}
