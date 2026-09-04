package com.cambridge.feature.feed.presentation.postingdetail

import com.cambridge.core.model.posting.SuitabilityLabel
import com.cambridge.feature.feed.presentation.FeedListingCategory
import com.cambridge.feature.feed.presentation.FeedListingUiModel

/** Number of analysis axes the suitability breakdown renders (spec F3-2). */
public const val POSTING_DETAIL_MAX_BREAKDOWN_COUNT: Int = 4

/** Maximum number of similar postings recommended on the detail screen (spec F3-3). */
public const val POSTING_DETAIL_MAX_SIMILAR_POSTING_COUNT: Int = 3

/**
 * 축 하나가 충족인가 미충족인가 — 기능 스펙 F3-3 「4개 분석 축 각각의 세부 점수 및 충족/미충족 표시」.
 *
 * 서버 계약에는 없는 클라이언트 파생값이다 — `API_SPEC §5` 의 breakdown 은 `{axis, score, weight}` 뿐이고
 * [com.cambridge.core.model.posting.SuitabilityAxis] 도 같다.
 */
public enum class SuitabilityAxisFulfillment {
    /** 경계값 이상 — 이 축은 공고가 요구하는 선을 넘었다. */
    Fulfilled,

    /** 경계값 미만. 「점수를 모른다」 는 이 값이 아니다 — [SuitabilityUiModel.breakdown] 에 행이 없는 것이 그것이다. */
    Unfulfilled,
}

/**
 * 축이 「충족」 으로 넘어가는 경계 — **60점**.
 *
 * F3-2 가 총점 60점부터 「적합」 이라고 부르는 그 경계를 그대로 쓴다. 막대 색을 가르던 예전 기준은
 * 80점이었는데, 그것을 충족 선으로 쓰면 총점 65점을 「적합」 이라고 부르면서 65점짜리 축은
 * 「미충족」 이라고 적는 화면이 된다. 한 화면에서 두 말이 맞붙어야 하므로 낮은 쪽(60)으로 모은다.
 *
 * 값을 베껴 적지 않고 [SuitabilityLabel.Suitable] 에서 **읽어 온다**(이슈 #200). 같은 수를 두 곳에
 * 적어 두면 한쪽만 고쳐지는 날이 오고, 그것이 이 이슈가 고치는 사고의 모양이다.
 */
public val SUITABILITY_AXIS_FULFILLED_THRESHOLD: Int = SuitabilityLabel.Suitable.minScore

/** One analysis axis of the suitability breakdown, e.g. "분야 유사도 · 40% · 95점". */
public data class SuitabilityAxisUiModel(
    val label: String,
    val score: Int,
    val weightLabel: String,
) {
    /**
     * 충족 여부. 생성자 인자가 아니라 [score] 에서 파생한다 — 점수와 어긋나는 값을
     * 넣을 자리를 아예 두지 않아야 경계가 한 곳에 남는다.
     */
    public val fulfillment: SuitabilityAxisFulfillment
        get() =
            if (score >= SUITABILITY_AXIS_FULFILLED_THRESHOLD) {
                SuitabilityAxisFulfillment.Fulfilled
            } else {
                SuitabilityAxisFulfillment.Unfulfilled
            }

    init {
        requireNonBlank("label", label)
        requireNonBlank("weightLabel", weightLabel)
        requireScore("score", score)
    }
}

/**
 * Display-ready suitability analysis for one posting.
 *
 * @property level 서버가 준 F3-2 레이블. 화면 강조 단계(`CareerCompassScoreLevel`)가 아니라 도메인 값을
 *  그대로 든다 — 게이지 색 구간·배지 톤·[levelLabel] 이 **한 값**에서 갈라져 나와야 서로 어긋나지
 *  않는다(이슈 #200).
 */
public data class SuitabilityUiModel(
    val score: Int,
    val levelLabel: String,
    val level: SuitabilityLabel,
    val breakdown: List<SuitabilityAxisUiModel>,
    val strengthComment: String?,
    val weaknessComment: String?,
) {
    init {
        requireScore("score", score)
        requireNonBlank("levelLabel", levelLabel)
        require(breakdown.size <= POSTING_DETAIL_MAX_BREAKDOWN_COUNT) {
            "breakdown must contain at most $POSTING_DETAIL_MAX_BREAKDOWN_COUNT axes"
        }
        require(breakdown.map(SuitabilityAxisUiModel::label).distinct().size == breakdown.size) {
            "breakdown labels must be unique"
        }
        requireNullOrNonBlank("strengthComment", strengthComment)
        requireNullOrNonBlank("weaknessComment", weaknessComment)
    }
}

/** An application form question auto-detected from the posting (spec F4-1). */
public data class PostingFormQuestionUiModel(
    val order: Int,
    val question: String,
    val maxCharsLabel: String?,
) {
    init {
        require(order >= 1) { "order must be at least 1: $order" }
        requireNonBlank("question", question)
        requireNullOrNonBlank("maxCharsLabel", maxCharsLabel)
    }
}

/** Whether a suitability score can be shown for the posting (spec F2-3, F3-1). */
public sealed interface PostingSuitabilityState {
    /** The user has not entered enough profile data to compute a score. */
    public data object ProfileIncomplete : PostingSuitabilityState

    /**
     * 서버가 아직 점수를 주지 않았다 — 파싱 전일 수도, 영구 실패일 수도 있다(계약이 둘을 가르지 못한다, #200).
     *
     * @property isAutoRecheckExhausted 화면이 스스로 다시 물어보는 횟수를 다 썼다. 그때까지는 기다리는
     *   상태라 행동 버튼이 없고, 다 쓴 뒤에는 「다시 확인」이 실제로 상태를 바꾸는 유일한 손잡이라 버튼을
     *   연다(#221). 문구도 갈린다 — 다 쓴 뒤에는 「곧 됩니다」를 약속하지 않는다.
     */
    public data class Analyzing(
        val isAutoRecheckExhausted: Boolean,
    ) : PostingSuitabilityState

    /** The analysis finished and [suitability] can be rendered. */
    public data class Ready(
        val suitability: SuitabilityUiModel,
    ) : PostingSuitabilityState
}

/** Display-only data for the posting detail screen. */
public data class PostingDetailUiModel(
    val id: String,
    val title: String,
    val category: FeedListingCategory,
    val categoryLabel: String,
    val sourceLabel: String,
    val collectedAtLabel: String,
    val deadlineLabel: String,
    val isDeadlineUrgent: Boolean,
    val isBookmarked: Boolean,
    val suitability: PostingSuitabilityState,
    val keywords: List<String>,
    val qualifications: List<String>,
    val preferences: List<String>,
    val formQuestions: List<PostingFormQuestionUiModel>,
    val similarPostings: List<FeedListingUiModel>,
    val canCreateDraft: Boolean,
) {
    init {
        requireNonBlank("id", id)
        requireNonBlank("title", title)
        require(category != FeedListingCategory.All) {
            "category must be a concrete listing category, not All"
        }
        requireNonBlank("categoryLabel", categoryLabel)
        requireNonBlank("sourceLabel", sourceLabel)
        requireNonBlank("collectedAtLabel", collectedAtLabel)
        requireNonBlank("deadlineLabel", deadlineLabel)
        require(keywords.all(String::isNotBlank)) { "keywords must not contain blank entries" }
        require(keywords.distinct().size == keywords.size) { "keywords must be unique" }
        require(qualifications.all(String::isNotBlank)) { "qualifications must not contain blank entries" }
        require(preferences.all(String::isNotBlank)) { "preferences must not contain blank entries" }
        require(
            formQuestions.map(PostingFormQuestionUiModel::order).distinct().size == formQuestions.size,
        ) {
            "formQuestion orders must be unique"
        }
        require(similarPostings.size <= POSTING_DETAIL_MAX_SIMILAR_POSTING_COUNT) {
            "similarPostings must contain at most $POSTING_DETAIL_MAX_SIMILAR_POSTING_COUNT listings"
        }
        require(similarPostings.map(FeedListingUiModel::id).distinct().size == similarPostings.size) {
            "similarPosting ids must be unique"
        }
        require(!canCreateDraft || category.supportsDraft()) {
            "canCreateDraft is only allowed for Employment or Scholarship postings"
        }
    }
}

/** Mutually exclusive loading states for the detail content. */
public sealed interface PostingDetailContentState {
    public data object Loading : PostingDetailContentState

    public data class Error(
        val message: String,
    ) : PostingDetailContentState {
        init {
            requireNonBlank("message", message)
        }
    }

    /**
     * 서버 점검(503 `LLM_UNAVAILABLE`). [Error] 와 달리 문구가 아니라 전용 안내 화면으로 그린다 —
     * 재시도만 되풀이해도 소용없다는 것을 한 줄 메시지로는 말할 수 없다.
     */
    public data object Maintenance : PostingDetailContentState

    public data class Loaded(
        val posting: PostingDetailUiModel,
    ) : PostingDetailContentState
}

/** Complete, display-ready state for [PostingDetailScreen]. */
public data class PostingDetailUiState(
    val content: PostingDetailContentState,
)

/** User intents emitted by the stateless posting detail UI. */
public sealed interface PostingDetailEvent {
    public data object BackClicked : PostingDetailEvent

    public data object BookmarkToggled : PostingDetailEvent

    public data object ShareClicked : PostingDetailEvent

    public data object ViewOriginalClicked : PostingDetailEvent

    public data object CreateDraftClicked : PostingDetailEvent

    public data object CompleteProfileClicked : PostingDetailEvent

    public data class SimilarPostingSelected(
        val listingId: String,
    ) : PostingDetailEvent

    public data object RetryClicked : PostingDetailEvent

    /**
     * 적합도 카드의 「다시 확인」 — 자동 재조회를 다 쓴 뒤에만 그려진다(#221).
     *
     * [RetryClicked] 와 다르다: 그쪽은 화면 전체를 로딩으로 되돌리지만, 이쪽은 읽은 상세를 그대로 둔 채
     * 적합도만 조용히 다시 묻는다.
     */
    public data object SuitabilityRecheckClicked : PostingDetailEvent
}

/** Draft generation is offered only for employment and scholarship postings (spec F3-3). */
private fun FeedListingCategory.supportsDraft(): Boolean = this == FeedListingCategory.Employment || this == FeedListingCategory.Scholarship

private fun requireNonBlank(
    fieldName: String,
    value: String,
) {
    require(value.isNotBlank()) { "$fieldName must not be blank" }
}

private fun requireNullOrNonBlank(
    fieldName: String,
    value: String?,
) {
    require(value == null || value.isNotBlank()) { "$fieldName must be null or non-blank" }
}

private fun requireScore(
    fieldName: String,
    value: Int,
) {
    require(value in 0..100) { "$fieldName must be between 0 and 100: $value" }
}
