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
import com.cambridge.core.ui.component.CareerCompassScoreLevel
import com.cambridge.feature.feed.presentation.FeedFilterUiModel
import com.cambridge.feature.feed.presentation.FeedListingCategory
import com.cambridge.feature.feed.presentation.FeedListingUiModel
import com.cambridge.feature.feed.presentation.FeedSortUiModel
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

public fun Int?.toMinScoreFilter(): FeedMinScoreFilter =
    when (this) {
        null -> FeedMinScoreFilter.All
        MIN_SCORE_60 -> FeedMinScoreFilter.AtLeast60
        MIN_SCORE_70 -> FeedMinScoreFilter.AtLeast70
        MIN_SCORE_80 -> FeedMinScoreFilter.AtLeast80
        else -> throw IllegalArgumentException("minScore must be null or one of 60·70·80: $this")
    }

public fun FeedMinScoreFilter.toMinScore(): Int? =
    when (this) {
        FeedMinScoreFilter.All -> null
        FeedMinScoreFilter.AtLeast60 -> MIN_SCORE_60
        FeedMinScoreFilter.AtLeast70 -> MIN_SCORE_70
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

public fun Posting.toListingUiModel(
    resources: Resources,
    clock: Clock,
): FeedListingUiModel {
    val today = LocalDate.now(clock)
    return FeedListingUiModel(
        id = id.toString(),
        title = title,
        category = type.toListingCategory(),
        categoryLabel = resources.getString(type.toListingCategory().labelRes()),
        sourceLabel = board.name,
        suitabilityScore = score,
        deadlineLabel = deadlineLabel(resources, dueDate, today),
        isDeadlineUrgent = isDeadlineUrgent(dueDate, today),
        isNew = isCollectedToday(clock),
        isBookmarked = isBookmarked,
    )
}

// ---- 공고 상세 ----

public fun PostingDetail.toDetailUiModel(
    resources: Resources,
    clock: Clock,
    suitability: PostingSuitabilityState,
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
                .map { it.toListingUiModel(resources, clock) },
        canCreateDraft = type.supportsApplicationDraft,
    )
}

public fun Suitability.toSuitabilityUiModel(resources: Resources): SuitabilityUiModel =
    SuitabilityUiModel(
        score = score,
        levelLabel = resources.getString(label.labelRes()),
        level = label.toScoreLevel(),
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

/** 점수 해석 레이블(F3-2) → 칩 강조. 「보통」·「낮음」은 같은 낮은 강조다. */
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

/** 서버 `status` 와 `isActive` 를 화면 상태 하나로 접는다. 모르는 상태는 활성 여부로만 판단한다. */
public fun Board.toUiBoardStatus(): BoardStatus =
    when (status) {
        DomainBoardStatus.Failed -> BoardStatus.Failing
        DomainBoardStatus.Paused -> BoardStatus.Paused
        DomainBoardStatus.Active -> if (isActive) BoardStatus.Active else BoardStatus.Paused
        DomainBoardStatus.Unknown -> if (isActive) BoardStatus.Active else BoardStatus.Paused
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

private const val MIN_SCORE_60 = 60
private const val MIN_SCORE_70 = 70
private const val MIN_SCORE_80 = 80

/** 마감 임박 판정 일수(당일 포함). */
public const val URGENT_DEADLINE_DAYS: Long = 3L

private val DETAIL_DATE_FORMAT: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy.MM.dd")
