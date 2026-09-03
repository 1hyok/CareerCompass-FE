package com.cambridge.feature.feed.presentation

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cambridge.core.ui.component.CareerCompassBadge
import com.cambridge.core.ui.component.CareerCompassBadgeTone
import com.cambridge.core.ui.component.CareerCompassButton
import com.cambridge.core.ui.component.CareerCompassButtonSize
import com.cambridge.core.ui.component.CareerCompassButtonVariant
import com.cambridge.core.ui.component.CareerCompassEmptyState
import com.cambridge.core.ui.component.CareerCompassTag
import com.cambridge.core.ui.icon.CareerCompassIcons
import com.cambridge.core.ui.theme.CareerCompassTheme
import com.cambridge.feature.feed.presentation.shared.component.FEED_ICON_SIZE
import com.cambridge.feature.feed.presentation.shared.component.FEED_INLINE_ICON_SIZE
import com.cambridge.feature.feed.presentation.shared.component.FeedIconButton
import com.cambridge.feature.feed.presentation.shared.component.FeedSuitabilityChip
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter

/** Stateless main feed matching the CareerCompass feed design. */
@Composable
public fun FeedScreen(
    state: FeedUiState,
    onEvent: (FeedUiEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    FeedScreen(
        state = state,
        onEvent = onEvent,
        listState = rememberLazyListState(),
        onLoadMore = null,
        isLoadingMore = false,
        modifier = modifier,
    )
}

/**
 * Stateless main feed with infinite-scroll hooks.
 *
 * [onLoadMore] is invoked when the last items of [listState] come into view; pass `null` when the
 * host has no further pages to offer. [isLoadingMore] appends a progress row below the listings.
 */
@Composable
public fun FeedScreen(
    state: FeedUiState,
    onEvent: (FeedUiEvent) -> Unit,
    listState: LazyListState,
    onLoadMore: (() -> Unit)?,
    isLoadingMore: Boolean,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier =
            modifier
                .fillMaxSize()
                .background(CareerCompassTheme.colors.subtleSurface)
                .windowInsetsPadding(WindowInsets.safeDrawing),
    ) {
        FeedHeader(state = state, onEvent = onEvent)
        state.offlineNotice?.let { notice -> FeedOfflineBanner(notice = notice) }
        if (state.isProfileNoticeVisible) {
            FeedProfileNoticeBanner(onClick = { onEvent(FeedUiEvent.CompleteProfileSelected) })
        }
        FeedSortRow(state = state, onEvent = onEvent)
        when (val content = state.content) {
            FeedContentState.Loading -> {
                FeedLoading(modifier = Modifier.weight(1f))
            }

            is FeedContentState.Empty -> {
                FeedEmpty(
                    reason = content.reason,
                    onEvent = onEvent,
                    modifier = Modifier.weight(1f),
                )
            }

            is FeedContentState.Loaded -> {
                FeedListingList(
                    listings = content.listings,
                    onEvent = onEvent,
                    listState = listState,
                    onLoadMore = onLoadMore,
                    isLoadingMore = isLoadingMore,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
private fun FeedHeader(
    state: FeedUiState,
    onEvent: (FeedUiEvent) -> Unit,
) {
    val spacing = CareerCompassTheme.spacing

    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(start = spacing.large, top = spacing.small, end = spacing.large),
        verticalArrangement = Arrangement.spacedBy(spacing.medium),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(
                modifier =
                    Modifier
                        .weight(1f)
                        .padding(end = spacing.small),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                Text(
                    text =
                        "${stringResource(R.string.feed_greeting, state.userName)} " +
                            stringResource(R.string.feed_icon_wave),
                    modifier = Modifier.semantics { heading() },
                    color = CareerCompassTheme.colors.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style =
                        CareerCompassTheme.typography.headline4.copy(
                            fontSize = 17.sp,
                            lineHeight = 25.5.sp,
                            letterSpacing = (-0.2).sp,
                        ),
                )
                Text(
                    text =
                        stringResource(
                            R.string.feed_today_new_listing_count,
                            state.newListingCount,
                        ),
                    color = CareerCompassTheme.colors.onSurfaceVariant,
                    style =
                        CareerCompassTheme.typography.caption.copy(
                            fontSize = 12.sp,
                            lineHeight = 18.sp,
                        ),
                )
            }
            FeedIconButton(
                icon = CareerCompassIcons.Notifications,
                contentDescription = stringResource(R.string.feed_notification_content_description),
                onClick = { onEvent(FeedUiEvent.NotificationsSelected) },
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(spacing.small),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            FeedSearchField(
                value = state.searchQuery,
                onValueChange = { onEvent(FeedUiEvent.SearchQueryChanged(it)) },
                modifier = Modifier.weight(1f),
            )
            FeedFilterButton(
                activeFilterCount = state.activeFilterCount,
                onClick = { onEvent(FeedUiEvent.FilterRequested) },
            )
        }

        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .selectableGroup(),
            horizontalArrangement = Arrangement.spacedBy(spacing.xSmall),
        ) {
            state.filters.forEach { filter ->
                val selected = filter.category == state.selectedFilter
                CareerCompassTag(
                    label = filter.label,
                    selected = selected,
                    onClick = { onEvent(FeedUiEvent.FilterSelected(filter.category)) },
                    stateDescription =
                        stringResource(
                            if (selected) {
                                R.string.feed_filter_selected_state
                            } else {
                                R.string.feed_filter_unselected_state
                            },
                        ),
                    role = Role.RadioButton,
                )
            }
        }
    }
}

@Composable
private fun FeedSearchField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = CareerCompassTheme.colors
    val shape = CareerCompassTheme.shapes.largeControl
    val searchContentDescription =
        stringResource(R.string.feed_search_content_description)

    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        modifier =
            modifier
                .fillMaxWidth()
                .height(48.dp)
                .clip(shape)
                .background(colors.surface)
                .border(width = 1.dp, color = colors.interactiveOutline, shape = shape)
                .semantics {
                    contentDescription = searchContentDescription
                },
        singleLine = true,
        textStyle =
            CareerCompassTheme.typography.bodyMedium.copy(
                color = colors.onSurface,
                fontSize = 14.sp,
                lineHeight = 21.sp,
            ),
        cursorBrush = SolidColor(colors.primaryEmphasis),
        decorationBox = { innerTextField ->
            Row(
                modifier =
                    Modifier
                        .fillMaxSize()
                        .padding(horizontal = 14.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    imageVector = CareerCompassIcons.Search,
                    contentDescription = null,
                    modifier = Modifier.size(FEED_INLINE_ICON_SIZE),
                    tint = colors.mutedContent,
                )
                Box(modifier = Modifier.weight(1f)) {
                    if (value.isEmpty()) {
                        // 큰 글꼴에서는 안내 문구가 48dp 입력칸 폭을 넘는다. 접히면 두 번째 줄이
                        // 고정 높이에 잘려 사라지므로 한 줄로 두고 말줄임으로 끝맺는다.
                        Text(
                            text = stringResource(R.string.feed_search_placeholder),
                            color = colors.mutedContent,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            style =
                                CareerCompassTheme.typography.bodyMedium.copy(
                                    fontSize = 14.sp,
                                    lineHeight = 21.sp,
                                ),
                        )
                    }
                    innerTextField()
                }
            }
        },
    )
}

/** 48dp filter trigger. The badge shows how many sheet conditions are active (spec F2-3 「필터 조건」). */
@Composable
private fun FeedFilterButton(
    activeFilterCount: Int,
    onClick: () -> Unit,
) {
    val colors = CareerCompassTheme.colors
    val shape = CareerCompassTheme.shapes.largeControl
    val filterDescription = stringResource(R.string.feed_filter_content_description)
    val activeStateDescription =
        if (activeFilterCount > 0) {
            stringResource(R.string.feed_filter_active_count_state, activeFilterCount)
        } else {
            null
        }

    Box(
        modifier =
            Modifier
                .size(48.dp)
                .clip(shape)
                .background(colors.surface)
                .border(width = 1.dp, color = colors.interactiveOutline, shape = shape)
                .clickable(role = Role.Button, onClick = onClick)
                .semantics {
                    contentDescription = filterDescription
                    role = Role.Button
                    if (activeStateDescription != null) {
                        stateDescription = activeStateDescription
                    }
                },
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = CareerCompassIcons.Filter,
            contentDescription = null,
            modifier = Modifier.size(FEED_INLINE_ICON_SIZE),
            tint = if (activeFilterCount > 0) colors.primaryEmphasis else colors.onSurface,
        )
        if (activeFilterCount > 0) {
            Box(
                modifier =
                    Modifier
                        .align(Alignment.TopEnd)
                        .padding(top = 6.dp, end = 6.dp)
                        .size(16.dp)
                        .background(colors.primary, CareerCompassTheme.shapes.pill),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = activeFilterCount.toString(),
                    modifier = Modifier.clearAndSetSemantics {},
                    color = colors.onPrimary,
                    maxLines = 1,
                    style =
                        CareerCompassTheme.typography.caption.copy(
                            fontSize = 10.sp,
                            lineHeight = 12.sp,
                            fontWeight = FontWeight.Bold,
                        ),
                )
            }
        }
    }
}

@Composable
private fun FeedSortRow(
    state: FeedUiState,
    onEvent: (FeedUiEvent) -> Unit,
) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .height(48.dp)
                .padding(horizontal = CareerCompassTheme.spacing.large),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = stringResource(R.string.feed_listing_count, state.totalListingCount),
            color = CareerCompassTheme.colors.onSurfaceVariant,
            style =
                CareerCompassTheme.typography.caption.copy(
                    fontSize = 12.sp,
                    lineHeight = 18.sp,
                    fontWeight = FontWeight.SemiBold,
                ),
        )
        CareerCompassButton(
            text = state.selectedSort.label,
            onClick = { onEvent(FeedUiEvent.SortMenuRequested) },
            variant = CareerCompassButtonVariant.Ghost,
            size = CareerCompassButtonSize.Small,
            trailingIcon = {
                Icon(
                    imageVector = CareerCompassIcons.ExpandMore,
                    contentDescription = null,
                    modifier = Modifier.size(FEED_INLINE_ICON_SIZE),
                )
            },
            contentDescription =
                stringResource(
                    R.string.feed_sort_content_description,
                    state.selectedSort.label,
                ),
        )
    }
}

@Composable
private fun FeedListingList(
    listings: List<FeedListingUiModel>,
    onEvent: (FeedUiEvent) -> Unit,
    listState: LazyListState,
    onLoadMore: (() -> Unit)?,
    isLoadingMore: Boolean,
    modifier: Modifier = Modifier,
) {
    if (onLoadMore != null) {
        val currentOnLoadMore by rememberUpdatedState(onLoadMore)
        LaunchedEffect(listState, listings.size) {
            snapshotFlow {
                val layoutInfo = listState.layoutInfo
                val lastVisibleIndex = layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: -1
                lastVisibleIndex >= layoutInfo.totalItemsCount - LOAD_MORE_THRESHOLD
            }.distinctUntilChanged()
                .filter { it }
                .collect { currentOnLoadMore() }
        }
    }

    LazyColumn(
        modifier = modifier.fillMaxWidth(),
        state = listState,
        contentPadding =
            PaddingValues(
                start = CareerCompassTheme.spacing.large,
                end = CareerCompassTheme.spacing.large,
                bottom = CareerCompassTheme.spacing.large,
            ),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        items(items = listings, key = FeedListingUiModel::id) { listing ->
            FeedListingCard(
                listing = listing,
                onSelected = { onEvent(FeedUiEvent.ListingSelected(listing.id)) },
                onBookmarkToggled = { onEvent(FeedUiEvent.BookmarkToggled(listing.id)) },
            )
        }
        if (isLoadingMore) {
            item(key = "loading-more") {
                FeedLoadingMoreRow()
            }
        }
    }
}

@Composable
private fun FeedLoadingMoreRow() {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(vertical = CareerCompassTheme.spacing.medium),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        CircularProgressIndicator(
            modifier = Modifier.size(20.dp),
            color = CareerCompassTheme.colors.primaryEmphasis,
            strokeWidth = 2.dp,
        )
        Spacer(modifier = Modifier.width(CareerCompassTheme.spacing.small))
        Text(
            text = stringResource(R.string.feed_loading_more),
            color = CareerCompassTheme.colors.onSurfaceVariant,
            style = CareerCompassTheme.typography.caption,
        )
    }
}

/** A reusable listing card that emits separate card and bookmark events. */
@Composable
public fun FeedListingCard(
    listing: FeedListingUiModel,
    onSelected: () -> Unit,
    onBookmarkToggled: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = CareerCompassTheme.colors
    val cardShape = RoundedCornerShape(16.dp)
    val newListingContentDescription =
        stringResource(R.string.feed_new_listing_content_description)

    Surface(
        modifier =
            modifier
                .fillMaxWidth()
                .clip(cardShape)
                .clickable(role = Role.Button, onClick = onSelected),
        shape = cardShape,
        color = colors.surface,
        border = BorderStroke(width = 1.dp, color = colors.surfaceVariant),
        shadowElevation = 1.dp,
    ) {
        Column(
            modifier = Modifier.padding(start = 16.dp, top = 16.dp, end = 4.dp, bottom = 4.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    CareerCompassBadge(
                        label = listing.categoryLabel,
                        tone = listing.category.badgeTone(),
                    )
                    CareerCompassBadge(
                        label = listing.sourceLabel,
                        tone = CareerCompassBadgeTone.Neutral,
                    )
                    if (listing.isNew) {
                        Box(
                            modifier =
                                Modifier
                                    .size(6.dp)
                                    .background(colors.success, CareerCompassTheme.shapes.pill)
                                    .semantics {
                                        contentDescription = newListingContentDescription
                                    },
                        )
                    }
                }
                Spacer(modifier = Modifier.width(6.dp))
                FeedSuitabilityChip(state = listing.suitability)
            }

            Text(
                text = listing.title,
                modifier = Modifier.padding(end = 12.dp),
                color = colors.onSurface,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                style =
                    CareerCompassTheme.typography.headline4.copy(
                        fontSize = 16.sp,
                        lineHeight = 24.sp,
                        letterSpacing = (-0.1).sp,
                    ),
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = listing.deadlineLabel,
                    color = if (listing.isDeadlineUrgent) colors.actionDanger else colors.mutedContent,
                    style =
                        CareerCompassTheme.typography.caption.copy(
                            fontSize = 12.sp,
                            lineHeight = 18.sp,
                            fontWeight = FontWeight.SemiBold,
                        ),
                )
                FeedBookmarkToggle(
                    title = listing.title,
                    bookmarked = listing.isBookmarked,
                    onClick = onBookmarkToggled,
                )
            }
        }
    }
}

@Composable
private fun FeedBookmarkToggle(
    title: String,
    bookmarked: Boolean,
    onClick: () -> Unit,
) {
    val bookmarkDescription =
        stringResource(
            R.string.feed_bookmark_content_description,
            title,
        )
    val bookmarkStateDescription =
        stringResource(
            if (bookmarked) {
                R.string.feed_bookmark_saved_state
            } else {
                R.string.feed_bookmark_not_saved_state
            },
        )

    Box(
        modifier =
            Modifier
                .size(48.dp)
                .semantics {
                    contentDescription = bookmarkDescription
                    stateDescription = bookmarkStateDescription
                }.toggleable(
                    value = bookmarked,
                    role = Role.Checkbox,
                    onValueChange = { onClick() },
                ),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = if (bookmarked) CareerCompassIcons.Bookmark else CareerCompassIcons.BookmarkBorder,
            contentDescription = null,
            modifier = Modifier.size(FEED_ICON_SIZE),
            tint = if (bookmarked) CareerCompassTheme.colors.primaryEmphasis else CareerCompassTheme.colors.mutedContent,
        )
    }
}

@Composable
private fun FeedLoading(modifier: Modifier = Modifier) {
    Box(modifier = modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(CareerCompassTheme.spacing.medium),
        ) {
            CircularProgressIndicator(color = CareerCompassTheme.colors.primaryEmphasis)
            Text(
                text = stringResource(R.string.feed_loading),
                color = CareerCompassTheme.colors.onSurfaceVariant,
                style = CareerCompassTheme.typography.bodyMedium,
            )
        }
    }
}

/**
 * 저장해 둔 스냅샷을 보여 주는 중이라는 표시 — 목록의 값이 지금 서버 상태가 아니라는 것을 화면에 남긴다.
 *
 * 아이콘은 장식이라 스크린 리더에서 지우고([clearAndSetSemantics]), 문구 하나만 읽히게 둔다.
 */
@Composable
private fun FeedOfflineBanner(notice: String) {
    val colors = CareerCompassTheme.colors
    val spacing = CareerCompassTheme.spacing

    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(horizontal = spacing.large, vertical = spacing.xSmall)
                .background(color = colors.warningContainer, shape = CareerCompassTheme.shapes.control)
                .padding(horizontal = spacing.medium, vertical = spacing.small),
        horizontalArrangement = Arrangement.spacedBy(spacing.xSmall),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = stringResource(R.string.feed_icon_offline),
            modifier = Modifier.clearAndSetSemantics {},
            style = CareerCompassTheme.typography.bodyMedium,
        )
        Text(
            text = notice,
            color = colors.onWarningContainer,
            style = CareerCompassTheme.typography.caption,
        )
    }
}

/**
 * 프로필이 비어 적합도를 못 내는 항목이 목록에 있을 때의 안내 — 누르면 마이 탭(프로필 입력)으로 간다.
 *
 * 카드마다 같은 말을 되풀이하지 않고 목록 위에 한 번만 얹는다. 행 전체가 버튼이라 아이콘·문구는
 * 스크린 리더에서 하나로 합치고([semantics] `mergeDescendants`), 터치 높이는 48dp 아래로 내려가지 않게 잡는다.
 */
@Composable
private fun FeedProfileNoticeBanner(onClick: () -> Unit) {
    val colors = CareerCompassTheme.colors
    val spacing = CareerCompassTheme.spacing
    val shape = CareerCompassTheme.shapes.control
    val noticeDescription = stringResource(R.string.feed_profile_notice_content_description)

    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(horizontal = spacing.large, vertical = spacing.xSmall)
                .clip(shape)
                .background(color = colors.primaryContainer, shape = shape)
                .clickable(role = Role.Button, onClick = onClick)
                .heightIn(min = 48.dp)
                .padding(horizontal = spacing.medium, vertical = spacing.small)
                .semantics(mergeDescendants = true) {
                    contentDescription = noticeDescription
                },
        horizontalArrangement = Arrangement.spacedBy(spacing.xSmall),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = stringResource(R.string.feed_icon_profile_notice),
            modifier = Modifier.clearAndSetSemantics {},
            style = CareerCompassTheme.typography.bodyMedium,
        )
        Text(
            text = stringResource(R.string.feed_profile_notice),
            modifier = Modifier.weight(1f),
            color = colors.onPrimaryContainer,
            style = CareerCompassTheme.typography.caption,
        )
        Text(
            text = stringResource(R.string.feed_profile_notice_action),
            color = colors.primaryEmphasis,
            maxLines = 1,
            style = CareerCompassTheme.typography.caption.copy(fontWeight = FontWeight.SemiBold),
        )
    }
}

/**
 * 빈 목록 — [FeedEmptyReason] 마다 다른 안내와 행동을 준다.
 *
 * 사유를 가르는 규칙과 우선순위는 [FeedEmptyReason] 의 KDoc 에 있다. 여기서는 사유 하나를 문구와
 * 행동으로 옮기기만 한다.
 *
 * 되돌릴 조건이 없는 사유([FeedEmptyReason.NotCollected]·[FeedEmptyReason.OfflineSnapshot])에는 행동을
 * 주지 않는다 — 눌러도 목록이 달라지지 않는 버튼은 기다리라는 안내와 모순된다. 새로고침은 화면 전체에
 * 걸린 당겨서 새로고침이 이미 맡고 있다.
 */
@Composable
private fun FeedEmpty(
    reason: FeedEmptyReason,
    onEvent: (FeedUiEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    when (reason) {
        FeedEmptyReason.NoBoards -> {
            CareerCompassEmptyState(
                title = stringResource(R.string.feed_empty_no_boards_title),
                description = stringResource(R.string.feed_empty_no_boards_description),
                actionText = stringResource(R.string.feed_empty_no_boards_action),
                onActionClick = { onEvent(FeedUiEvent.BoardRegisterSelected) },
                modifier = modifier,
            )
        }

        is FeedEmptyReason.Search -> {
            // 검색어를 지우는 것도 검색어 변경이다 — 입력칸의 글자와 조회 조건이 한 이벤트로만 바뀌게
            // 두어, 지우기 전용 경로가 둘 사이를 어긋나게 만들지 않는다.
            CareerCompassEmptyState(
                title = stringResource(R.string.feed_empty_search_title, reason.query),
                description = stringResource(R.string.feed_empty_search_description),
                actionText = stringResource(R.string.feed_empty_search_action),
                onActionClick = { onEvent(FeedUiEvent.SearchQueryChanged("")) },
                modifier = modifier,
            )
        }

        FeedEmptyReason.Filter -> {
            CareerCompassEmptyState(
                title = stringResource(R.string.feed_empty_filter_title),
                description = stringResource(R.string.feed_empty_filter_description),
                actionText = stringResource(R.string.feed_empty_filter_action),
                onActionClick = { onEvent(FeedUiEvent.FilterResetSelected) },
                modifier = modifier,
            )
        }

        is FeedEmptyReason.NotCollected -> {
            CareerCompassEmptyState(
                title = stringResource(R.string.feed_empty_not_collected_title),
                description =
                    reason.collectNotice?.let { notice ->
                        stringResource(R.string.feed_empty_not_collected_description_with_notice, notice)
                    } ?: stringResource(R.string.feed_empty_not_collected_description),
                actionText = null,
                onActionClick = null,
                modifier = modifier,
            )
        }

        FeedEmptyReason.OfflineSnapshot -> {
            CareerCompassEmptyState(
                title = stringResource(R.string.feed_empty_offline_title),
                description = stringResource(R.string.feed_empty_offline_description),
                actionText = null,
                onActionClick = null,
                modifier = modifier,
            )
        }
    }
}

private fun FeedListingCategory.badgeTone(): CareerCompassBadgeTone =
    when (this) {
        FeedListingCategory.All,
        FeedListingCategory.Employment,
        -> CareerCompassBadgeTone.Brand

        FeedListingCategory.Scholarship -> CareerCompassBadgeTone.Info

        FeedListingCategory.Contest -> CareerCompassBadgeTone.Warning

        FeedListingCategory.ExternalActivity,
        FeedListingCategory.Other,
        -> CareerCompassBadgeTone.Neutral
    }

/** Trigger the next page when this many items (or fewer) remain below the viewport. */
private const val LOAD_MORE_THRESHOLD = 3
