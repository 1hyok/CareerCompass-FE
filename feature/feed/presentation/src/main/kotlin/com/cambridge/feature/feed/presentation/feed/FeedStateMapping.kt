package com.cambridge.feature.feed.presentation.feed

import android.content.res.Resources
import androidx.annotation.StringRes
import com.cambridge.core.model.board.Board
import com.cambridge.feature.feed.presentation.FeedContentState
import com.cambridge.feature.feed.presentation.FeedUiState
import com.cambridge.feature.feed.presentation.R
import com.cambridge.feature.feed.presentation.feedfilter.FeedBoardFilterUiModel
import com.cambridge.feature.feed.presentation.feedfilter.FeedDeadlineFilter
import com.cambridge.feature.feed.presentation.feedfilter.FeedFilterUiState
import com.cambridge.feature.feed.presentation.shared.util.feedCategoryFilters
import com.cambridge.feature.feed.presentation.shared.util.toListingUiModel
import com.cambridge.feature.feed.presentation.shared.util.toMinScoreFilter
import com.cambridge.feature.feed.presentation.shared.util.toSortOption
import com.cambridge.feature.feed.presentation.shared.util.toSortUiModel
import java.time.Clock
import java.time.Instant
import java.time.format.DateTimeFormatter
import java.util.Locale

/** [FeedViewState] → [FeedUiState]. 오류 상태는 화면 계약에 없으므로 Entry 가 따로 그린다. */
internal fun FeedViewState.toFeedUiState(
    resources: Resources,
    clock: Clock,
): FeedUiState =
    FeedUiState(
        userName = userName ?: resources.getString(R.string.feed_user_name_fallback),
        newListingCount = todayNewCount,
        searchQuery = searchInput,
        filters = feedCategoryFilters(resources),
        selectedFilter = selectedCategory,
        selectedSort = query.sort.toSortOption().toSortUiModel(resources),
        totalListingCount = postings.size,
        content =
            when {
                loadState == FeedLoadState.Loading -> FeedContentState.Loading
                postings.isEmpty() -> FeedContentState.Empty
                else -> FeedContentState.Loaded(postings.map { it.toListingUiModel(resources, clock) })
            },
        activeFilterCount = activeFilterCount,
        offlineNotice = offlineSavedAt?.let { savedAt -> resources.getString(R.string.feed_offline_notice, savedAt.toNoticeLabel(clock)) },
    )

/** 시트 초안 → 시트 계약. 목록에 없는 게시판 선택은 버린다(계약 불변식). 건수 미리 계산은 없어 `null`. */
internal fun FeedFilterDraft.toFilterUiState(
    resources: Resources,
    boards: List<Board>,
): FeedFilterUiState {
    val boardModels = boards.map { FeedBoardFilterUiModel(id = it.id.toString(), name = it.name) }
    val knownIds = boardModels.map(FeedBoardFilterUiModel::id).toSet()
    return FeedFilterUiState(
        categories = feedCategoryFilters(resources),
        selectedCategory = category,
        boards = boardModels,
        selectedBoardIds = boardIds.map(Long::toString).filter { it in knownIds }.toSet(),
        deadline = deadline,
        deadlineRange = deadlineRange.takeIf { deadline == FeedDeadlineFilter.Range },
        minScore = minScore.toMinScoreFilter(),
        unreadOnly = unreadOnly,
        matchingCount = null,
    )
}

@StringRes
internal fun FeedMessage.messageRes(): Int =
    when (this) {
        FeedMessage.BookmarkFailed -> R.string.feed_bookmark_failed
        FeedMessage.LoadMoreFailed -> R.string.feed_load_more_failed
        FeedMessage.RefreshFailed -> R.string.feed_refresh_failed
        FeedMessage.OfflineReadOnly -> R.string.feed_offline_read_only
    }

/**
 * 스냅샷 저장 시각의 배너 문구 — 「9월 3일 14:20」. 주입된 [clock] 의 지역 시간대로 읽는다.
 *
 * 연도를 넣지 않는 이유: 스냅샷은 마지막 조회 한 번의 사본이라 해가 바뀔 만큼 묵을 수 없고, 짧을수록
 * 한 줄 배너가 줄바꿈되지 않는다.
 */
private fun Instant.toNoticeLabel(clock: Clock): String = NOTICE_FORMATTER.format(atZone(clock.zone))

private val NOTICE_FORMATTER: DateTimeFormatter = DateTimeFormatter.ofPattern("M월 d일 HH:mm", Locale.KOREAN)
