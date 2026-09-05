package com.careercompass.feature.feed.presentation.postingraw

import android.content.ActivityNotFoundException
import android.content.Intent
import android.content.res.Resources
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
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
import androidx.compose.ui.res.stringResource
import androidx.core.net.toUri
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.careercompass.core.model.posting.PostingDetail
import com.careercompass.core.ui.theme.CareerCompassTheme
import com.careercompass.feature.feed.presentation.R
import com.careercompass.feature.feed.presentation.shared.component.FeedLoadingContent
import com.careercompass.feature.feed.presentation.shared.component.FeedTopBar
import kotlinx.coroutines.launch
import java.time.Clock
import java.time.format.DateTimeFormatter

/**
 * 원문 보기 진입점 — 본문이 오기 전·실패 시에는 같은 상단 바 아래에 로딩·오류 상태를 그린다.
 *
 * 실패는 사유별로 [PostingRawFailureContent] 가 갈라 그린다 — 서버 점검(503)은 바로 앞 화면인 공고 상세와
 * 같은 안내를 쓴다(#212).
 */
@Composable
public fun PostingRawScreen(
    onBackClick: () -> Unit,
    onSessionEnded: () -> Unit,
    viewModel: PostingRawViewModel,
    modifier: Modifier = Modifier,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val resources = LocalResources.current
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    val snackbarScope = rememberCoroutineScope()

    val isBackRequested = state.isBackRequested
    LaunchedEffect(isBackRequested) {
        if (isBackRequested) {
            viewModel.onIntent(PostingRawIntent.ConsumeBack)
            onBackClick()
        }
    }
    val sessionEnded = state.sessionEnded
    LaunchedEffect(sessionEnded) {
        if (sessionEnded) {
            viewModel.onIntent(PostingRawIntent.ConsumeSessionEnded)
            onSessionEnded()
        }
    }
    val openUrl = state.openUrl
    LaunchedEffect(openUrl) {
        if (openUrl == null) return@LaunchedEffect
        viewModel.onIntent(PostingRawIntent.ConsumeOpenUrl)
        try {
            context.startActivity(Intent(Intent.ACTION_VIEW, openUrl.toUri()))
        } catch (_: ActivityNotFoundException) {
            snackbarScope.launch { snackbarHostState.showSnackbar(resources.getString(R.string.feed_open_url_failed)) }
        }
    }

    // 웹 주소가 아닌 원본 링크는 ViewModel 이 걸러 낸다. 사용자에게는 열기 실패와 같은 안내다.
    val openUrlRejected = state.openUrlRejected
    LaunchedEffect(openUrlRejected) {
        if (!openUrlRejected) return@LaunchedEffect
        viewModel.onIntent(PostingRawIntent.ConsumeOpenUrlRejected)
        snackbarScope.launch { snackbarHostState.showSnackbar(resources.getString(R.string.feed_open_url_failed)) }
    }

    Box(modifier = modifier.fillMaxSize()) {
        when (val loadState = state.loadState) {
            PostingRawLoadState.Loading -> {
                PostingRawChrome(onBackClick = { viewModel.onIntent(PostingRawIntent.Screen(PostingRawEvent.BackClicked)) }) {
                    FeedLoadingContent(
                        message = stringResource(R.string.feed_posting_detail_loading),
                        modifier = Modifier.fillMaxSize(),
                    )
                }
            }

            is PostingRawLoadState.Failed -> {
                PostingRawChrome(onBackClick = { viewModel.onIntent(PostingRawIntent.Screen(PostingRawEvent.BackClicked)) }) {
                    PostingRawFailureContent(
                        reason = loadState.reason,
                        onRetryClick = { viewModel.onIntent(PostingRawIntent.Retry) },
                    )
                }
            }

            is PostingRawLoadState.Loaded -> {
                val uiState = remember(loadState.detail, resources) { loadState.detail.toRawUiState(resources, viewModel.clock) }
                PostingRawContent(
                    state = uiState,
                    onEvent = { viewModel.onIntent(PostingRawIntent.Screen(it)) },
                )
            }
        }
        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.align(Alignment.BottomCenter),
        )
    }
}

@Composable
private fun PostingRawChrome(
    onBackClick: () -> Unit,
    content: @Composable () -> Unit,
) {
    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .background(CareerCompassTheme.colors.subtleSurface)
                .windowInsetsPadding(WindowInsets.safeDrawing),
    ) {
        FeedTopBar(
            title = stringResource(R.string.feed_posting_raw_title),
            onBackClick = onBackClick,
            actions = null,
        )
        content()
    }
}

/** 본문이 비어 오면 계약 불변식(비어 있지 않음)을 지키려고 안내 문구로 대신한다. */
internal fun PostingDetail.toRawUiState(
    resources: Resources,
    clock: Clock,
): PostingRawUiState =
    PostingRawUiState(
        title = title,
        sourceLabel =
            resources.getString(
                R.string.feed_posting_raw_source,
                board.name,
                collectedAt.atZone(clock.zone).format(RAW_COLLECTED_DATE_FORMAT),
            ),
        originalUrl = url.takeIf { it.isNotBlank() },
        rawContent = rawContent.ifBlank { resources.getString(R.string.feed_posting_raw_empty_content) },
    )

private val RAW_COLLECTED_DATE_FORMAT: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy.MM.dd")
