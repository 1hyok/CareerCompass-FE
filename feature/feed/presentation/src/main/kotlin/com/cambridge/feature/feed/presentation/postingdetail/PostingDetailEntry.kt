package com.cambridge.feature.feed.presentation.postingdetail

import android.content.Intent
import android.content.res.Resources
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.cambridge.core.model.posting.Suitability
import com.cambridge.feature.feed.presentation.R
import com.cambridge.feature.feed.presentation.shared.model.FeedFailureReason
import com.cambridge.feature.feed.presentation.shared.model.SuitabilityJudgement
import com.cambridge.feature.feed.presentation.shared.model.judgeSuitability
import com.cambridge.feature.feed.presentation.shared.util.toDetailUiModel
import com.cambridge.feature.feed.presentation.shared.util.toSuitabilityUiModel
import kotlinx.coroutines.launch
import java.time.Clock

/**
 * 공고 상세 진입점 — 도메인 상세를 [PostingDetailScreen] 계약으로 옮기고 공유·이동·스낵바 신호를 소비한다.
 *
 * 유사 공고 선택은 [onPostingClick] 으로 상세를 하나 더 쌓고, 프로필 입력 안내는 [onProfileClick] 으로 앱 셸에 맡긴다.
 * 서버 점검은 한 줄 오류 문구가 아니라 [PostingDetailContentState.Maintenance] 로 옮겨 전용 안내 화면이 그려진다.
 */
@Composable
public fun PostingDetailEntry(
    onBackClick: () -> Unit,
    onPostingClick: (Long) -> Unit,
    onRawClick: (Long) -> Unit,
    onProfileClick: () -> Unit,
    onSessionEnded: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: PostingDetailViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val resources = LocalResources.current
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    val snackbarScope = rememberCoroutineScope()

    val pendingNavigation = state.pendingNavigation
    LaunchedEffect(pendingNavigation) {
        if (pendingNavigation == null) return@LaunchedEffect
        viewModel.onNavigationConsumed()
        when (pendingNavigation) {
            PostingDetailDestination.Back -> onBackClick()
            is PostingDetailDestination.Raw -> onRawClick(pendingNavigation.postingId)
            is PostingDetailDestination.Posting -> onPostingClick(pendingNavigation.postingId)
            PostingDetailDestination.Profile -> onProfileClick()
        }
    }
    val sessionEnded = state.sessionEnded
    LaunchedEffect(sessionEnded) {
        if (sessionEnded) {
            viewModel.onSessionEndedConsumed()
            onSessionEnded()
        }
    }
    val shareRequest = state.shareRequest
    LaunchedEffect(shareRequest) {
        if (shareRequest == null) return@LaunchedEffect
        viewModel.onShareConsumed()
        val sendIntent =
            Intent(Intent.ACTION_SEND).apply {
                type = SHARE_MIME_TYPE
                putExtra(Intent.EXTRA_SUBJECT, shareRequest.title)
                putExtra(
                    Intent.EXTRA_TEXT,
                    resources.getString(R.string.feed_posting_detail_share_text, shareRequest.title, shareRequest.url),
                )
            }
        context.startActivity(Intent.createChooser(sendIntent, resources.getString(R.string.feed_posting_detail_share_chooser)))
    }
    val message = state.message
    LaunchedEffect(message) {
        if (message == null) return@LaunchedEffect
        viewModel.onMessageConsumed()
        val messageRes =
            when (message) {
                PostingDetailMessage.BookmarkFailed -> R.string.feed_bookmark_failed
                PostingDetailMessage.DraftComingSoon -> R.string.feed_posting_detail_draft_coming_soon
            }
        snackbarScope.launch { snackbarHostState.showSnackbar(resources.getString(messageRes)) }
    }

    val uiState =
        remember(state.loadState, state.profile, resources) {
            state.toUiState(resources, viewModel.clock)
        }

    Box(modifier = modifier.fillMaxSize()) {
        PostingDetailScreen(
            state = uiState,
            onEvent = viewModel::onEvent,
        )
        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.align(Alignment.BottomCenter),
        )
    }
}

internal fun PostingDetailViewState.toUiState(
    resources: Resources,
    clock: Clock,
): PostingDetailUiState =
    PostingDetailUiState(
        content =
            when (val loadState = loadState) {
                PostingDetailLoadState.Loading -> {
                    PostingDetailContentState.Loading
                }

                is PostingDetailLoadState.Failed -> {
                    when (loadState.reason) {
                        FeedFailureReason.Maintenance -> {
                            PostingDetailContentState.Maintenance
                        }

                        FeedFailureReason.NetworkUnavailable -> {
                            PostingDetailContentState.Error(resources.getString(R.string.feed_posting_detail_error_network))
                        }

                        FeedFailureReason.Generic -> {
                            PostingDetailContentState.Error(resources.getString(R.string.feed_posting_detail_error_generic))
                        }
                    }
                }

                is PostingDetailLoadState.Loaded -> {
                    PostingDetailContentState.Loaded(
                        loadState.detail.toDetailUiModel(
                            resources = resources,
                            clock = clock,
                            suitability =
                                toSuitabilityState(
                                    judgement = judgeSuitability(hasScore = loadState.detail.suitability != null, profile = profile),
                                    suitability = loadState.detail.suitability,
                                    resources = resources,
                                ),
                            profile = profile,
                        ),
                    )
                }
            },
    )

/** 판정 → 카드 상태. 「준비됨」인데 점수가 없는 모순은 「분석 중」으로 접는다. */
internal fun toSuitabilityState(
    judgement: SuitabilityJudgement,
    suitability: Suitability?,
    resources: Resources,
): PostingSuitabilityState =
    when (judgement) {
        SuitabilityJudgement.ProfileIncomplete -> {
            PostingSuitabilityState.ProfileIncomplete
        }

        SuitabilityJudgement.Analyzing -> {
            PostingSuitabilityState.Analyzing
        }

        SuitabilityJudgement.Ready -> {
            suitability?.let { PostingSuitabilityState.Ready(it.toSuitabilityUiModel(resources)) }
                ?: PostingSuitabilityState.Analyzing
        }
    }

private const val SHARE_MIME_TYPE = "text/plain"
