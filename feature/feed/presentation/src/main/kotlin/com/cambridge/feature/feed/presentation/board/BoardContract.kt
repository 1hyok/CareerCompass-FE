package com.cambridge.feature.feed.presentation.board

/** Maximum number of preview posts returned by structure detection (spec F2-1). */
public const val BOARD_MAX_PREVIEW_COUNT: Int = 5

/** Default cap on registered boards (spec F2-1). */
public const val BOARD_DEFAULT_MAX_COUNT: Int = 20

/** Posting category a board is registered under (spec F2-1). */
public enum class BoardType {
    Scholarship,
    Employment,
    Contest,
    ExternalActivity,
    Other,
}

/** How often a board is crawled. */
public enum class BoardCollectCycle(
    public val hours: Int,
) {
    Daily(24),
    TwiceDaily(12),
    Weekly(168),
}

/** One recent post shown in the detection preview. */
public data class BoardPreviewItemUiModel(
    val title: String,
    val url: String,
    val dateLabel: String?,
) {
    init {
        require(title.isNotBlank()) { "title must not be blank" }
        require(url.isNotBlank()) { "url must not be blank" }
        require(dateLabel == null || dateLabel.isNotBlank()) { "dateLabel must be null or non-blank" }
    }
}

/** Why automatic structure detection could not register the board (spec F2-1). */
public enum class BoardDetectionFailure {
    LoginRequired,
    Spa,
    Blocked,
    Failed,
}

/** Lifecycle of the structure detection request. */
public sealed interface BoardDetectionState {
    public data object Idle : BoardDetectionState

    public data object Detecting : BoardDetectionState

    public data class Success(
        val preview: List<BoardPreviewItemUiModel>,
        val dateDetected: Boolean,
    ) : BoardDetectionState {
        init {
            require(preview.size in 1..BOARD_MAX_PREVIEW_COUNT) {
                "preview must contain 1..$BOARD_MAX_PREVIEW_COUNT items"
            }
        }
    }

    public data class Failed(
        val reason: BoardDetectionFailure,
    ) : BoardDetectionState
}

/** Complete, display-ready state for [BoardRegisterScreen]. */
public data class BoardRegisterUiState(
    val url: String,
    val urlError: String?,
    val detection: BoardDetectionState,
    val name: String,
    val type: BoardType?,
    val cycle: BoardCollectCycle,
    val isSubmitting: Boolean,
) {
    init {
        require(urlError == null || urlError.isNotBlank()) { "urlError must be null or non-blank" }
    }

    /** Structure detection needs a URL and must not overlap an in-flight detection or submission. */
    val isDetectEnabled: Boolean
        get() = url.isNotBlank() && detection != BoardDetectionState.Detecting && !isSubmitting

    /** Registration requires a successful detection plus the mandatory name and type. */
    val isRegisterEnabled: Boolean
        get() = detection is BoardDetectionState.Success && name.isNotBlank() && type != null && !isSubmitting
}

/** User intents emitted by the stateless board registration UI. */
public sealed interface BoardRegisterEvent {
    public data class UrlChanged(
        val value: String,
    ) : BoardRegisterEvent

    public data object DetectClicked : BoardRegisterEvent

    public data class NameChanged(
        val value: String,
    ) : BoardRegisterEvent

    public data class TypeSelected(
        val type: BoardType,
    ) : BoardRegisterEvent

    public data class CycleSelected(
        val cycle: BoardCollectCycle,
    ) : BoardRegisterEvent

    public data object RegisterClicked : BoardRegisterEvent

    public data object BackClicked : BoardRegisterEvent
}

/**
 * 등록된 게시판의 수집 상태.
 *
 * [Failing] 은 수집이 실패하는 중이지만 아직 켜져 있는 상태고, [Deactivated] 는 연속 실패로 **서버가 끈** 상태다
 * (기능 스펙 F2-2 「3회 연속 실패 시 비활성화」). 사용자가 직접 끈 [Paused] 와 갈라 두는 이유는 화면이 할 말이
 * 다르기 때문이다 — 꺼진 이유와 되살리는 방법을 [Deactivated] 에서만 안내한다.
 */
public enum class BoardStatus {
    Active,
    Paused,
    Failing,
    Deactivated,
}

/**
 * Display-only data for one registered board.
 *
 * [postingCount] is `null` when the source does not report how many postings the board produced;
 * the count row is then omitted instead of showing a misleading zero.
 */
public data class BoardUiModel(
    val id: String,
    val name: String,
    val url: String,
    val type: BoardType,
    val typeLabel: String,
    val status: BoardStatus,
    val isActive: Boolean,
    val failCount: Int,
    val lastCollectedLabel: String?,
    val postingCount: Int?,
) {
    init {
        require(id.isNotBlank()) { "id must not be blank" }
        require(name.isNotBlank()) { "name must not be blank" }
        require(url.isNotBlank()) { "url must not be blank" }
        require(typeLabel.isNotBlank()) { "typeLabel must not be blank" }
        require(failCount >= 0) { "failCount must not be negative" }
        require(lastCollectedLabel == null || lastCollectedLabel.isNotBlank()) {
            "lastCollectedLabel must be null or non-blank"
        }
        require(postingCount == null || postingCount >= 0) { "postingCount must be null or non-negative" }
        // 실패 상태 둘은 토글 값으로 갈린다. 어긋난 조합을 허용하면 화면이 안내를 잘못 고르므로 만들지 못하게 막는다.
        require(status != BoardStatus.Deactivated || !isActive) { "Deactivated boards must not be active" }
        require(status != BoardStatus.Failing || isActive) { "Failing boards must be active" }
    }
}

/** Mutually exclusive loading states for the board list. */
public sealed interface BoardListContentState {
    public data object Loading : BoardListContentState

    public data object Empty : BoardListContentState

    public data class Loaded(
        val boards: List<BoardUiModel>,
    ) : BoardListContentState {
        init {
            require(boards.isNotEmpty()) { "Use BoardListContentState.Empty when there are no boards" }
            require(boards.map(BoardUiModel::id).distinct().size == boards.size) { "board ids must be unique" }
        }
    }
}

/** Complete, display-ready state for [BoardListScreen]. */
public data class BoardListUiState(
    val content: BoardListContentState,
    val maxBoardCount: Int = BOARD_DEFAULT_MAX_COUNT,
) {
    init {
        require(maxBoardCount > 0) { "maxBoardCount must be positive" }
        if (content is BoardListContentState.Loaded) {
            require(content.boards.size <= maxBoardCount) {
                "boards must not exceed maxBoardCount ($maxBoardCount)"
            }
        }
    }
}

/** User intents emitted by the stateless board list UI. */
public sealed interface BoardListEvent {
    public data object AddBoardClicked : BoardListEvent

    public data class BoardToggled(
        val boardId: String,
    ) : BoardListEvent

    public data class RetryClicked(
        val boardId: String,
    ) : BoardListEvent

    public data class DeleteClicked(
        val boardId: String,
    ) : BoardListEvent

    public data class BoardSelected(
        val boardId: String,
    ) : BoardListEvent

    public data object BackClicked : BoardListEvent
}
