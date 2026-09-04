package com.careercompass.feature.feed.presentation.feed

import android.content.res.Resources
import androidx.annotation.StringRes
import com.careercompass.core.model.board.Board
import com.careercompass.feature.feed.presentation.FeedContentState
import com.careercompass.feature.feed.presentation.FeedEmptyReason
import com.careercompass.feature.feed.presentation.FeedLoadMoreState
import com.careercompass.feature.feed.presentation.FeedUiState
import com.careercompass.feature.feed.presentation.R
import com.careercompass.feature.feed.presentation.board.BoardCollectCycle
import com.careercompass.feature.feed.presentation.board.labelRes
import com.careercompass.feature.feed.presentation.feedfilter.FeedBoardFilterUiModel
import com.careercompass.feature.feed.presentation.feedfilter.FeedDeadlineFilter
import com.careercompass.feature.feed.presentation.feedfilter.FeedFilterUiState
import com.careercompass.feature.feed.presentation.feedfilter.FeedMissingBoardsReason
import com.careercompass.feature.feed.presentation.feedfilter.FeedMissingBoardsUiModel
import com.careercompass.feature.feed.presentation.shared.util.feedCategoryFilters
import com.careercompass.feature.feed.presentation.shared.util.toCollectCycle
import com.careercompass.feature.feed.presentation.shared.util.toListingUiModel
import com.careercompass.feature.feed.presentation.shared.util.toMinScoreFilter
import com.careercompass.feature.feed.presentation.shared.util.toSortOption
import com.careercompass.feature.feed.presentation.shared.util.toSortUiModel
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

                // 빈 목록에서 「더 찾아보기」를 누른 뒤에도 진행 표시가 있어야 한다 — 목록이 없으니
                // 붙일 자리가 없고, 아무 반응이 없으면 버튼이 죽은 것으로 보인다.
                postings.isEmpty() && isLoadingMore -> FeedContentState.Loading

                postings.isEmpty() -> FeedContentState.Empty(toEmptyReason(resources))

                else -> FeedContentState.Loaded(postings.map { it.toListingUiModel(resources, clock, profile) })
            },
        activeFilterCount = activeFilterCount,
        offlineNotice = offlineSavedAt?.let { savedAt -> resources.getString(R.string.feed_offline_notice, savedAt.toNoticeLabel(clock)) },
        isProfileNoticeVisible = isProfileNoticeVisible,
    )

/**
 * 목록이 빈 사유를 하나 고른다. 겹칠 때의 우선순위와 그 근거는 [FeedEmptyReason] 의 KDoc 에 있다.
 *
 * 「게시판 0개」는 목록을 **받아 본 뒤에만** 말한다([FeedViewState.boardsLoaded]) — 게시판 조회는 피드
 * 조회와 별개로 실패할 수 있고(실패해도 피드는 막지 않는다), 그때 빈 목록을 0개로 읽으면 게시판을 20개
 * 등록해 둔 사용자에게 등록하라고 하게 된다. 아직 모르면 조건 쪽 사유로 내려간다.
 *
 * **커서가 남아 있으면([FeedViewState.hasNext]) 「없다」고 말하지 않는다.** 검색어·마감일은 받아 온
 * 페이지 안에서만 걸러지므로, 앞쪽 몇 페이지가 통째로 걸러진 것과 서버에 정말 없는 것이 여기서는 똑같이
 * 빈 목록으로 보인다. 그 둘을 가르는 유일한 근거가 커서다 — 남아 있으면 [FeedEmptyReason.MoreAvailable]
 * 로 「아직 못 찾았다」고 말하고 이어 읽을 길을 준다.
 *
 * 그 자리에서 이어 읽기가 실패했을 때도([FeedLoadMoreState.Failed]) 같은 사유를 쓴다 — 실패는 이미
 * 스낵바가 말했고, 화면이 할 말은 어느 쪽이든 「여기까지 찾았고, 더 찾아볼 수 있다」로 같다.
 *
 * **사라진 게시판은 검색어·필터보다 앞에 둔다**([FeedViewState.hasDeletedBoardFilter], 이슈 #206). 화면이
 * 한 번에 말할 수 있는 사유는 하나인데, 나머지 조건은 사용자가 화면에서 볼 수 있고 이것만은 어디에도
 * 보이지 않는다. 다만 「게시판 0개」·[FeedEmptyReason.MoreAvailable] 보다는 뒤다 — 모을 곳이 아예 없으면
 * 조건을 빼도 나올 것이 없고, 아직 안 읽은 페이지가 남았으면 「없다」고 말할 근거가 아직 없다.
 */
internal fun FeedViewState.toEmptyReason(resources: Resources): FeedEmptyReason =
    when {
        isOffline -> FeedEmptyReason.OfflineSnapshot
        boardsLoaded && boards.isEmpty() -> FeedEmptyReason.NoBoards
        hasNext -> FeedEmptyReason.MoreAvailable
        hasDeletedBoardFilter -> toMissingBoardsReason()
        query.hasSearchQuery -> FeedEmptyReason.Search(query.searchQuery)
        hasActiveFilter -> FeedEmptyReason.Filter
        else -> FeedEmptyReason.NotCollected(boards.toCollectNotice(resources))
    }

/**
 * 사라진 게시판 사유 하나 — 개수와 「이 조건이 전부인가」를 함께 싣는다.
 *
 * 개수는 이름을 대신한다. 조건이 들고 있는 것은 id 뿐이고 그 게시판은 이미 목록에서 사라져, 이름을 알
 * 길이 없다(시트의 태그가 개수로만 묶는 것과 같은 이유, 이슈 #155).
 */
private fun FeedViewState.toMissingBoardsReason(): FeedEmptyReason.MissingBoards =
    FeedEmptyReason.MissingBoards(count = missingBoardIds.size, isOnlyCondition = missingBoardsAreOnlyCondition)

/**
 * 「언제쯤 들어오나」 한 줄 — 수집이 도는 게시판이 없으면 null 이라 아무 말도 하지 않는다.
 *
 * **주기가 게시판마다 다르면 가장 짧은 주기를 말한다.** 사용자가 묻는 것은 「목록이 언제 달라지나」이고,
 * 그 답은 가장 먼저 도는 게시판이 정한다. 가장 긴 주기를 말하면 이미 들어와 있을 시각에도 더 기다리라고
 * 하게 되고, 평균은 어느 게시판의 주기도 아니라 근거가 없다. 대신 문구를 「가장 자주 보는 게시판을 …」로
 * 갈아 끼워, 나머지 게시판이 더 느리다는 사실을 숨기지 않는다.
 *
 * 꺼 둔 게시판은 세지 않는다 — 수집이 돌지 않으므로 그 주기는 목록이 달라질 시점을 말해 주지 않는다.
 */
private fun List<Board>.toCollectNotice(resources: Resources): String? {
    val cycles = filter(Board::isActive).map { it.cycleHours.toCollectCycle() }
    val shortest = cycles.minByOrNull(BoardCollectCycle::hours) ?: return null
    val noticeRes =
        if (cycles.distinct().size == 1) {
            R.string.feed_empty_collect_notice
        } else {
            R.string.feed_empty_collect_notice_mixed
        }
    return resources.getString(noticeRes, resources.getString(shortest.labelRes()))
}

/**
 * 시트 초안 → 시트 계약. 건수 미리 계산은 없어 `null`.
 *
 * **목록에 없는 게시판 id 를 버리지 않는다**(이슈 #155). 조건에서 빼면 사용자가 모르는 사이 조회가
 * 넓어지고, 태그로 그리지 않으면 배지에만 남아 끌 수 없는 조건이 된다 — [FeedFilterUiState.missingBoards]
 * 로 넘겨 시트가 켜진 태그 하나로 보이고, 누르면 끄게 한다.
 *
 * [boardsLoaded] 로 「지워졌다」와 「모른다」를 가른다. 게시판 조회는 피드와 별개로 실패할 수 있고
 * (실패해도 피드는 막지 않는다), 받아 본 적 없는 빈 목록을 근거로 「지워졌다」고 말하면 잠깐 연결이 끊긴
 * 사용자에게 없는 사실을 알리게 된다. 어느 쪽이든 **조건은 그대로 둔다** — 지우는 것은 사용자의 몫이다.
 */
internal fun FeedFilterDraft.toFilterUiState(
    resources: Resources,
    boards: List<Board>,
    boardsLoaded: Boolean,
): FeedFilterUiState {
    val boardModels = boards.map { FeedBoardFilterUiModel(id = it.id.toString(), name = it.name) }
    val missingIds = boardIds.missingFrom(boards)
    return FeedFilterUiState(
        categories = feedCategoryFilters(resources),
        selectedCategory = category,
        boards = boardModels,
        selectedBoardIds = (boardIds - missingIds).map(Long::toString).toSet(),
        missingBoards =
            if (missingIds.isEmpty()) {
                null
            } else {
                FeedMissingBoardsUiModel(
                    count = missingIds.size,
                    reason = if (boardsLoaded) FeedMissingBoardsReason.Deleted else FeedMissingBoardsReason.Unverified,
                )
            },
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
