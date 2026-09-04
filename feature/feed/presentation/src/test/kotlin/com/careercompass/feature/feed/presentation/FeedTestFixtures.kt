package com.careercompass.feature.feed.presentation

import com.careercompass.core.common.reporting.ErrorReporter
import com.careercompass.core.model.board.Board
import com.careercompass.core.model.board.BoardType
import com.careercompass.core.model.posting.Posting
import com.careercompass.core.model.posting.PostingBoardRef
import com.careercompass.core.model.posting.PostingDetail
import com.careercompass.core.model.posting.PostingParsed
import com.careercompass.core.model.posting.PostingType
import com.careercompass.core.model.posting.Suitability
import com.careercompass.core.model.posting.SuitabilityLabel
import com.careercompass.core.model.user.JobInterest
import com.careercompass.core.model.user.UserProfile
import com.careercompass.feature.feed.presentation.reporting.FEED_REPORT_KEY_STAGE
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.rules.TestWatcher
import org.junit.runner.Description
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import com.careercompass.core.model.board.BoardStatus as DomainBoardStatus

internal val SEOUL: ZoneId = ZoneId.of("Asia/Seoul")
internal val TODAY: LocalDate = LocalDate.of(2026, 9, 2)
internal val NOON_TODAY: Instant = TODAY.atTime(12, 0).atZone(SEOUL).toInstant()

/** 2026-09-02 12:00 KST 에 고정된 시계. */
internal val FIXED_CLOCK: Clock = Clock.fixed(NOON_TODAY, SEOUL)

/** `Dispatchers.Main` 을 테스트 디스패처로 바꾼다 — ViewModel 의 `viewModelScope` 가 즉시 실행된다. */
@OptIn(ExperimentalCoroutinesApi::class)
class MainDispatcherRule(
    val dispatcher: TestDispatcher = UnconfinedTestDispatcher(),
) : TestWatcher() {
    override fun starting(description: Description) {
        Dispatchers.setMain(dispatcher)
    }

    override fun finished(description: Description) {
        Dispatchers.resetMain()
    }
}

/** [ErrorReporter] 기록 fake — 단계 속성만 검증한다. */
internal class RecordingErrorReporter : ErrorReporter {
    val records = mutableListOf<Pair<Throwable, Map<String, String>>>()

    val stages: List<String> get() = records.mapNotNull { it.second[FEED_REPORT_KEY_STAGE] }

    override fun writeFailure(
        throwable: Throwable,
        attributes: Map<String, String>,
    ) {
        records += throwable to attributes
    }
}

internal fun posting(
    id: Long,
    title: String = "공고 $id",
    type: PostingType = PostingType.Recruit,
    boardId: Long = 1L,
    dueDate: LocalDate? = null,
    collectedAt: Instant = NOON_TODAY,
    score: Int? = null,
    isRead: Boolean = false,
    isBookmarked: Boolean = false,
): Posting =
    Posting(
        id = id,
        title = title,
        type = type,
        board = PostingBoardRef(id = boardId, name = "게시판 $boardId"),
        dueDate = dueDate,
        collectedAt = collectedAt,
        score = score,
        scoreLabel = score?.let(SuitabilityLabel::fromScore),
        isRead = isRead,
        isBookmarked = isBookmarked,
    )

internal fun postingDetail(
    id: Long,
    type: PostingType = PostingType.Recruit,
    isRead: Boolean = false,
    isBookmarked: Boolean = false,
    parsed: PostingParsed? = null,
    suitability: Suitability? = null,
    similar: List<Posting> = emptyList(),
    dueDate: LocalDate? = null,
    rawContent: String = "본문",
): PostingDetail =
    PostingDetail(
        id = id,
        title = "공고 $id",
        type = type,
        board = PostingBoardRef(id = 1L, name = "게시판 1"),
        url = "https://example.com/postings/$id",
        rawContent = rawContent,
        dueDate = dueDate,
        collectedAt = NOON_TODAY,
        isRead = isRead,
        isBookmarked = isBookmarked,
        parsed = parsed,
        suitability = suitability,
        similar = similar,
    )

internal fun board(
    id: Long,
    isActive: Boolean = true,
    status: DomainBoardStatus = DomainBoardStatus.Active,
    failCount: Int = 0,
    lastCollectedAt: Instant? = null,
    type: BoardType = BoardType.Scholarship,
    cycleHours: Int = 24,
): Board =
    Board(
        id = id,
        url = "https://example.com/boards/$id",
        name = "게시판 $id",
        type = type,
        cycleHours = cycleHours,
        isActive = isActive,
        status = status,
        failCount = failCount,
        lastCollectedAt = lastCollectedAt,
    )

internal fun profile(
    name: String? = "일혁",
    jobInterests: List<JobInterest> = listOf(JobInterest(code = "backend", priority = 1)),
    tags: List<String> = listOf("AI"),
): UserProfile =
    UserProfile(
        id = 1L,
        name = name,
        school = null,
        department = null,
        gpa = null,
        gradYear = null,
        jobInterests = jobInterests,
        tags = tags,
        onboardingDone = true,
        completion = 60,
    )
