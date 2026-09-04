package com.careercompass.feature.feed.presentation.board

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.careercompass.core.ui.component.CareerCompassButton
import com.careercompass.core.ui.component.CareerCompassButtonSize
import com.careercompass.core.ui.component.CareerCompassButtonVariant
import com.careercompass.core.ui.component.CareerCompassTextField
import com.careercompass.core.ui.icon.CareerCompassIcons
import com.careercompass.core.ui.theme.CareerCompassTheme
import com.careercompass.feature.feed.presentation.R
import com.careercompass.feature.feed.presentation.shared.component.FeedChoiceTag
import com.careercompass.feature.feed.presentation.shared.component.FeedIconButton
import com.careercompass.feature.feed.presentation.shared.component.FeedSectionTitle

/**
 * Body of the board edit bottom sheet — name, type, and collect cycle (`PATCH /boards/{id}`).
 *
 * The caller wraps this in a `ModalBottomSheet`; keeping the sheet chrome outside makes the content
 * testable and previewable on its own. Inputs lock while a save is in flight.
 */
@Composable
public fun BoardEditSheetContent(
    state: BoardEditUiState,
    onEvent: (BoardEditEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = CareerCompassTheme.colors
    val spacing = CareerCompassTheme.spacing

    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(start = spacing.large, end = spacing.xxSmall),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.feed_board_edit_title),
                    modifier = Modifier.semantics { heading() },
                    color = colors.onSurface,
                    style = CareerCompassTheme.typography.headline2,
                )
                Text(
                    text = state.boardName,
                    color = colors.mutedContent,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = CareerCompassTheme.typography.caption,
                )
            }
            FeedIconButton(
                icon = CareerCompassIcons.Close,
                contentDescription = stringResource(R.string.feed_board_edit_close),
                onClick = { onEvent(BoardEditEvent.DismissClicked) },
            )
        }
        Column(
            modifier =
                Modifier
                    .weight(weight = 1f, fill = false)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = spacing.large, vertical = spacing.small),
            verticalArrangement = Arrangement.spacedBy(spacing.large),
        ) {
            BoardEditUrlSection(url = state.url)
            CareerCompassTextField(
                value = state.name,
                onValueChange = { onEvent(BoardEditEvent.NameChanged(it)) },
                label = stringResource(R.string.feed_board_register_name_label),
                placeholder = stringResource(R.string.feed_board_register_name_placeholder),
                errorMessage = state.nameError,
                isError = state.nameError != null,
                enabled = !state.isSaving,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            )
            BoardEditSection(title = stringResource(R.string.feed_board_register_type_title)) {
                BoardType.entries.forEach { type ->
                    FeedChoiceTag(
                        label = stringResource(type.labelRes()),
                        selected = type == state.type,
                        onClick = { onEvent(BoardEditEvent.TypeSelected(type)) },
                        role = Role.RadioButton,
                        enabled = !state.isSaving,
                    )
                }
            }
            BoardEditSection(title = stringResource(R.string.feed_board_register_cycle_title)) {
                BoardCollectCycle.entries.forEach { cycle ->
                    FeedChoiceTag(
                        label = stringResource(cycle.labelRes()),
                        selected = cycle == state.cycle,
                        onClick = { onEvent(BoardEditEvent.CycleSelected(cycle)) },
                        role = Role.RadioButton,
                        enabled = !state.isSaving,
                    )
                }
            }
            if (state.isSaving) {
                BoardEditSavingRow()
            }
        }
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(spacing.large),
            horizontalArrangement = Arrangement.spacedBy(spacing.small),
        ) {
            CareerCompassButton(
                text = stringResource(R.string.feed_board_edit_cancel),
                onClick = { onEvent(BoardEditEvent.DismissClicked) },
                variant = CareerCompassButtonVariant.Ghost,
                size = CareerCompassButtonSize.Large,
                enabled = !state.isSaving,
            )
            CareerCompassButton(
                text = stringResource(R.string.feed_board_edit_save),
                onClick = { onEvent(BoardEditEvent.SaveClicked) },
                modifier = Modifier.weight(1f),
                variant = CareerCompassButtonVariant.Primary,
                size = CareerCompassButtonSize.Large,
                enabled = state.isSaveEnabled,
                contentDescription = stringResource(R.string.feed_board_edit_save_content_description, state.boardName),
            )
        }
    }
}

/** URL is bound to the detected structure, so it is shown read-only with a note instead of a field. */
@Composable
private fun BoardEditUrlSection(url: String) {
    val colors = CareerCompassTheme.colors
    val spacing = CareerCompassTheme.spacing

    Column(verticalArrangement = Arrangement.spacedBy(spacing.xxSmall)) {
        FeedSectionTitle(text = stringResource(R.string.feed_board_edit_url_title))
        Text(
            text = url,
            color = colors.onSurfaceVariant,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            style = CareerCompassTheme.typography.bodyMedium,
        )
        Text(
            text = stringResource(R.string.feed_board_edit_url_readonly),
            color = colors.mutedContent,
            style = CareerCompassTheme.typography.caption,
        )
    }
}

@Composable
private fun BoardEditSection(
    title: String,
    content: @Composable () -> Unit,
) {
    val spacing = CareerCompassTheme.spacing

    Column(verticalArrangement = Arrangement.spacedBy(spacing.small)) {
        FeedSectionTitle(text = title)
        FlowRow(
            modifier = Modifier.selectableGroup(),
            horizontalArrangement = Arrangement.spacedBy(spacing.xSmall),
            verticalArrangement = Arrangement.spacedBy(spacing.xSmall),
        ) {
            content()
        }
    }
}

@Composable
private fun BoardEditSavingRow() {
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
            text = stringResource(R.string.feed_board_edit_saving),
            color = colors.onSurfaceVariant,
            style = CareerCompassTheme.typography.bodyMedium,
        )
    }
}
