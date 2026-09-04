package com.cambridge.feature.feed.presentation.shared.util

import android.content.res.Resources
import androidx.annotation.StringRes
import com.cambridge.core.model.board.Board
import com.cambridge.core.model.board.BoardDetection
import com.cambridge.core.model.board.BoardDetectionStatus
import com.cambridge.core.model.posting.Posting
import com.cambridge.core.model.posting.PostingDetail
import com.cambridge.core.model.posting.PostingSort
import com.cambridge.core.model.posting.PostingType
import com.cambridge.core.model.posting.Suitability
import com.cambridge.core.model.posting.SuitabilityAxisKind
import com.cambridge.core.model.posting.SuitabilityLabel
import com.cambridge.core.model.user.UserProfile
import com.cambridge.core.ui.component.CareerCompassScoreLevel
import com.cambridge.feature.feed.presentation.FeedFilterUiModel
import com.cambridge.feature.feed.presentation.FeedListingCategory
import com.cambridge.feature.feed.presentation.FeedListingUiModel
import com.cambridge.feature.feed.presentation.FeedSortUiModel
import com.cambridge.feature.feed.presentation.FeedSuitabilityState
import com.cambridge.feature.feed.presentation.R
import com.cambridge.feature.feed.presentation.board.BOARD_MAX_PREVIEW_COUNT
import com.cambridge.feature.feed.presentation.board.BoardCollectCycle
import com.cambridge.feature.feed.presentation.board.BoardDetectionFailure
import com.cambridge.feature.feed.presentation.board.BoardDetectionState
import com.cambridge.feature.feed.presentation.board.BoardPreviewItemUiModel
import com.cambridge.feature.feed.presentation.board.BoardStatus
import com.cambridge.feature.feed.presentation.board.BoardType
import com.cambridge.feature.feed.presentation.board.BoardUiModel
import com.cambridge.feature.feed.presentation.board.labelRes
import com.cambridge.feature.feed.presentation.feedfilter.FeedDeadlineFilter
import com.cambridge.feature.feed.presentation.feedfilter.FeedDeadlineRange
import com.cambridge.feature.feed.presentation.feedfilter.FeedMinScoreFilter
import com.cambridge.feature.feed.presentation.feedfilter.FeedSortOption
import com.cambridge.feature.feed.presentation.feedfilter.labelRes
import com.cambridge.feature.feed.presentation.postingdetail.POSTING_DETAIL_MAX_SIMILAR_POSTING_COUNT
import com.cambridge.feature.feed.presentation.postingdetail.PostingDetailUiModel
import com.cambridge.feature.feed.presentation.postingdetail.PostingFormQuestionUiModel
import com.cambridge.feature.feed.presentation.postingdetail.PostingSuitabilityState
import com.cambridge.feature.feed.presentation.postingdetail.SuitabilityAxisUiModel
import com.cambridge.feature.feed.presentation.postingdetail.SuitabilityUiModel
import com.cambridge.feature.feed.presentation.shared.model.SuitabilityJudgement
import com.cambridge.feature.feed.presentation.shared.model.judgeSuitability
import java.text.NumberFormat
import java.time.Clock
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import com.cambridge.core.model.board.BoardStatus as DomainBoardStatus
import com.cambridge.core.model.board.BoardType as DomainBoardType
import com.cambridge.feature.feed.domain.model.FeedDeadlineFilter as DomainDeadlineFilter

/*
 * 도메인 → 화면 계약 매핑. 전부 순수 함수다 — `@Composable` 이 아니고, 문구는 [Resources] 로만 만든다.
 * ViewModel 은 이 파일을 모른 채 도메인 값을 상태에 두고, Entry 가 여기서 UiModel 로 바꾼다.
 */

// ---- 공고 유형 ↔ 목록 카테고리 (기능 스펙 F2-3 「공고 유형」) ----

public fun PostingType.toListingCategory(): FeedListingCategory =
    when (this) {
        PostingType.Recruit -> FeedListingCategory.Employment
        PostingType.Scholarship -> FeedListingCategory.Scholarship
        PostingType.Contest -> FeedListingCategory.Contest
        PostingType.Activity -> FeedListingCategory.ExternalActivity
        PostingType.Other -> FeedListingCategory.Other
    }

/** 카테고리 칩 하나는 서버 `types` 파라미터 하나다. 「전체」는 빈 집합. */
public fun FeedListingCategory.toPostingTypes(): Set<PostingType> =
    when (this) {
        FeedListingCategory.All -> emptySet()
        FeedListingCategory.Employment -> setOf(PostingType.Recruit)
        FeedListingCategory.Scholarship -> setOf(PostingType.Scholarship)
        FeedListingCategory.Contest -> setOf(PostingType.Contest)
        FeedListingCategory.ExternalActivity -> setOf(PostingType.Activity)
        FeedListingCategory.Other -> setOf(PostingType.Other)
    }

/** 서버 `types` 집합을 칩으로 되돌린다 — 하나가 아니면 「전체」로 본다. */
public fun Set<PostingType>.toListingCategory(): FeedListingCategory = singleOrNull()?.toListingCategory() ?: FeedListingCategory.All

@StringRes
public fun FeedListingCategory.labelRes(): Int =
    when (this) {
        FeedListingCategory.All -> R.string.feed_category_all
        FeedListingCategory.Employment -> R.string.feed_category_employment
        FeedListingCategory.Scholarship -> R.string.feed_category_scholarship
        FeedListingCategory.Contest -> R.string.feed_category_contest
        FeedListingCategory.ExternalActivity -> R.string.feed_category_external_activity
        FeedListingCategory.Other -> R.string.feed_category_other
    }

public fun feedCategoryFilters(resources: Resources): List<FeedFilterUiModel> =
    FeedListingCategory.entries.map { category ->
        FeedFilterUiModel(category = category, label = resources.getString(category.labelRes()))
    }

// ---- 정렬·필터 값 ↔ 도메인 ----

public fun PostingSort.toSortOption(): FeedSortOption =
    when (this) {
        PostingSort.CollectedDesc -> FeedSortOption.CollectedDesc
        PostingSort.DueAsc -> FeedSortOption.DueAsc
        PostingSort.ScoreDesc -> FeedSortOption.ScoreDesc
    }

public fun FeedSortOption.toPostingSort(): PostingSort =
    when (this) {
        FeedSortOption.CollectedDesc -> PostingSort.CollectedDesc
        FeedSortOption.DueAsc -> PostingSort.DueAsc
        FeedSortOption.ScoreDesc -> PostingSort.ScoreDesc
    }

public fun FeedSortOption.toSortUiModel(resources: Resources): FeedSortUiModel =
    FeedSortUiModel(id = name, label = resources.getString(labelRes()))

public fun DomainDeadlineFilter.toUiDeadlineFilter(): FeedDeadlineFilter =
    when (this) {
        DomainDeadlineFilter.All -> FeedDeadlineFilter.All
        DomainDeadlineFilter.WithinWeek -> FeedDeadlineFilter.WithinWeek
        DomainDeadlineFilter.WithinMonth -> FeedDeadlineFilter.WithinMonth
        DomainDeadlineFilter.IncludeExpired -> FeedDeadlineFilter.IncludeExpired
        is DomainDeadlineFilter.Range -> FeedDeadlineFilter.Range
    }

/** 조회에 걸린 범위 → 시트 편집값. 프리셋이면 빈 범위에서 시작한다. */
public fun DomainDeadlineFilter.toUiDeadlineRange(): FeedDeadlineRange =
    when (this) {
        is DomainDeadlineFilter.Range -> FeedDeadlineRange(start = start, end = end)
        else -> FeedDeadlineRange()
    }

/**
 * 시트 선택 → 도메인 필터. 잘못된 범위([FeedDeadlineRange.error])는 **null** 이다 — 도메인 값이 뒤집힌
 * 범위를 만들 수 없으므로, 옮길 수 없다는 사실을 타입으로 돌려주고 호출부가 「적용」을 막는다.
 */
public fun FeedDeadlineFilter.toDomainDeadlineFilter(range: FeedDeadlineRange): DomainDeadlineFilter? =
    when (this) {
        FeedDeadlineFilter.All -> DomainDeadlineFilter.All
        FeedDeadlineFilter.WithinWeek -> DomainDeadlineFilter.WithinWeek
        FeedDeadlineFilter.WithinMonth -> DomainDeadlineFilter.WithinMonth
        FeedDeadlineFilter.IncludeExpired -> DomainDeadlineFilter.IncludeExpired
        FeedDeadlineFilter.Range -> range.toDomainRange()
    }

private fun FeedDeadlineRange.toDomainRange(): DomainDeadlineFilter.Range? =
    if (error == null) DomainDeadlineFilter.Range(start = start, end = end) else null

/**
 * 조회에 걸린 하한 → 시트 선택지.
 *
 * 선택지에 없는 값은 **「전체」로 접는다.** 예전 선택지였던 70 이 그렇다(이슈 #200) — 시트가 그릴 수 없는
 * 값을 만나면 예외로 화면을 죽이는 대신, 걸린 조건이 없다고 말한다. `FeedQuery` 가 이미 그 값을 거절하므로
 * 여기까지 오는 경로는 남아 있지 않지만, 매핑 하나가 앱을 죽일 수 있는 자리로 남지 않게 한다.
 */
public fun Int?.toMinScoreFilter(): FeedMinScoreFilter =
    when (this) {
        MIN_SCORE_60 -> FeedMinScoreFilter.AtLeast60
        MIN_SCORE_80 -> FeedMinScoreFilter.AtLeast80
        else -> FeedMinScoreFilter.All
    }

public fun FeedMinScoreFilter.toMinScore(): Int? =
    when (this) {
        FeedMinScoreFilter.All -> null
        FeedMinScoreFilter.AtLeast60 -> MIN_SCORE_60
        FeedMinScoreFilter.AtLeast80 -> MIN_SCORE_80
    }

// ---- 마감·신규 표기 ----

/** 「D-7」·「오늘 마감」·「마감」·「마감 미정」. */
public fun deadlineLabel(
    resources: Resources,
    dueDate: LocalDate?,
    today: LocalDate,
): String {
    val days = daysUntil(dueDate, today) ?: return resources.getString(R.string.feed_deadline_none)
    return when {
        days < 0 -> resources.getString(R.string.feed_deadline_expired)
        days == 0L -> resources.getString(R.string.feed_deadline_today)
        else -> resources.getString(R.string.feed_deadline_days, days)
    }
}

/** 마감까지 [URGENT_DEADLINE_DAYS]일 이내(당일 포함)면 임박 — 마감 알림 D-3 기준(기능 스펙 F2-4)과 맞춘다. */
public fun isDeadlineUrgent(
    dueDate: LocalDate?,
    today: LocalDate,
): Boolean = daysUntil(dueDate, today)?.let { it in 0..URGENT_DEADLINE_DAYS } == true

private fun daysUntil(
    dueDate: LocalDate?,
    today: LocalDate,
): Long? = dueDate?.let { ChronoUnit.DAYS.between(today, it) }

/** 「신규」 = 시계의 시간대 기준 오늘 수집. */
public fun Posting.isCollectedToday(clock: Clock): Boolean = collectedAt.atZone(clock.zone).toLocalDate() == LocalDate.now(clock)

/**
 * 카드의 수집일 한 줄 — 「오늘 수집」·「수집 3일 전」(기능 스펙 F2-3 「카드 정보」).
 *
 * 오늘 것만 상대 시각을 쓰지 않는 이유는 초록 점과 **어긋나지 않게** 하려는 것이다. 점은 시간대 기준
 * 날짜([isCollectedToday])로 켜지는데 상대 시각은 흐른 시간으로 세므로, 새벽 1시에 들어온 공고를
 * 밤 11시에 보면 점은 켜져 있는데 문구는 「수집 22시간 전」이라 둘이 다른 말을 한다.
 *
 * 오늘이 아니면 상세 화면과 같은 [RelativeTimeFormatter] 를 쓴다 — 같은 공고를 목록과 상세에서
 * 다른 말로 부르지 않는다.
 */
public fun Posting.collectedAtLabel(
    resources: Resources,
    clock: Clock,
): String =
    if (isCollectedToday(clock)) {
        resources.getString(R.string.feed_collected_today)
    } else {
        resources.getString(
            R.string.feed_collected_at,
            RelativeTimeFormatter.format(resources, collectedAt, clock),
        )
    }

/**
 * 카드의 점수 자리 — 판정([judgeSuitability])을 화면 계약으로 옮긴다.
 *
 * 「준비됨」인데 점수가 없는 모순은 「분석 중」으로 접는다(상세 화면과 같은 처분).
 */
public fun Posting.toSuitabilityState(profile: UserProfile?): FeedSuitabilityState =
    when (judgeSuitability(hasScore = score != null, profile = profile)) {
        SuitabilityJudgement.ProfileIncomplete -> FeedSuitabilityState.ProfileIncomplete
        SuitabilityJudgement.Analyzing -> FeedSuitabilityState.Analyzing
        SuitabilityJudgement.Ready -> score?.let(FeedSuitabilityState::Scored) ?: FeedSuitabilityState.Analyzing
    }

public fun Posting.toListingUiModel(
    resources: Resources,
    clock: Clock,
    profile: UserProfile?,
): FeedListingUiModel {
    val today = LocalDate.now(clock)
    return FeedListingUiModel(
        id = id.toString(),
        title = title,
        category = type.toListingCategory(),
        categoryLabel = resources.getString(type.toListingCategory().labelRes()),
        sourceLabel = board.name,
        suitability = toSuitabilityState(profile),
        deadlineLabel = deadlineLabel(resources, dueDate, today),
        isDeadlineUrgent = isDeadlineUrgent(dueDate, today),
        collectedAtLabel = collectedAtLabel(resources, clock),
        isNew = isCollectedToday(clock),
        isRead = isRead,
        isBookmarked = isBookmarked,
    )
}

// ---- 공고 상세 ----

public fun PostingDetail.toDetailUiModel(
    resources: Resources,
    clock: Clock,
    suitability: PostingSuitabilityState,
    profile: UserProfile?,
): PostingDetailUiModel {
    val today = LocalDate.now(clock)
    val category = type.toListingCategory()
    val qualifications =
        listOfNotNull(
            parsed?.qualifications?.year?.let { resources.getString(R.string.feed_posting_detail_qualification_year, it) },
            parsed?.qualifications?.gpa?.let { resources.getString(R.string.feed_posting_detail_qualification_gpa, it) },
        )
    return PostingDetailUiModel(
        id = id.toString(),
        title = title,
        category = category,
        categoryLabel = resources.getString(category.labelRes()),
        sourceLabel = board.name,
        collectedAtLabel = RelativeTimeFormatter.format(resources, collectedAt, clock),
        deadlineLabel = dueDate?.format(DETAIL_DATE_FORMAT) ?: resources.getString(R.string.feed_posting_detail_deadline_none),
        isDeadlineUrgent = isDeadlineUrgent(dueDate, today),
        isBookmarked = isBookmarked,
        suitability = suitability,
        keywords = parsed?.keywords?.distinct().orEmpty(),
        qualifications = qualifications,
        preferences = parsed?.preferences.orEmpty(),
        formQuestions =
            parsed?.formQuestions.orEmpty().map { question ->
                PostingFormQuestionUiModel(
                    order = question.order,
                    question = question.question,
                    maxCharsLabel =
                        question.maxChars?.let { maxChars ->
                            resources.getString(R.string.feed_posting_detail_max_chars, NumberFormat.getIntegerInstance().format(maxChars))
                        },
                )
            },
        similarPostings =
            similar
                .distinctBy(Posting::id)
                .take(POSTING_DETAIL_MAX_SIMILAR_POSTING_COUNT)
                .map { it.toListingUiModel(resources, clock, profile) },
        canCreateDraft = type.supportsApplicationDraft,
    )
}

public fun Suitability.toSuitabilityUiModel(resources: Resources): SuitabilityUiModel =
    SuitabilityUiModel(
        score = score,
        levelLabel = resources.getString(label.labelRes()),
        level = label,
        breakdown =
            breakdown.map { axis ->
                SuitabilityAxisUiModel(
                    label = resources.getString(axis.kind.labelRes()),
                    score = axis.score,
                    weightLabel = resources.getString(R.string.feed_posting_detail_axis_weight, axis.weight),
                )
            },
        strengthComment = strengthComment,
        weaknessComment = weaknessComment,
    )

@StringRes
public fun SuitabilityLabel.labelRes(): Int =
    when (this) {
        SuitabilityLabel.VerySuitable -> R.string.feed_posting_detail_level_very_suitable
        SuitabilityLabel.Suitable -> R.string.feed_posting_detail_level_suitable
        SuitabilityLabel.Neutral -> R.string.feed_posting_detail_level_neutral
        SuitabilityLabel.Low -> R.string.feed_posting_detail_level_low
    }

/**
 * 점수 해석 레이블(F3-2) → 목록 카드 점수 칩의 강조 3단계. 「보통」·「낮음」은 같은 낮은 강조다.
 *
 * 칩에는 레이블 글자가 없고 숫자만 실리므로 네 단계를 다 그릴 자리가 없다. 다만 **강조가 갈리는 지점은
 * 전부 레이블 경계**(80·60)다 — 공고 상세의 게이지가 네 구간으로 더 잘게 나뉘어도 두 화면이 같은 눈금
 * 위에 있는 이유다(이슈 #200).
 */
public fun SuitabilityLabel.toScoreLevel(): CareerCompassScoreLevel =
    when (this) {
        SuitabilityLabel.VerySuitable -> CareerCompassScoreLevel.High

        SuitabilityLabel.Suitable -> CareerCompassScoreLevel.Mid

        SuitabilityLabel.Neutral,
        SuitabilityLabel.Low,
        -> CareerCompassScoreLevel.Low
    }

@StringRes
public fun SuitabilityAxisKind.labelRes(): Int =
    when (this) {
        SuitabilityAxisKind.FieldSimilarity -> R.string.feed_posting_detail_axis_field_similarity
        SuitabilityAxisKind.Qualification -> R.string.feed_posting_detail_axis_qualification
        SuitabilityAxisKind.Preference -> R.string.feed_posting_detail_axis_preference
        SuitabilityAxisKind.Competition -> R.string.feed_posting_detail_axis_competition
    }

// ---- 게시판 ----

public fun DomainBoardType.toUiBoardType(): BoardType =
    when (this) {
        DomainBoardType.Scholarship -> BoardType.Scholarship
        DomainBoardType.Recruit -> BoardType.Employment
        DomainBoardType.Contest -> BoardType.Contest
        DomainBoardType.Activity -> BoardType.ExternalActivity
        DomainBoardType.Other -> BoardType.Other
    }

public fun BoardType.toDomainBoardType(): DomainBoardType =
    when (this) {
        BoardType.Scholarship -> DomainBoardType.Scholarship
        BoardType.Employment -> DomainBoardType.Recruit
        BoardType.Contest -> DomainBoardType.Contest
        BoardType.ExternalActivity -> DomainBoardType.Activity
        BoardType.Other -> DomainBoardType.Other
    }

/**
 * 서버 `status` 와 `isActive` 를 화면 상태 하나로 접는다 — 이 판정이 사는 유일한 곳이다.
 *
 * `Failed` 이면서 꺼져 있으면 연속 수집 실패로 **서버가 끈** 게시판이다(기능 스펙 F2-2). 사용자가 끈
 * [BoardStatus.Paused] 와 같은 그림이 되지 않게 [BoardStatus.Deactivated] 로 가른다. 같은 `Failed` 라도
 * 켜져 있으면 아직 수집을 시도하는 중이므로 [BoardStatus.Failing] 이다.
 *
 * 모르는 상태는 예전대로 활성 여부로만 판단한다 — 서버가 `status` 를 늘려도 목록이 깨지지 않는다.
 */
public fun Board.toUiBoardStatus(): BoardStatus =
    when (status) {
        DomainBoardStatus.Failed -> if (isActive) BoardStatus.Failing else BoardStatus.Deactivated

        DomainBoardStatus.Paused -> BoardStatus.Paused

        DomainBoardStatus.Active,
        DomainBoardStatus.Unknown,
        -> if (isActive) BoardStatus.Active else BoardStatus.Paused
    }

/** 주기(시간) → 선택지. 선택지에 없는 값은 기본 1일 1회로 보인다. */
public fun Int.toCollectCycle(): BoardCollectCycle = BoardCollectCycle.entries.firstOrNull { it.hours == this } ?: BoardCollectCycle.Daily

public fun Board.toBoardUiModel(
    resources: Resources,
    clock: Clock,
): BoardUiModel =
    BoardUiModel(
        id = id.toString(),
        name = name,
        url = url,
        type = type.toUiBoardType(),
        typeLabel = resources.getString(type.toUiBoardType().labelRes()),
        status = toUiBoardStatus(),
        isActive = isActive,
        failCount = failCount,
        lastCollectedLabel = lastCollectedAt?.let { RelativeTimeFormatter.format(resources, it, clock) },
        postingCount = null,
    )

/** 감지 결과 → 등록 화면 상태. 미리보기는 계약 상한(5건)까지만 싣고 날짜는 ISO 로 적는다. */
public fun BoardDetection.toDetectionState(): BoardDetectionState =
    when (status) {
        BoardDetectionStatus.Success -> {
            BoardDetectionState.Success(
                preview =
                    preview.take(BOARD_MAX_PREVIEW_COUNT).map { item ->
                        BoardPreviewItemUiModel(
                            title = item.title,
                            url = item.url,
                            dateLabel = item.date?.toString(),
                        )
                    },
                dateDetected = hasDateSelector,
            )
        }

        BoardDetectionStatus.LoginRequired -> {
            BoardDetectionState.Failed(BoardDetectionFailure.LoginRequired)
        }

        BoardDetectionStatus.Spa -> {
            BoardDetectionState.Failed(BoardDetectionFailure.Spa)
        }

        BoardDetectionStatus.Blocked -> {
            BoardDetectionState.Failed(BoardDetectionFailure.Blocked)
        }

        BoardDetectionStatus.Failed -> {
            BoardDetectionState.Failed(BoardDetectionFailure.Failed)
        }
    }

// 선택지 값은 F3-2 레이블 경계다 — 도메인의 `SuitabilityLabel.minScore` 와 같은 수여야 한다
// (`FeedUiMappersTest` 가 고정한다).
private val MIN_SCORE_60 = SuitabilityLabel.Suitable.minScore
private val MIN_SCORE_80 = SuitabilityLabel.VerySuitable.minScore

/** 마감 임박 판정 일수(당일 포함). */
public const val URGENT_DEADLINE_DAYS: Long = 3L

private val DETAIL_DATE_FORMAT: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy.MM.dd")
