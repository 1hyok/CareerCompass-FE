package com.careercompass.feature.feed.presentation.postingraw

/** Display-ready state for [PostingRawScreen]. [originalUrl] is `null` when the source link is unknown. */
public data class PostingRawUiState(
    val title: String,
    val sourceLabel: String,
    val originalUrl: String?,
    val rawContent: String,
) {
    init {
        require(title.isNotBlank()) { "title must not be blank" }
        require(sourceLabel.isNotBlank()) { "sourceLabel must not be blank" }
        require(originalUrl == null || originalUrl.isNotBlank()) { "originalUrl must be null or non-blank" }
        require(rawContent.isNotBlank()) { "rawContent must not be blank" }
    }
}

/** User intents emitted by the stateless raw posting UI. */
public sealed interface PostingRawEvent {
    public data object BackClicked : PostingRawEvent

    public data object OpenOriginalClicked : PostingRawEvent
}
