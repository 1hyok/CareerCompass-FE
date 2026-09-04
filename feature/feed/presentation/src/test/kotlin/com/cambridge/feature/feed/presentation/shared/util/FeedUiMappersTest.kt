package com.cambridge.feature.feed.presentation.shared.util

import android.content.res.Resources
import com.cambridge.core.model.board.BoardDetection
import com.cambridge.core.model.board.BoardDetectionStatus
import com.cambridge.core.model.board.BoardPreviewItem
import com.cambridge.core.model.posting.PostingFormQuestion
import com.cambridge.core.model.posting.PostingParsed
import com.cambridge.core.model.posting.PostingQualifications
import com.cambridge.core.model.posting.PostingType
import com.cambridge.core.model.posting.Suitability
import com.cambridge.core.model.posting.SuitabilityAxis
import com.cambridge.core.model.posting.SuitabilityAxisKind
import com.cambridge.core.model.posting.SuitabilityLabel
import com.cambridge.feature.feed.presentation.FIXED_CLOCK
import com.cambridge.feature.feed.presentation.FeedListingCategory
import com.cambridge.feature.feed.presentation.FeedSuitabilityState
import com.cambridge.feature.feed.presentation.NOON_TODAY
import com.cambridge.feature.feed.presentation.SEOUL
import com.cambridge.feature.feed.presentation.TODAY
import com.cambridge.feature.feed.presentation.board
import com.cambridge.feature.feed.presentation.board.BoardDetectionState
import com.cambridge.feature.feed.presentation.board.BoardStatus
import com.cambridge.feature.feed.presentation.board.BoardType
import com.cambridge.feature.feed.presentation.posting
import com.cambridge.feature.feed.presentation.postingDetail
import com.cambridge.feature.feed.presentation.postingdetail.PostingSuitabilityState
import com.cambridge.feature.feed.presentation.profile
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import com.cambridge.core.model.board.BoardStatus as DomainBoardStatus
import com.cambridge.core.model.board.BoardType as DomainBoardType

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class FeedUiMappersTest {
    private val resources: Resources = RuntimeEnvironment.getApplication().resources

    @Test
    fun `D-day 는 남은 일수·오늘·지남·미정을 구분한다`() {
        assertEquals("D-7", deadlineLabel(resources, TODAY.plusDays(7), TODAY))
        assertEquals("오늘 마감", deadlineLabel(resources, TODAY, TODAY))
        assertEquals("마감", deadlineLabel(resources, TODAY.minusDays(1), TODAY))
        assertEquals("마감 미정", deadlineLabel(resources, null, TODAY))
    }

    @Test
    fun `마감 임박은 오늘부터 3일 이내다`() {
        assertTrue(isDeadlineUrgent(TODAY, TODAY))
        assertTrue(isDeadlineUrgent(TODAY.plusDays(3), TODAY))
        assertFalse(isDeadlineUrgent(TODAY.plusDays(4), TODAY))
        assertFalse(isDeadlineUrgent(TODAY.minusDays(1), TODAY))
        assertFalse(isDeadlineUrgent(null, TODAY))
    }

    @Test
    fun `신규는 시계의 시간대 기준 오늘 수집이다`() {
        assertTrue(posting(id = 1, collectedAt = NOON_TODAY).isCollectedToday(FIXED_CLOCK))
        // 09-02 01:30 KST — UTC 로는 어제지만 서울 기준 오늘이다.
        assertTrue(posting(id = 2, collectedAt = NOON_TODAY.minus(Duration.ofHours(10).plusMinutes(30))).isCollectedToday(FIXED_CLOCK))
        assertFalse(posting(id = 3, collectedAt = NOON_TODAY.minus(Duration.ofDays(1))).isCollectedToday(FIXED_CLOCK))
    }

    @Test
    fun `목록 카드는 라벨·출처·D-day·신규·점수 없음을 옮긴다`() {
        val listing =
            posting(id = 5, title = "카카오 인턴", type = PostingType.Contest, dueDate = TODAY.plusDays(2), score = null)
                .toListingUiModel(resources, FIXED_CLOCK, profile())

        assertEquals("5", listing.id)
        assertEquals(FeedListingCategory.Contest, listing.category)
        assertEquals("공모전", listing.categoryLabel)
        assertEquals("게시판 1", listing.sourceLabel)
        assertEquals(FeedSuitabilityState.Analyzing, listing.suitability)
        assertEquals("D-2", listing.deadlineLabel)
        assertTrue(listing.isDeadlineUrgent)
        assertTrue(listing.isNew)
        assertEquals("오늘 수집", listing.collectedAtLabel)
        assertFalse(listing.isRead)
    }

    @Test
    fun `수집일 문구는 오늘 것과 지난 것을 가른다`() {
        assertEquals("오늘 수집", posting(id = 1, collectedAt = NOON_TODAY).collectedAtLabel(resources, FIXED_CLOCK))
        assertEquals(
            "수집 3일 전",
            posting(id = 2, collectedAt = NOON_TODAY.minus(Duration.ofDays(3))).collectedAtLabel(resources, FIXED_CLOCK),
        )
        // 어제 밤 11시 수집분을 오늘 새벽 1시에 본다 — 날짜가 바뀌었으므로 상대 시각으로 말한다.
        val afterMidnight = Clock.fixed(seoulInstant(TODAY, hour = 1), SEOUL)
        val lastNight = posting(id = 3, collectedAt = seoulInstant(TODAY.minusDays(1), hour = 23))
        assertEquals("수집 2시간 전", lastNight.collectedAtLabel(resources, afterMidnight))
    }

    /**
     * 오늘 것만 상대 시각을 쓰지 않는 이유 — 새벽에 들어온 공고를 밤에 보면 「22시간 전」이 되어
     * 초록 점(오늘 수집)과 문구가 다른 말을 한다. 날짜로 말하면 둘이 어긋날 수 없다.
     */
    @Test
    fun `오늘 새벽 수집분도 밤에 오늘 수집으로 읽힌다`() {
        val lateNight = Clock.fixed(seoulInstant(TODAY, hour = 23), SEOUL)
        val dawn = posting(id = 1, collectedAt = seoulInstant(TODAY, hour = 1))

        assertTrue(dawn.isCollectedToday(lateNight))
        assertEquals("오늘 수집", dawn.collectedAtLabel(resources, lateNight))
    }

    /**
     * 읽음은 [com.cambridge.core.model.posting.Posting] 하나에서만 온다 — 오프라인 스냅샷으로 복원한
     * 공고도 같은 매퍼를 지나므로 온라인 목록과 같은 카드가 된다(스냅샷 DTO 가 두 값을 그대로 싣는다).
     */
    @Test
    fun `읽음은 도메인 값 그대로 카드에 실린다`() {
        assertTrue(posting(id = 1, isRead = true).toListingUiModel(resources, FIXED_CLOCK, profile()).isRead)
        assertFalse(posting(id = 2, isRead = false).toListingUiModel(resources, FIXED_CLOCK, profile()).isRead)
    }

    @Test
    fun `점수 자리는 점수·프로필 미입력·분석 중을 가른다`() {
        val emptyProfile = profile(jobInterests = emptyList(), tags = emptyList())

        assertEquals(FeedSuitabilityState.Scored(88), posting(id = 1, score = 88).toSuitabilityState(emptyProfile))
        assertEquals(FeedSuitabilityState.ProfileIncomplete, posting(id = 2).toSuitabilityState(emptyProfile))
        assertEquals(FeedSuitabilityState.Analyzing, posting(id = 3).toSuitabilityState(profile()))
        assertEquals(FeedSuitabilityState.Analyzing, posting(id = 4).toSuitabilityState(null))
    }

    @Test
    fun `공고 유형과 카테고리는 서로 되돌릴 수 있고 전체는 빈 집합이다`() {
        PostingType.entries.forEach { type ->
            assertEquals(setOf(type), type.toListingCategory().toPostingTypes())
        }
        assertEquals(emptySet<PostingType>(), FeedListingCategory.All.toPostingTypes())
        assertEquals(FeedListingCategory.All, emptySet<PostingType>().toListingCategory())
        assertEquals(FeedListingCategory.All, setOf(PostingType.Recruit, PostingType.Contest).toListingCategory())
        assertEquals(FeedListingCategory.Other, setOf(PostingType.Other).toListingCategory())
        assertEquals(FeedListingCategory.entries.size, feedCategoryFilters(resources).size)
    }

    @Test
    fun `상세는 파싱 결과·유사 공고 상한·초안 가능 여부를 옮긴다`() {
        val detail =
            postingDetail(
                id = 7,
                type = PostingType.Scholarship,
                dueDate = TODAY.plusDays(10),
                parsed =
                    PostingParsed(
                        keywords = listOf("Kotlin", "Kotlin", "Spring"),
                        qualifications = PostingQualifications(year = "3학년 이상", gpa = "3.0 이상"),
                        preferences = listOf("Spring 경험"),
                        formQuestions = listOf(PostingFormQuestion(order = 1, question = "지원 동기", maxChars = 1000)),
                    ),
                suitability =
                    Suitability(
                        score = 88,
                        label = SuitabilityLabel.VerySuitable,
                        breakdown = listOf(SuitabilityAxis(kind = SuitabilityAxisKind.FieldSimilarity, score = 95, weight = 40)),
                        strengthComment = "강점",
                        weaknessComment = null,
                    ),
                similar = List(5) { posting(id = 100L + it, score = 70) },
            )

        val ready = PostingSuitabilityState.Ready(requireNotNull(detail.suitability).toSuitabilityUiModel(resources))
        val model = detail.toDetailUiModel(resources, FIXED_CLOCK, ready, profile())

        assertEquals("7", model.id)
        assertEquals(FeedListingCategory.Scholarship, model.category)
        assertEquals("장학금", model.categoryLabel)
        assertEquals("방금", model.collectedAtLabel)
        assertEquals(
            TODAY.plusDays(10).format(
                java.time.format.DateTimeFormatter
                    .ofPattern("yyyy.MM.dd"),
            ),
            model.deadlineLabel,
        )
        assertFalse(model.isDeadlineUrgent)
        assertEquals(listOf("Kotlin", "Spring"), model.keywords)
        assertEquals(listOf("학년 3학년 이상", "학점 3.0 이상"), model.qualifications)
        assertEquals("최대 1,000자", model.formQuestions.single().maxCharsLabel)
        assertEquals(3, model.similarPostings.size)
        assertTrue(model.canCreateDraft)
        val suitability = (model.suitability as PostingSuitabilityState.Ready).suitability
        assertEquals("매우 적합", suitability.levelLabel)
        assertEquals(SuitabilityLabel.VerySuitable, suitability.level)
        assertEquals("분야 유사도", suitability.breakdown.single().label)
        assertEquals("40%", suitability.breakdown.single().weightLabel)
    }

    @Test
    fun `마감일 없는 상세는 미정으로, 공모전은 초안 불가로 옮긴다`() {
        val model =
            postingDetail(
                id = 8,
                type = PostingType.Contest,
            ).toDetailUiModel(resources, FIXED_CLOCK, PostingSuitabilityState.Analyzing(isAutoRecheckExhausted = false), profile())

        assertEquals("미정", model.deadlineLabel)
        assertFalse(model.canCreateDraft)
        assertTrue(model.keywords.isEmpty())
    }

    @Test
    fun `게시판은 상태·유형·상대 시각을 옮기고 공고 수는 모른다`() {
        val failing =
            board(
                id = 3,
                isActive = true,
                status = DomainBoardStatus.Failed,
                failCount = 2,
                lastCollectedAt = NOON_TODAY.minus(Duration.ofHours(2)),
                type = DomainBoardType.Recruit,
            ).toBoardUiModel(resources, FIXED_CLOCK)

        assertEquals("3", failing.id)
        assertEquals(BoardType.Employment, failing.type)
        assertEquals("채용", failing.typeLabel)
        assertEquals(BoardStatus.Failing, failing.status)
        assertEquals(2, failing.failCount)
        assertEquals("2시간 전", failing.lastCollectedLabel)
        assertNull(failing.postingCount)

        val paused = board(id = 4, isActive = false, status = DomainBoardStatus.Unknown).toBoardUiModel(resources, FIXED_CLOCK)
        assertEquals(BoardStatus.Paused, paused.status)
        assertNull(paused.lastCollectedLabel)
    }

    @Test
    fun `연속 실패로 서버가 끈 게시판만 중단 상태이고 사용자가 끈 것은 일시중지다`() {
        // 기능 스펙 F2-2 「3회 연속 실패 시 비활성화」 — 판정은 isActive == false && status == Failed 하나뿐이다.
        val deactivated = board(id = 5, isActive = false, status = DomainBoardStatus.Failed, failCount = 3)
        assertEquals(BoardStatus.Deactivated, deactivated.toUiBoardStatus())
        assertEquals(3, deactivated.toBoardUiModel(resources, FIXED_CLOCK).failCount)

        // 같은 Failed 라도 켜져 있으면 아직 수집을 시도하는 중이다.
        val stillTrying = board(id = 6, isActive = true, status = DomainBoardStatus.Failed, failCount = 2)
        assertEquals(BoardStatus.Failing, stillTrying.toUiBoardStatus())

        // 사용자가 끈 게시판은 실패한 적이 없어도 꺼져 있다 — 중단 안내를 붙이면 안 된다.
        assertEquals(BoardStatus.Paused, board(id = 7, isActive = false, status = DomainBoardStatus.Paused).toUiBoardStatus())
        assertEquals(BoardStatus.Paused, board(id = 8, isActive = false, status = DomainBoardStatus.Active).toUiBoardStatus())

        // 모르는 상태는 예전대로 활성 여부로만 접힌다.
        assertEquals(BoardStatus.Active, board(id = 9, isActive = true, status = DomainBoardStatus.Unknown).toUiBoardStatus())
        assertEquals(BoardStatus.Paused, board(id = 10, isActive = false, status = DomainBoardStatus.Unknown).toUiBoardStatus())
    }

    @Test
    fun `감지 결과는 미리보기를 5건까지만 싣고 실패 사유를 옮긴다`() {
        val success =
            BoardDetection(
                status = BoardDetectionStatus.Success,
                preview = List(7) { BoardPreviewItem(title = "글 $it", url = "https://example.com/$it", date = TODAY) },
                hasDateSelector = false,
            ).toDetectionState() as BoardDetectionState.Success

        assertEquals(5, success.preview.size)
        assertEquals("2026-09-02", success.preview.first().dateLabel)
        assertFalse(success.dateDetected)

        val blocked =
            BoardDetection(
                status = BoardDetectionStatus.Blocked,
                preview = emptyList(),
                hasDateSelector = false,
            ).toDetectionState()
        assertEquals(BoardDetectionState.Failed(com.cambridge.feature.feed.presentation.board.BoardDetectionFailure.Blocked), blocked)
    }

    @Test
    fun `점수 하한과 주기는 선택지로 되돌린다`() {
        assertEquals(
            80,
            com.cambridge.feature.feed.presentation.feedfilter.FeedMinScoreFilter.AtLeast80
                .toMinScore(),
        )
        assertEquals(com.cambridge.feature.feed.presentation.feedfilter.FeedMinScoreFilter.All, (null as Int?).toMinScoreFilter())
        // 없어진 선택지 70 은 예외가 아니라 「전체」로 접힌다 — 매핑 하나가 화면을 죽이지 않게(이슈 #200).
        assertEquals(com.cambridge.feature.feed.presentation.feedfilter.FeedMinScoreFilter.All, 70.toMinScoreFilter())
        assertEquals(com.cambridge.feature.feed.presentation.board.BoardCollectCycle.Weekly, 168.toCollectCycle())
        assertEquals(com.cambridge.feature.feed.presentation.board.BoardCollectCycle.Daily, 7.toCollectCycle())
    }
}

private fun seoulInstant(
    date: LocalDate,
    hour: Int,
): Instant = date.atTime(hour, 0).atZone(SEOUL).toInstant()
