package com.careercompass.feature.feed.presentation.board

/**
 * Complete, display-ready state for [BoardEditSheetContent] (`PATCH /boards/{id}`).
 *
 * [boardName] is the name the board currently has on the server; [name] is what the user is typing.
 * The URL is bound to the detected structure, so the sheet only shows it and never edits it.
 * [hasChanges] is decided by the owner from the same diff that builds the request, so the save
 * button is enabled exactly when a request would go out.
 */
public data class BoardEditUiState(
    val boardName: String,
    val url: String,
    val name: String,
    val nameError: String?,
    val type: BoardType,
    val cycle: BoardCollectCycle,
    val isSaving: Boolean,
    val hasChanges: Boolean,
) {
    init {
        require(boardName.isNotBlank()) { "boardName must not be blank" }
        require(url.isNotBlank()) { "url must not be blank" }
        require(nameError == null || nameError.isNotBlank()) { "nameError must be null or non-blank" }
    }

    /** Saving needs a non-blank name, no in-flight save, and at least one field that differs from the original. */
    val isSaveEnabled: Boolean
        get() = name.isNotBlank() && !isSaving && hasChanges
}

/** User intents emitted by the stateless board edit sheet. */
public sealed interface BoardEditEvent {
    public data class NameChanged(
        val value: String,
    ) : BoardEditEvent

    public data class TypeSelected(
        val type: BoardType,
    ) : BoardEditEvent

    public data class CycleSelected(
        val cycle: BoardCollectCycle,
    ) : BoardEditEvent

    public data object SaveClicked : BoardEditEvent

    public data object DismissClicked : BoardEditEvent
}
