package com.cambridge.feature.feed.presentation.board

import android.content.res.Resources
import androidx.activity.compose.BackHandler
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
import androidx.compose.ui.platform.LocalResources
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.cambridge.feature.feed.presentation.R
import kotlinx.coroutines.launch

/** 게시판 등록 진입점. 등록이 끝나면 [onBackClick] 으로 목록에 돌아간다(목록은 재진입 시 다시 읽는다). */
@Composable
public fun BoardRegisterEntry(
    onBackClick: () -> Unit,
    onSessionEnded: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: BoardRegisterViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val resources = LocalResources.current
    val snackbarHostState = remember { SnackbarHostState() }
    val snackbarScope = rememberCoroutineScope()

    // 제출 중 이탈 차단은 상단 화살표만 막아서는 반쪽이다 — 시스템 뒤로가기와 가장자리 제스처가 그대로
    // 남으면 사용자는 늘 쓰던 손짓으로 나가고 요청은 그대로 끊긴다(#146). 두 길을 같은 이벤트로 모아
    // ViewModel 이 한자리에서 판정하게 한다. 제출 중이 아니면 꺼져 있어 평소 뒤로가기는 그대로 동작한다.
    BackHandler(enabled = state.isSubmitting) { viewModel.onEvent(BoardRegisterEvent.BackClicked) }

    val isBackRequested = state.isBackRequested
    LaunchedEffect(isBackRequested) {
        if (isBackRequested) {
            viewModel.onBackConsumed()
            onBackClick()
        }
    }
    val sessionEnded = state.sessionEnded
    LaunchedEffect(sessionEnded) {
        if (sessionEnded) {
            viewModel.onSessionEndedConsumed()
            onSessionEnded()
        }
    }
    val message = state.message
    LaunchedEffect(message) {
        if (message == null) return@LaunchedEffect
        viewModel.onMessageConsumed()
        snackbarScope.launch {
            // 제출 중 뒤로가기는 조급한 사용자가 연달아 누른다. 그대로 두면 안내가 줄을 서서 등록이 끝난
            // 뒤에도 계속 뜨므로, 새 안내가 앞의 안내를 대신하게 한다.
            snackbarHostState.currentSnackbarData?.dismiss()
            snackbarHostState.showSnackbar(message.toLabel(resources))
        }
    }

    val uiState = remember(state, resources) { state.toUiState(resources) }

    Box(modifier = modifier.fillMaxSize()) {
        BoardRegisterScreen(
            state = uiState,
            onEvent = viewModel::onEvent,
        )
        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.align(Alignment.BottomCenter),
        )
    }
}

internal fun BoardRegisterViewState.toUiState(resources: Resources): BoardRegisterUiState =
    BoardRegisterUiState(
        url = url,
        urlError =
            when (urlError) {
                null -> null
                BoardUrlError.Invalid -> resources.getString(R.string.feed_board_register_url_invalid)
                BoardUrlError.Duplicate -> resources.getString(R.string.feed_board_register_url_duplicate)
            },
        detection = detection,
        name = name,
        type = type,
        cycle = cycle,
        isSubmitting = isSubmitting,
    )

internal fun BoardRegisterMessage.toLabel(resources: Resources): String =
    when (this) {
        BoardRegisterMessage.NetworkUnavailable -> resources.getString(R.string.feed_board_network_unavailable)

        BoardRegisterMessage.DetectFailed -> resources.getString(R.string.feed_board_register_detect_failed)

        BoardRegisterMessage.RegisterFailed -> resources.getString(R.string.feed_board_register_failed)

        // 점검 문구는 새로 짓지 않는다 — 화면 한 장을 쓰는 자리(FeedMaintenanceState)와 같은 문장이다.
        BoardRegisterMessage.Maintenance -> resources.getString(R.string.feed_maintenance_title)

        BoardRegisterMessage.SubmitInProgress -> resources.getString(R.string.feed_board_register_submit_in_progress)

        BoardRegisterMessage.AlreadyRegistered -> resources.getString(R.string.feed_board_register_duplicate_notice)

        is BoardRegisterMessage.LimitReached -> resources.getString(R.string.feed_board_register_limit_reached, limit)
    }
