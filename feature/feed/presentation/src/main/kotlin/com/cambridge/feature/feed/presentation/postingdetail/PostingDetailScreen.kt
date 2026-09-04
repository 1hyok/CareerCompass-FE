package com.cambridge.feature.feed.presentation.postingdetail

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cambridge.core.ui.component.CareerCompassAnalyzingState
import com.cambridge.core.ui.component.CareerCompassBadge
import com.cambridge.core.ui.component.CareerCompassBadgeTone
import com.cambridge.core.ui.component.CareerCompassButton
import com.cambridge.core.ui.component.CareerCompassButtonSize
import com.cambridge.core.ui.component.CareerCompassButtonVariant
import com.cambridge.core.ui.component.CareerCompassFailureState
import com.cambridge.core.ui.component.CareerCompassNetworkErrorState
import com.cambridge.core.ui.component.CareerCompassStatePresentation
import com.cambridge.core.ui.icon.CareerCompassIcons
import com.cambridge.core.ui.theme.CareerCompassTheme
import com.cambridge.feature.feed.presentation.FeedListingUiModel
import com.cambridge.feature.feed.presentation.R
import com.cambridge.feature.feed.presentation.postingdetail.component.SimilarPostingCard
import com.cambridge.feature.feed.presentation.postingdetail.component.SuitabilityBreakdownRow
import com.cambridge.feature.feed.presentation.postingdetail.component.SuitabilityGauge
import com.cambridge.feature.feed.presentation.postingdetail.component.badgeTone
import com.cambridge.feature.feed.presentation.shared.component.FEED_ICON_SIZE
import com.cambridge.feature.feed.presentation.shared.component.FEED_INLINE_ICON_SIZE
import com.cambridge.feature.feed.presentation.shared.component.FeedCard
import com.cambridge.feature.feed.presentation.shared.component.FeedIconButton
import com.cambridge.feature.feed.presentation.shared.component.FeedLoadingContent
import com.cambridge.feature.feed.presentation.shared.component.FeedMaintenanceState
import com.cambridge.feature.feed.presentation.shared.component.FeedSectionTitle
import com.cambridge.feature.feed.presentation.shared.component.FeedTopBar

/** Stateless posting detail screen matching the CareerCompass "공고 상세" design (spec F3-3). */
@Composable
public fun PostingDetailScreen(
    state: PostingDetailUiState,
    onEvent: (PostingDetailEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    val loadedPosting = (state.content as? PostingDetailContentState.Loaded)?.posting
    val topBarActions: (@Composable RowScope.() -> Unit)? =
        if (loadedPosting == null) {
            null
        } else {
            {
                PostingDetailTopActions(
                    isBookmarked = loadedPosting.isBookmarked,
                    onEvent = onEvent,
                )
            }
        }

    Column(
        modifier =
            modifier
                .fillMaxSize()
                .background(CareerCompassTheme.colors.subtleSurface)
                .windowInsetsPadding(WindowInsets.safeDrawing),
    ) {
        FeedTopBar(
            title = stringResource(R.string.feed_posting_detail_title),
            onBackClick = { onEvent(PostingDetailEvent.BackClicked) },
            actions = topBarActions,
        )
        when (val content = state.content) {
            PostingDetailContentState.Loading -> {
                FeedLoadingContent(
                    message = stringResource(R.string.feed_posting_detail_loading),
                    modifier = Modifier.weight(1f),
                )
            }

            PostingDetailContentState.NetworkUnavailable -> {
                // 상세는 스냅샷을 저장하지 않아 오프라인 경로가 없다.
                CareerCompassNetworkErrorState(
                    onRetryClick = { onEvent(PostingDetailEvent.RetryClicked) },
                    onOfflineClick = null,
                    modifier = Modifier.weight(1f),
                )
            }

            is PostingDetailContentState.Error -> {
                // 실패 전용 부품(#222). 재시도 버튼은 표의 판정(isRetryable)을 따른다 — 눌러도 같은 답이 오는
                // 실패에 버튼을 주면 사용자는 누르고 같은 실패를 다시 만난다.
                val onRetry: (() -> Unit)? =
                    if (content.isRetryable) ({ onEvent(PostingDetailEvent.RetryClicked) }) else null
                CareerCompassFailureState(
                    title = content.title,
                    description = content.description,
                    actionText = stringResource(R.string.feed_posting_detail_retry).takeIf { content.isRetryable },
                    onActionClick = onRetry,
                    modifier = Modifier.weight(1f),
                )
            }

            PostingDetailContentState.Maintenance -> {
                // 상세는 스냅샷을 저장하지 않아 오프라인 경로가 없다.
                FeedMaintenanceState(
                    onRetryClick = { onEvent(PostingDetailEvent.RetryClicked) },
                    onOfflineClick = null,
                    modifier = Modifier.weight(1f),
                )
            }

            is PostingDetailContentState.Loaded -> {
                PostingDetailBody(
                    posting = content.posting,
                    onEvent = onEvent,
                    modifier = Modifier.weight(1f),
                )
                PostingDetailBottomBar(
                    canCreateDraft = content.posting.canCreateDraft,
                    onEvent = onEvent,
                )
            }
        }
    }
}

@Composable
private fun PostingDetailTopActions(
    isBookmarked: Boolean,
    onEvent: (PostingDetailEvent) -> Unit,
) {
    PostingBookmarkToggle(
        bookmarked = isBookmarked,
        onClick = { onEvent(PostingDetailEvent.BookmarkToggled) },
    )
    FeedIconButton(
        icon = CareerCompassIcons.Share,
        contentDescription = stringResource(R.string.feed_posting_detail_share),
        onClick = { onEvent(PostingDetailEvent.ShareClicked) },
    )
}

@Composable
private fun PostingBookmarkToggle(
    bookmarked: Boolean,
    onClick: () -> Unit,
) {
    val colors = CareerCompassTheme.colors
    val bookmarkDescription = stringResource(R.string.feed_posting_detail_bookmark_content_description)
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
            tint = if (bookmarked) colors.primaryEmphasis else colors.onSurface,
        )
    }
}

@Composable
private fun PostingDetailBody(
    posting: PostingDetailUiModel,
    onEvent: (PostingDetailEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    val spacing = CareerCompassTheme.spacing

    Column(
        modifier =
            modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = spacing.large, vertical = spacing.small),
        verticalArrangement = Arrangement.spacedBy(spacing.medium),
    ) {
        PostingHeaderCard(posting = posting)
        PostingSuitabilityCard(
            suitability = posting.suitability,
            onCompleteProfileClick = { onEvent(PostingDetailEvent.CompleteProfileClicked) },
            onRecheckClick = { onEvent(PostingDetailEvent.SuitabilityRecheckClicked) },
        )
        if (posting.keywords.isNotEmpty()) {
            PostingKeywordsCard(keywords = posting.keywords)
        }
        if (posting.qualifications.isNotEmpty()) {
            PostingBulletCard(
                title = stringResource(R.string.feed_posting_detail_qualifications_title),
                items = posting.qualifications,
            )
        }
        if (posting.preferences.isNotEmpty()) {
            PostingBulletCard(
                title = stringResource(R.string.feed_posting_detail_preferences_title),
                items = posting.preferences,
            )
        }
        PostingFormQuestionsCard(questions = posting.formQuestions)
        if (posting.similarPostings.isNotEmpty()) {
            PostingSimilarSection(
                similarPostings = posting.similarPostings,
                onEvent = onEvent,
            )
        }
        Spacer(modifier = Modifier.height(spacing.small))
    }
}

@Composable
private fun PostingHeaderCard(posting: PostingDetailUiModel) {
    val colors = CareerCompassTheme.colors

    FeedCard(onClick = null) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(CareerCompassTheme.spacing.xSmall),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            CareerCompassBadge(
                label = posting.categoryLabel,
                tone = posting.category.badgeTone(),
            )
            CareerCompassBadge(
                label = posting.sourceLabel,
                tone = CareerCompassBadgeTone.Neutral,
            )
        }
        Text(
            text = posting.title,
            modifier = Modifier.semantics { heading() },
            color = colors.onSurface,
            style = CareerCompassTheme.typography.headline2,
        )
        Text(
            // 마감 임박을 색으로만 말하지 않는다. 피드 카드는 「D-2」라는 숫자가 임박을 지지만 상세는
            // 절대 날짜라, 색을 못 보면 임박했다는 사실 자체가 사라진다(이슈 #205) — 문구로 한 번 더 적는다.
            text =
                stringResource(
                    if (posting.isDeadlineUrgent) {
                        R.string.feed_posting_detail_collected_and_deadline_urgent
                    } else {
                        R.string.feed_posting_detail_collected_and_deadline
                    },
                    posting.collectedAtLabel,
                    posting.deadlineLabel,
                ),
            color = if (posting.isDeadlineUrgent) colors.error else colors.mutedContent,
            style =
                CareerCompassTheme.typography.caption.copy(
                    fontSize = 12.sp,
                    lineHeight = 18.sp,
                    fontWeight = FontWeight.SemiBold,
                ),
        )
    }
}

/**
 * 적합도 카드 — 세 판정([PostingSuitabilityState])이 세 모양이다. 「분석 중」과 「프로필 미입력」을 섞지 않는다:
 * 뒤쪽은 사용자가 할 일이 있고 앞쪽은 기다리는 수밖에 없다(#100·#221).
 */
@Composable
private fun PostingSuitabilityCard(
    suitability: PostingSuitabilityState,
    onCompleteProfileClick: () -> Unit,
    onRecheckClick: () -> Unit,
) {
    val colors = CareerCompassTheme.colors
    val spacing = CareerCompassTheme.spacing

    FeedCard(onClick = null) {
        FeedSectionTitle(text = stringResource(R.string.feed_posting_detail_suitability_title))
        when (suitability) {
            PostingSuitabilityState.ProfileIncomplete -> {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(spacing.medium),
                ) {
                    Text(
                        text = stringResource(R.string.feed_posting_detail_profile_incomplete),
                        color = colors.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        style = CareerCompassTheme.typography.bodyMedium,
                    )
                    CareerCompassButton(
                        text = stringResource(R.string.feed_posting_detail_complete_profile),
                        onClick = onCompleteProfileClick,
                    )
                }
            }

            is PostingSuitabilityState.Analyzing -> {
                if (suitability.isAutoRecheckExhausted) {
                    PostingSuitabilityPending(onRecheckClick = onRecheckClick)
                } else {
                    // Figma 09 「분석 중」을 카드 안에 끼운다 — 화면을 통째로 덮으면 제목·원문 보기가 사라진다.
                    // 기다리는 상태라 행동 버튼이 없다(엣지 상태 §3) — 화면이 스스로 다시 묻는다.
                    CareerCompassAnalyzingState(
                        title = stringResource(R.string.feed_posting_detail_analyzing),
                        description = stringResource(R.string.feed_posting_detail_analyzing_description),
                        progress = null,
                        progressLabel = null,
                        presentation = CareerCompassStatePresentation.Inline,
                    )
                }
            }

            is PostingSuitabilityState.Ready -> {
                PostingSuitabilityReady(suitability = suitability.suitability)
            }
        }
    }
}

/**
 * 자동 재조회를 다 쓴 뒤 — 진행 표시를 거둔다. 도는 인디케이터는 「무언가 하고 있다」는 뜻인데 이제 아무것도
 * 하지 않는다.
 *
 * 「다시 확인」은 실제로 상태를 바꾸는 버튼이다(누르면 한 번 더 묻는다). 영구 실패를 나타낼 계약이 없으므로
 * (#200) 문구는 실패도 성공도 약속하지 않고, 하단 바에 늘 있는 「원문 보기」를 대안으로 가리킨다.
 */
@Composable
private fun PostingSuitabilityPending(onRecheckClick: () -> Unit) {
    val colors = CareerCompassTheme.colors
    val spacing = CareerCompassTheme.spacing

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(spacing.medium),
    ) {
        Text(
            text = stringResource(R.string.feed_posting_detail_analysis_pending_title),
            color = colors.onSurface,
            textAlign = TextAlign.Center,
            style = CareerCompassTheme.typography.headline4,
        )
        Text(
            text = stringResource(R.string.feed_posting_detail_analysis_pending_description),
            color = colors.onSurfaceVariant,
            textAlign = TextAlign.Center,
            style = CareerCompassTheme.typography.bodyMedium,
        )
        CareerCompassButton(
            text = stringResource(R.string.feed_posting_detail_analysis_recheck),
            onClick = onRecheckClick,
            variant = CareerCompassButtonVariant.Secondary,
        )
    }
}

@Composable
private fun PostingSuitabilityReady(suitability: SuitabilityUiModel) {
    val colors = CareerCompassTheme.colors
    val spacing = CareerCompassTheme.spacing

    SuitabilityGauge(
        score = suitability.score,
        levelLabel = suitability.levelLabel,
        level = suitability.level,
    )
    if (suitability.breakdown.isEmpty()) {
        // 총점은 나왔는데 축 분해가 비어 온 경우. 「모름」 이지 「미충족」 이 아니므로 0점짜리 축 4개를
        // 그려서는 안 된다 — 없는 것은 없다고만 적는다. 축이 일부만 오면 온 축만 그리고 나머지는
        // 자리 자체를 만들지 않는 것도 같은 이유다.
        Text(
            text = stringResource(R.string.feed_posting_detail_breakdown_unavailable),
            color = colors.mutedContent,
            style = CareerCompassTheme.typography.caption,
        )
    } else {
        Column(verticalArrangement = Arrangement.spacedBy(spacing.medium)) {
            suitability.breakdown.forEach { axis ->
                SuitabilityBreakdownRow(axis = axis)
            }
        }
    }
    suitability.strengthComment?.let { comment ->
        PostingCommentBox(
            icon = stringResource(R.string.feed_icon_strength),
            title = stringResource(R.string.feed_posting_detail_strength_title),
            comment = comment,
            containerColor = colors.successContainer,
            titleColor = colors.onSuccessContainer,
        )
    }
    suitability.weaknessComment?.let { comment ->
        PostingCommentBox(
            icon = stringResource(R.string.feed_icon_weakness),
            title = stringResource(R.string.feed_posting_detail_weakness_title),
            comment = comment,
            containerColor = colors.warningContainer,
            titleColor = colors.onWarningContainer,
        )
    }
}

@Composable
private fun PostingCommentBox(
    icon: String,
    title: String,
    comment: String,
    containerColor: Color,
    titleColor: Color,
) {
    val spacing = CareerCompassTheme.spacing

    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .background(containerColor, CareerCompassTheme.shapes.largeControl)
                .padding(spacing.medium),
        verticalArrangement = Arrangement.spacedBy(spacing.xxSmall),
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(spacing.xxSmall),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = icon,
                modifier = Modifier.clearAndSetSemantics {},
                style = CareerCompassTheme.typography.caption,
            )
            Text(
                text = title,
                color = titleColor,
                style =
                    CareerCompassTheme.typography.caption.copy(
                        fontSize = 12.sp,
                        lineHeight = 18.sp,
                        fontWeight = FontWeight.SemiBold,
                    ),
            )
        }
        Text(
            text = comment,
            color = CareerCompassTheme.colors.onSurface,
            style = CareerCompassTheme.typography.bodyMedium,
        )
    }
}

@Composable
private fun PostingKeywordsCard(keywords: List<String>) {
    val spacing = CareerCompassTheme.spacing

    FeedCard(onClick = null) {
        FeedSectionTitle(text = stringResource(R.string.feed_posting_detail_keywords_title))
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(spacing.xSmall),
            verticalArrangement = Arrangement.spacedBy(spacing.xSmall),
        ) {
            keywords.forEach { keyword ->
                CareerCompassBadge(
                    label = keyword,
                    tone = CareerCompassBadgeTone.Neutral,
                )
            }
        }
    }
}

@Composable
private fun PostingBulletCard(
    title: String,
    items: List<String>,
) {
    val colors = CareerCompassTheme.colors
    val spacing = CareerCompassTheme.spacing

    FeedCard(onClick = null) {
        FeedSectionTitle(text = title)
        Column(verticalArrangement = Arrangement.spacedBy(spacing.xSmall)) {
            items.forEach { item ->
                Row(horizontalArrangement = Arrangement.spacedBy(spacing.small)) {
                    Icon(
                        imageVector = CareerCompassIcons.Bullet,
                        contentDescription = null,
                        modifier = Modifier.size(FEED_INLINE_ICON_SIZE),
                        tint = colors.mutedContent,
                    )
                    Text(
                        text = item,
                        color = colors.onSurface,
                        style = CareerCompassTheme.typography.bodyMedium,
                    )
                }
            }
        }
    }
}

@Composable
private fun PostingFormQuestionsCard(questions: List<PostingFormQuestionUiModel>) {
    val colors = CareerCompassTheme.colors
    val spacing = CareerCompassTheme.spacing

    FeedCard(onClick = null) {
        FeedSectionTitle(text = stringResource(R.string.feed_posting_detail_form_questions_title))
        if (questions.isEmpty()) {
            Text(
                text = stringResource(R.string.feed_posting_detail_form_questions_empty),
                color = colors.mutedContent,
                style = CareerCompassTheme.typography.caption,
            )
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(spacing.small)) {
                questions.forEach { question ->
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(
                            text =
                                stringResource(
                                    R.string.feed_posting_detail_form_question,
                                    question.order,
                                    question.question,
                                ),
                            color = colors.onSurface,
                            style = CareerCompassTheme.typography.bodyMedium,
                        )
                        question.maxCharsLabel?.let { maxCharsLabel ->
                            Text(
                                text = maxCharsLabel,
                                color = colors.mutedContent,
                                style = CareerCompassTheme.typography.caption,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PostingSimilarSection(
    similarPostings: List<FeedListingUiModel>,
    onEvent: (PostingDetailEvent) -> Unit,
) {
    val spacing = CareerCompassTheme.spacing

    Column(verticalArrangement = Arrangement.spacedBy(spacing.small)) {
        FeedSectionTitle(
            text = stringResource(R.string.feed_posting_detail_similar_title),
            modifier = Modifier.padding(top = spacing.xxSmall),
        )
        similarPostings.forEach { listing ->
            SimilarPostingCard(
                listing = listing,
                onClick = { onEvent(PostingDetailEvent.SimilarPostingSelected(listing.id)) },
            )
        }
    }
}

@Composable
private fun PostingDetailBottomBar(
    canCreateDraft: Boolean,
    onEvent: (PostingDetailEvent) -> Unit,
) {
    val spacing = CareerCompassTheme.spacing

    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .background(CareerCompassTheme.colors.subtleSurface)
                .padding(
                    start = spacing.large,
                    top = spacing.medium,
                    end = spacing.large,
                    bottom = spacing.large,
                ),
        horizontalArrangement = Arrangement.spacedBy(spacing.small),
    ) {
        CareerCompassButton(
            text = stringResource(R.string.feed_posting_detail_view_original),
            onClick = { onEvent(PostingDetailEvent.ViewOriginalClicked) },
            modifier = Modifier.weight(VIEW_ORIGINAL_WEIGHT),
            variant = CareerCompassButtonVariant.Secondary,
            size = CareerCompassButtonSize.Large,
        )
        if (canCreateDraft) {
            CareerCompassButton(
                text = stringResource(R.string.feed_posting_detail_create_draft),
                onClick = { onEvent(PostingDetailEvent.CreateDraftClicked) },
                modifier = Modifier.weight(CREATE_DRAFT_WEIGHT),
                variant = CareerCompassButtonVariant.Primary,
                size = CareerCompassButtonSize.Large,
            )
        }
    }
}

private const val VIEW_ORIGINAL_WEIGHT = 1f
private const val CREATE_DRAFT_WEIGHT = 1.4f
