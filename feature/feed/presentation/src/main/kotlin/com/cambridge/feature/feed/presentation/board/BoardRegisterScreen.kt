package com.cambridge.feature.feed.presentation.board

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import com.cambridge.core.ui.component.CareerCompassButton
import com.cambridge.core.ui.component.CareerCompassButtonSize
import com.cambridge.core.ui.component.CareerCompassButtonVariant
import com.cambridge.core.ui.component.CareerCompassTextField
import com.cambridge.core.ui.theme.CareerCompassTheme
import com.cambridge.feature.feed.presentation.R
import com.cambridge.feature.feed.presentation.shared.component.FeedCard
import com.cambridge.feature.feed.presentation.shared.component.FeedChoiceTag
import com.cambridge.feature.feed.presentation.shared.component.FeedSectionTitle
import com.cambridge.feature.feed.presentation.shared.component.FeedTopBar

/** Stateless board registration screen: URL → structure detection → preview → name/type/cycle (spec F2-1). */
@Composable
public fun BoardRegisterScreen(
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
            Box(
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
            ) {
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

@Composable
private fun BoardDetectingRow() {
    val colors = CareerCompassTheme.colors

    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .semantics(mergeDescendants = true) { liveRegion = LiveRegionMode.Polite },
        horizontalArrangement = Arrangement.spacedBy(CareerCompassTheme.spacing.small),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        CircularProgressIndicator(
            modifier = Modifier.size(20.dp),
            color = colors.primaryEmphasis,
            strokeWidth = 2.dp,
        )
        Text(
            text = stringResource(R.string.feed_board_register_detecting),
            color = colors.onSurfaceVariant,
            style = CareerCompassTheme.typography.bodyMedium,
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
