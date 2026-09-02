package com.cambridge.feature.feed.presentation.board

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
        snackbarScope.launch { snackbarHostState.showSnackbar(message.toLabel(resources)) }
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
        is BoardRegisterMessage.LimitReached -> resources.getString(R.string.feed_board_register_limit_reached, limit)
    }
