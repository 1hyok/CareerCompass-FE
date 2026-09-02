package com.cambridge.feature.feed.presentation.postingdetail

import com.cambridge.core.ui.component.CareerCompassScoreLevel
import com.cambridge.feature.feed.presentation.FeedListingCategory
import com.cambridge.feature.feed.presentation.FeedListingUiModel

/** Number of analysis axes the suitability breakdown renders (spec F3-2). */
public const val POSTING_DETAIL_MAX_BREAKDOWN_COUNT: Int = 4

/** Maximum number of similar postings recommended on the detail screen (spec F3-3). */
public const val POSTING_DETAIL_MAX_SIMILAR_POSTING_COUNT: Int = 3

/** One analysis axis of the suitability breakdown, e.g. "분야 유사도 · 40% · 95점". */
public data class SuitabilityAxisUiModel(
    val label: String,
    val score: Int,
    val weightLabel: String,
) {
    init {
        requireNonBlank("label", label)
        requireNonBlank("weightLabel", weightLabel)
        requireScore("score", score)
    }
}

/** Display-ready suitability analysis for one posting. */
public data class SuitabilityUiModel(
    val score: Int,
    val levelLabel: String,
    val level: CareerCompassScoreLevel,
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

    /** The posting is still being parsed, so the score is not available yet. */
    public data object Analyzing : PostingSuitabilityState

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
