package com.cambridge.core.ui.failure

import com.careercompass.core.domain.error.CoreDataFailure
import com.careercompass.core.model.application.MAX_PAST_APPLICATIONS
import com.careercompass.core.model.board.MAX_BOARDS
import com.careercompass.core.model.experience.MAX_EXPERIENCE_CARDS
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import java.net.SocketTimeoutException
import java.net.UnknownHostException

/**
 * 실패 표의 회귀 가드 — **표에 없는 실패가 화면에 닿지 않는다**는 것을 지킨다.
 *
 * 갈래가 하나 늘어도 `when` 이 exhaustive 라 컴파일이 먼저 막는다. 컴파일이 못 잡는 것은 두 가지다.
 * 1. 갈래는 늘었는데 **API_SPEC 의 어느 코드에서도 닿지 않는** 유령 행 → [`표의 모든 행은 코드나 전송 실패로 닿는다`]
 * 2. 문구는 붙었는데 **행동 판정이 뒤집힌 것** → [`재시도해도 답이 갈리지 않는 실패에는 재시도를 주지 않는다`]
 *
 * 변이 확인: `FailureKind.ParsingFailed` 의 행동을 `FailureAction.Retry` 로 바꾸면 재시도 테스트 두 개가
 * 함께 깨진다(2026-09-04 실측).
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class FailureDisplayTest {
    private val resources = RuntimeEnvironment.getApplication().resources
    private val cause = RuntimeException("boom")

    /**
     * API_SPEC v0.1 §9 의 에러 코드 14종. `docs/spec/error-copy.md` 의 표와 같은 순서다.
     *
     * 값은 data 계층(`ApiFailureMapper`)이 그 코드로 만드는 도메인 사유 그대로다 — 표가 「서버가 준
     * 코드」에서 시작해 「사용자가 읽는 문장」까지 끊기지 않는지를 이 목록 하나로 본다.
     */
    private val specRows: List<SpecRow> =
        listOf(
            SpecRow("INVALID_INPUT", CoreDataFailure.InvalidInput("INVALID_INPUT", field = "url", cause), FailureKind.InvalidInput),
            SpecRow("AUTH_REQUIRED", CoreDataFailure.Unauthorized("AUTH_REQUIRED", cause), FailureKind.AuthExpired),
            SpecRow("AUTH_INVALID", CoreDataFailure.Unauthorized("AUTH_INVALID", cause), FailureKind.AuthExpired),
            SpecRow("PERMISSION_DENIED", CoreDataFailure.Forbidden("PERMISSION_DENIED", cause), FailureKind.PermissionDenied),
            SpecRow("RESOURCE_NOT_FOUND", CoreDataFailure.NotFound("RESOURCE_NOT_FOUND", cause), FailureKind.NotFound),
            SpecRow("POSTING_NOT_FOUND", CoreDataFailure.NotFound("POSTING_NOT_FOUND", cause), FailureKind.NotFound),
            SpecRow("DUPLICATE_BOARD", CoreDataFailure.DuplicateBoard("DUPLICATE_BOARD", cause), FailureKind.DuplicateBoard),
            SpecRow("LIMIT_EXCEEDED", CoreDataFailure.LimitExceeded("LIMIT_EXCEEDED", cause), FailureKind.LimitExceeded),
            SpecRow("PROFILE_INCOMPLETE", CoreDataFailure.ProfileIncomplete("PROFILE_INCOMPLETE", cause), FailureKind.ProfileIncomplete),
            SpecRow("PARSING_FAILED", CoreDataFailure.ParsingFailed("PARSING_FAILED", cause), FailureKind.ParsingFailed),
            SpecRow("BOARD_BLOCKED", CoreDataFailure.BoardBlocked("BOARD_BLOCKED", cause), FailureKind.BoardBlocked),
            SpecRow("RATE_LIMITED", CoreDataFailure.RateLimited("RATE_LIMITED", cause), FailureKind.RateLimited),
            SpecRow("LLM_UNAVAILABLE", CoreDataFailure.ServiceUnavailable("LLM_UNAVAILABLE", cause), FailureKind.ServiceUnavailable),
            SpecRow("INTERNAL_ERROR", CoreDataFailure.ServerError("INTERNAL_ERROR", cause), FailureKind.Unexpected),
        )

    @Test
    fun `API_SPEC 14종이 빠짐없이 표에 있다`() {
        assertEquals(14, specRows.size)
        assertEquals(specRows.size, specRows.map(SpecRow::code).distinct().size)

        specRows.forEach { row ->
            assertEquals("${row.code} 의 갈래", row.kind, row.failure.toFailureKind())
            val display = row.failure.toFailureDisplay()
            assertTrue("${row.code} 의 제목이 비었다", display.title(resources).isNotBlank())
            assertTrue("${row.code} 의 본문이 비었다", display.description(resources).isNotBlank())
        }
    }

    /**
     * 표에 사용자가 절대 만날 수 없는 행이 남지 않게 한다.
     *
     * 갈래만 늘리고 코드 매핑을 잊으면 컴파일은 통과한다 — 그 행은 아무도 닿지 않는 문구가 되고,
     * 정작 서버가 준 코드는 [FailureKind.Unexpected] 로 내려앉아 「잠시 후 다시」만 말한다.
     */
    @Test
    fun `표의 모든 행은 코드나 전송 실패로 닿는다`() {
        val reachable = specRows.map(SpecRow::kind).toSet() + FailureKind.NoConnection + FailureKind.Timeout

        assertEquals(FailureKind.entries.toSet(), reachable)
    }

    @Test
    fun `코드 없는 실패는 연결 없음과 타임아웃으로 갈린다`() {
        val offline = CoreDataFailure.NetworkUnavailable(UnknownHostException("no dns"))
        val timedOut = CoreDataFailure.NetworkUnavailable(SocketTimeoutException("read timed out"))

        assertEquals(FailureKind.NoConnection, offline.toFailureKind())
        assertEquals(FailureKind.Timeout, timedOut.toFailureKind())
        assertNotEquals(
            offline.toFailureDisplay().titleRes,
            timedOut.toFailureDisplay().titleRes,
        )
    }

    @Test
    fun `사유를 확인하지 못한 실패도 문구가 있다`() {
        val unknown = IllegalStateException("정체불명")

        assertEquals(FailureKind.Unexpected, unknown.toFailureKind())
        assertTrue(unknown.toFailureDisplay().title(resources).isNotBlank())
        assertTrue(unknown.toFailureDisplay().isRetryable)
    }

    /**
     * 조건이 그대로면 답도 그대로인 실패 — 이슈 #204 가 이름으로 지목한 여섯이다.
     *
     * 여기에 재시도 버튼을 주면 사용자는 같은 실패를 되풀이해 만난다.
     */
    @Test
    fun `재시도해도 답이 갈리지 않는 실패에는 재시도를 주지 않는다`() {
        val pointless =
            listOf(
                FailureKind.ProfileIncomplete,
                FailureKind.ParsingFailed,
                FailureKind.LimitExceeded,
                FailureKind.DuplicateBoard,
                FailureKind.BoardBlocked,
                FailureKind.PermissionDenied,
            )

        pointless.forEach { kind ->
            FailureSurface.entries.forEach { surface ->
                val display = kind.display(surface)
                assertFalse("$kind($surface) 에 재시도가 붙었다", display.isRetryable)
                assertNotEquals("$kind($surface) 의 행동이 재시도다", FailureAction.Retry, display.action)
            }
        }
    }

    /** 반대 방향 — 시간이 지나면 갈리는 실패에서 재시도를 빼면 사용자는 막다른 골목에 남는다. */
    @Test
    fun `시간이 지나면 갈리는 실패에만 재시도가 붙는다`() {
        val retryable = FailureKind.entries.filter { it.display().isRetryable }.toSet()

        assertEquals(
            setOf(
                FailureKind.NoConnection,
                FailureKind.Timeout,
                FailureKind.RateLimited,
                FailureKind.ServiceUnavailable,
                FailureKind.Unexpected,
            ),
            retryable,
        )
    }

    @Test
    fun `할 수 있는 일이 있으면 버튼 문구가 있고 없으면 없다`() {
        FailureAction.entries.forEach { action ->
            if (action == FailureAction.None) {
                assertNull("행동 없음에 버튼 문구가 붙었다", action.labelRes)
            } else {
                val labelRes = action.labelRes
                assertNotNull("$action 에 버튼 문구가 없다", labelRes)
                assertTrue("$action 의 버튼 문구가 비었다", resources.getString(labelRes!!).isNotBlank())
            }
        }
    }

    @Test
    fun `프로필과 상한은 각각 채우러 가기와 지우러 가기로 이어진다`() {
        assertEquals(FailureAction.CompleteProfile, FailureKind.ProfileIncomplete.display().action)
        assertEquals(FailureAction.FreeUpSpace, FailureKind.LimitExceeded.display(FailureSurface.Board).action)
        assertEquals(FailureAction.SignIn, FailureKind.AuthExpired.display().action)
    }

    /** 404 는 없어진 것이 무엇이냐에 따라 다른 말을 해야 한다 — 공고와 게시판은 같은 문장을 쓰지 않는다. */
    @Test
    fun `같은 코드라도 화면 문맥이 문구를 가른다`() {
        val unspecified = FailureKind.NotFound.display().title(resources)
        val posting = FailureKind.NotFound.display(FailureSurface.Posting).title(resources)
        val board = FailureKind.NotFound.display(FailureSurface.Board).title(resources)

        assertEquals(3, setOf(unspecified, posting, board).size)
        assertTrue(posting.contains("공고"))
        assertTrue(board.contains("게시판"))
    }

    @Test
    fun `상한 초과는 문맥이 있을 때만 개수를 말한다`() {
        val board = FailureKind.LimitExceeded.display(FailureSurface.Board).description(resources)
        val experience = FailureKind.LimitExceeded.display(FailureSurface.ExperienceCard).description(resources)
        val application = FailureKind.LimitExceeded.display(FailureSurface.Application).description(resources)
        val unspecified = FailureKind.LimitExceeded.display()

        assertTrue(board.contains(MAX_BOARDS.toString()))
        assertTrue(experience.contains(MAX_EXPERIENCE_CARDS.toString()))
        assertTrue(application.contains(MAX_PAST_APPLICATIONS.toString()))
        assertEquals(emptyList<Any>(), unspecified.descriptionArgs)
        assertFalse(unspecified.description(resources).any(Char::isDigit))
    }

    /** 도메인이 자기 상한을 들고 오는 자리(`FeedFailure.BoardLimitReached`)를 위한 우회. */
    @Test
    fun `도메인이 넘긴 상한이 표의 기본값을 이긴다`() {
        val display = FailureKind.LimitExceeded.display(FailureSurface.Board, itemLimit = 7)

        assertEquals(listOf<Any>(7), display.descriptionArgs)
        assertTrue(display.description(resources).contains("7"))
    }

    /**
     * 문맥을 안 넘긴 자리도 말이 되는 문구가 나가야 한다 — 옮기지 못한 화면이 표를 쓰기 시작할 때
     * 처음 만나는 것이 이 조합이다.
     */
    @Test
    fun `모든 갈래와 문맥 조합이 해석 가능한 문구를 낸다`() {
        FailureKind.entries.forEach { kind ->
            FailureSurface.entries.forEach { surface ->
                val display = kind.display(surface)
                assertTrue("$kind($surface) 제목", display.title(resources).isNotBlank())
                assertTrue("$kind($surface) 본문", display.description(resources).isNotBlank())
                assertTrue("$kind($surface) 한 줄", display.sentence(resources).isNotBlank())
            }
        }
    }

    private data class SpecRow(
        val code: String,
        val failure: CoreDataFailure,
        val kind: FailureKind,
    )
}
