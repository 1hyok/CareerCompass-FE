package com.careercompass.feature.feed.presentation.board

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.careercompass.core.ui.component.CareerCompassButton
import com.careercompass.core.ui.component.CareerCompassButtonSize
import com.careercompass.core.ui.component.CareerCompassButtonVariant
import com.careercompass.core.ui.component.CareerCompassTextField
import com.careercompass.core.ui.theme.CareerCompassTheme
import com.careercompass.feature.feed.presentation.R
import com.careercompass.feature.feed.presentation.shared.component.FeedCard
import com.careercompass.feature.feed.presentation.shared.component.FeedChoiceTag
import com.careercompass.feature.feed.presentation.shared.component.FeedMaintenanceNotice
import com.careercompass.feature.feed.presentation.shared.component.FeedSectionTitle
import com.careercompass.feature.feed.presentation.shared.component.FeedTopBar

/** Stateless board registration screen: URL → structure detection → preview → name/type/cycle (spec F2-1). */
@Composable
public fun BoardRegisterContent(
    state: BoardRegisterUiState,
    onEvent: (BoardRegisterEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = CareerCompassTheme.colors
    val spacing = CareerCompassTheme.spacing

    Column(
        modifier =
            modifier
                .fillMaxSize()
                .background(colors.subtleSurface)
                .windowInsetsPadding(WindowInsets.safeDrawing),
    ) {
        FeedTopBar(
            title = stringResource(R.string.feed_board_register_title),
            onBackClick = { onEvent(BoardRegisterEvent.BackClicked) },
            actions = null,
        )
        Column(
            modifier =
                Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = spacing.large, vertical = spacing.small),
            verticalArrangement = Arrangement.spacedBy(spacing.large),
        ) {
            BoardRegisterInfoCard()
            CareerCompassTextField(
                value = state.url,
                onValueChange = { onEvent(BoardRegisterEvent.UrlChanged(it)) },
                label = stringResource(R.string.feed_board_register_url_label),
                placeholder = stringResource(R.string.feed_board_register_url_placeholder),
                errorMessage = state.urlError,
                isError = state.urlError != null,
                enabled = !state.isSubmitting,
                keyboardOptions =
                    KeyboardOptions(
                        keyboardType = KeyboardType.Uri,
                        imeAction = ImeAction.Done,
                    ),
            )
            CareerCompassButton(
                text = stringResource(R.string.feed_board_register_detect),
                onClick = { onEvent(BoardRegisterEvent.DetectClicked) },
                modifier = Modifier.fillMaxWidth(),
                variant = CareerCompassButtonVariant.Primary,
                enabled = state.isDetectEnabled,
            )
            when (val detection = state.detection) {
                BoardDetectionState.Idle -> {
                    Unit
                }

                BoardDetectionState.Detecting -> {
                    BoardDetectingRow()
                }

                BoardDetectionState.TimedOut -> {
                    BoardDetectionTimedOutBox(
                        retryEnabled = state.isDetectEnabled,
                        onRetryClick = { onEvent(BoardRegisterEvent.DetectClicked) },
                    )
                }

                // 점검은 재시도 버튼 없이 알리기만 한다 — 서버가 돌아와야 답이 달라진다. 그래도 막다른 길은
                // 아니다: 위의 「구조 분석하기」가 그대로 눌린다.
                BoardDetectionState.Maintenance -> {
                    FeedMaintenanceNotice()
                }

                is BoardDetectionState.Failed -> {
                    BoardDetectionFailedBox(
                        reason = detection.reason,
                        retryEnabled = state.isDetectEnabled,
                        onRetryClick = { onEvent(BoardRegisterEvent.DetectClicked) },
                    )
                }

                is BoardDetectionState.Success -> {
                    BoardDetectionPreviewCard(detection = detection)
                    BoardRegisterForm(state = state, onEvent = onEvent)
                }
            }
        }
        if (state.detection is BoardDetectionState.Success) {
            Column(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .background(colors.subtleSurface)
                        .padding(
                            start = spacing.large,
                            top = spacing.medium,
                            end = spacing.large,
                            bottom = spacing.large,
                        ),
                verticalArrangement = Arrangement.spacedBy(spacing.small),
            ) {
                if (state.isSubmitting) {
                    BoardRegisterSubmittingRow()
                }
                CareerCompassButton(
                    text = stringResource(R.string.feed_board_register_submit),
                    onClick = { onEvent(BoardRegisterEvent.RegisterClicked) },
                    modifier = Modifier.fillMaxWidth(),
                    variant = CareerCompassButtonVariant.Primary,
                    size = CareerCompassButtonSize.Large,
                    enabled = state.isRegisterEnabled,
                )
            }
        }
    }
}

@Composable
private fun BoardRegisterInfoCard() {
    val colors = CareerCompassTheme.colors
    val spacing = CareerCompassTheme.spacing

    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .background(colors.primaryContainer, CareerCompassTheme.shapes.largeControl)
                .padding(spacing.large),
        verticalArrangement = Arrangement.spacedBy(spacing.xxSmall),
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(spacing.xSmall),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(R.string.feed_icon_sparkles),
                modifier = Modifier.clearAndSetSemantics {},
                style = CareerCompassTheme.typography.bodyMedium,
            )
            Text(
                text = stringResource(R.string.feed_board_register_info_title),
                color = colors.onPrimaryContainer,
                style = CareerCompassTheme.typography.headline4,
            )
        }
        Text(
            text = stringResource(R.string.feed_board_register_info_description),
            color = colors.onPrimaryContainer,
            style = CareerCompassTheme.typography.bodyMedium,
        )
    }
}

/**
 * 감지 진행 표시.
 *
 * 외부 사이트를 크롤링하는 호출이라 수십 초가 예사다(#134). 얼마나 걸릴지 말해 주지 않으면 사용자가 멈춘
 * 줄 알고 화면을 떠나거나 다시 누르므로, 진행 문구 아래에 걸리는 시간을 함께 적는다.
 */
@Composable
private fun BoardDetectingRow() {
    val colors = CareerCompassTheme.colors
    val spacing = CareerCompassTheme.spacing

    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .semantics(mergeDescendants = true) { liveRegion = LiveRegionMode.Polite },
        horizontalArrangement = Arrangement.spacedBy(spacing.small),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        CircularProgressIndicator(
            modifier = Modifier.size(20.dp),
            color = colors.primaryEmphasis,
            strokeWidth = 2.dp,
        )
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                text = stringResource(R.string.feed_board_register_detecting),
                color = colors.onSurfaceVariant,
                style = CareerCompassTheme.typography.bodyMedium,
            )
            Text(
                text = stringResource(R.string.feed_board_register_detecting_hint),
                color = colors.mutedContent,
                style = CareerCompassTheme.typography.caption,
            )
        }
    }
}

/**
 * 등록 제출 진행 표시.
 *
 * **감지 표시([BoardDetectingRow])와 자리를 나눠 쓰지 않는다.** 감지 표시는 스크롤되는 본문 위쪽에 있고,
 * 「등록하기」를 누른 사용자의 눈은 화면 맨 아래 버튼에 있다. 본문에 세우면 미리보기·폼을 지나 위로
 * 올려야 보이고, 그 사이 사용자는 아무 반응이 없다고 읽는다(#146). 그래서 누른 버튼 **바로 위**,
 * 스크롤되지 않는 하단 영역에 둔다.
 *
 * 문구를 두 줄로 나눈 것도 같은 이유다 — 둘째 줄이 「끝나면 목록으로 돌아간다」를 미리 알려, 멈춘 줄
 * 알고 뒤로가기를 누르는 일을 줄인다. 큰 글꼴(fontScale 2.0)에서는 글자가 잘리는 대신 줄이 늘어나고,
 * 본문이 `weight(1f)` 로 줄어 버튼은 그대로 보인다.
 */
@Composable
private fun BoardRegisterSubmittingRow() {
    val colors = CareerCompassTheme.colors
    val spacing = CareerCompassTheme.spacing

    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .semantics(mergeDescendants = true) { liveRegion = LiveRegionMode.Polite },
        horizontalArrangement = Arrangement.spacedBy(spacing.small),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        CircularProgressIndicator(
            modifier = Modifier.size(20.dp),
            color = colors.primaryEmphasis,
            strokeWidth = 2.dp,
        )
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Text(
                text = stringResource(R.string.feed_board_register_submitting),
                color = colors.onSurfaceVariant,
                style = CareerCompassTheme.typography.bodyMedium,
            )
            Text(
                text = stringResource(R.string.feed_board_register_submitting_hint),
                color = colors.mutedContent,
                style = CareerCompassTheme.typography.caption,
            )
        }
    }
}

/**
 * 타임아웃 안내 — 실패([BoardDetectionFailedBox])와 달리 경고 톤이다.
 *
 * 같은 오류 상자에 담으면 사용자가 사이트가 지원되지 않는다고 읽는다. 여기서 알려야 할 것은 「우리가
 * 기다리기를 그만뒀다」와 「다시 시도할 수 있다」뿐이다.
 */
@Composable
private fun BoardDetectionTimedOutBox(
    retryEnabled: Boolean,
    onRetryClick: () -> Unit,
) {
    val colors = CareerCompassTheme.colors
    val spacing = CareerCompassTheme.spacing

    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .background(colors.warningContainer, CareerCompassTheme.shapes.largeControl)
                .padding(spacing.large),
        verticalArrangement = Arrangement.spacedBy(spacing.medium),
    ) {
        Column(
            modifier = Modifier.semantics(mergeDescendants = true) { liveRegion = LiveRegionMode.Polite },
            verticalArrangement = Arrangement.spacedBy(spacing.xxSmall),
        ) {
            Text(
                text = stringResource(R.string.feed_board_detect_timeout_title),
                color = colors.onWarningContainer,
                style = CareerCompassTheme.typography.headline4,
            )
            Text(
                text = stringResource(R.string.feed_board_detect_timeout_description),
                color = colors.onWarningContainer,
                style = CareerCompassTheme.typography.bodyMedium,
            )
        }
        CareerCompassButton(
            text = stringResource(R.string.feed_board_register_retry),
            onClick = onRetryClick,
            variant = CareerCompassButtonVariant.Secondary,
            size = CareerCompassButtonSize.Small,
            enabled = retryEnabled,
        )
    }
}

@Composable
private fun BoardDetectionFailedBox(
    reason: BoardDetectionFailure,
    retryEnabled: Boolean,
    onRetryClick: () -> Unit,
) {
    val colors = CareerCompassTheme.colors
    val spacing = CareerCompassTheme.spacing

    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .background(colors.errorContainer, CareerCompassTheme.shapes.largeControl)
                .padding(spacing.large),
        verticalArrangement = Arrangement.spacedBy(spacing.medium),
    ) {
        Text(
            text = stringResource(reason.messageRes()),
            modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
            color = colors.onErrorContainer,
            style = CareerCompassTheme.typography.bodyMedium,
        )
        // 재시도해도 같은 답이 오는 사유에는 버튼을 주지 않는다([BoardDetectionFailure.isRetryable], #204).
        if (reason.isRetryable) {
            CareerCompassButton(
                text = stringResource(R.string.feed_board_register_retry),
                onClick = onRetryClick,
                variant = CareerCompassButtonVariant.Secondary,
                size = CareerCompassButtonSize.Small,
                enabled = retryEnabled,
            )
        }
    }
}

@Composable
private fun BoardDetectionPreviewCard(detection: BoardDetectionState.Success) {
    val colors = CareerCompassTheme.colors
    val spacing = CareerCompassTheme.spacing

    FeedCard(onClick = null) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(spacing.xSmall),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(R.string.feed_icon_detect_success),
                modifier = Modifier.clearAndSetSemantics {},
                style = CareerCompassTheme.typography.bodyMedium,
            )
            FeedSectionTitle(
                text =
                    stringResource(
                        R.string.feed_board_register_detect_success,
                        detection.preview.size,
                    ),
            )
        }
        Column(verticalArrangement = Arrangement.spacedBy(spacing.small)) {
            detection.preview.forEach { item ->
                Column(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .background(colors.subtleSurface, CareerCompassTheme.shapes.control)
                            .padding(horizontal = spacing.medium, vertical = spacing.small),
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                ) {
                    Text(
                        text = item.title,
                        color = colors.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        style = CareerCompassTheme.typography.bodyMedium,
                    )
                    item.dateLabel?.let { dateLabel ->
                        Text(
                            text = dateLabel,
                            color = colors.mutedContent,
                            style = CareerCompassTheme.typography.caption,
                        )
                    }
                }
            }
        }
        if (!detection.dateDetected) {
            Text(
                text = stringResource(R.string.feed_board_register_date_missing_warning),
                color = colors.onWarningContainer,
                style = CareerCompassTheme.typography.caption,
            )
        }
    }
}

@Composable
private fun BoardRegisterForm(
    state: BoardRegisterUiState,
    onEvent: (BoardRegisterEvent) -> Unit,
) {
    val spacing = CareerCompassTheme.spacing

    CareerCompassTextField(
        value = state.name,
        onValueChange = { onEvent(BoardRegisterEvent.NameChanged(it)) },
        label = stringResource(R.string.feed_board_register_name_label),
        placeholder = stringResource(R.string.feed_board_register_name_placeholder),
        enabled = !state.isSubmitting,
    )
    Column(verticalArrangement = Arrangement.spacedBy(spacing.small)) {
        FeedSectionTitle(text = stringResource(R.string.feed_board_register_type_title))
        FlowRow(
            modifier = Modifier.selectableGroup(),
            horizontalArrangement = Arrangement.spacedBy(spacing.xSmall),
            verticalArrangement = Arrangement.spacedBy(spacing.xSmall),
        ) {
            BoardType.entries.forEach { type ->
                FeedChoiceTag(
                    label = stringResource(type.labelRes()),
                    selected = type == state.type,
                    onClick = { onEvent(BoardRegisterEvent.TypeSelected(type)) },
                    role = Role.RadioButton,
                    enabled = !state.isSubmitting,
                )
            }
        }
    }
    Column(verticalArrangement = Arrangement.spacedBy(spacing.small)) {
        FeedSectionTitle(text = stringResource(R.string.feed_board_register_cycle_title))
        FlowRow(
            modifier = Modifier.selectableGroup(),
            horizontalArrangement = Arrangement.spacedBy(spacing.xSmall),
            verticalArrangement = Arrangement.spacedBy(spacing.xSmall),
        ) {
            BoardCollectCycle.entries.forEach { cycle ->
                FeedChoiceTag(
                    label = stringResource(cycle.labelRes()),
                    selected = cycle == state.cycle,
                    onClick = { onEvent(BoardRegisterEvent.CycleSelected(cycle)) },
                    role = Role.RadioButton,
                    enabled = !state.isSubmitting,
                )
            }
        }
    }
}
