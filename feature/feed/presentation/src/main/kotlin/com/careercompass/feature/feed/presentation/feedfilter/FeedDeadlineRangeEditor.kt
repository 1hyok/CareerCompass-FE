package com.careercompass.feature.feed.presentation.feedfilter

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.careercompass.core.ui.theme.CareerCompassTheme
import com.careercompass.feature.feed.presentation.R
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

/**
 * 마감일 필터의 「직접 지정」 편집 영역 — 시작일·종료일을 각각 눌러 고르고, 잘못된 범위는 이유를 보여 준다.
 *
 * 두 끝을 따로 고르게 둔 이유 — 한쪽만 고른 조회(「11월부터」)가 스펙상 유효하고, 달력 하나로 이어 고르게
 * 하면 그 상태를 만들 수 없다. 대신 뒤집힌 범위가 만들어질 수 있어 [FeedDeadlineRange.error] 로 막는다.
 *
 * 날짜 선택 자체는 Material3 달력을 쓴다 — 이 시트에만 필요한 UI 라 `core:ui` 로 올리지 않는다.
 */
@Composable
internal fun FeedDeadlineRangeEditor(
    range: FeedDeadlineRange,
    onEvent: (FeedFilterEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = CareerCompassTheme.colors
    val spacing = CareerCompassTheme.spacing
    val startLabel = stringResource(R.string.feed_filter_deadline_range_start)
    val endLabel = stringResource(R.string.feed_filter_deadline_range_end)

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(spacing.xSmall),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(spacing.xSmall),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            FeedDeadlineRangeField(
                label = startLabel,
                date = range.start,
                onClick = { onEvent(FeedFilterEvent.DeadlineRangeEndpointClicked(FeedDeadlineRangeEndpoint.Start)) },
                modifier = Modifier.weight(1f),
            )
            Text(
                text = stringResource(R.string.feed_filter_deadline_range_separator),
                color = colors.mutedContent,
                style = CareerCompassTheme.typography.bodyMedium,
            )
            FeedDeadlineRangeField(
                label = endLabel,
                date = range.end,
                onClick = { onEvent(FeedFilterEvent.DeadlineRangeEndpointClicked(FeedDeadlineRangeEndpoint.End)) },
                modifier = Modifier.weight(1f),
            )
        }
        range.error?.let { error ->
            Text(
                text = stringResource(error.messageRes()),
                color = colors.error,
                style = CareerCompassTheme.typography.caption,
            )
        }
    }

    range.editing?.let { endpoint ->
        FeedDeadlineDatePickerDialog(
            title = if (endpoint == FeedDeadlineRangeEndpoint.Start) startLabel else endLabel,
            initialDate = range.dateOf(endpoint),
            onDateSelected = { date -> onEvent(FeedFilterEvent.DeadlineRangeDateSelected(date)) },
            onDismiss = { onEvent(FeedFilterEvent.DeadlineRangePickerDismissed) },
        )
    }
}

/** 고른 날짜를 보여 주고 누르면 달력을 여는 칸. 값이 없으면 [label] 자체가 자리 표시가 된다. */
@Composable
private fun FeedDeadlineRangeField(
    label: String,
    date: LocalDate?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = CareerCompassTheme.colors
    val shape = CareerCompassTheme.shapes.control
    val value = date?.format(RANGE_DATE_FORMAT)
    val description =
        stringResource(
            R.string.feed_filter_deadline_range_field,
            label,
            value ?: stringResource(R.string.feed_filter_deadline_range_unset),
        )

    Box(
        modifier =
            modifier
                .height(FIELD_HEIGHT)
                .clip(shape)
                .border(BorderStroke(1.dp, colors.interactiveOutline), shape)
                .clickable(role = Role.Button, onClick = onClick)
                .semantics(mergeDescendants = true) { contentDescription = description }
                .padding(horizontal = CareerCompassTheme.spacing.small),
        contentAlignment = Alignment.CenterStart,
    ) {
        Text(
            text = value ?: label,
            color = if (value == null) colors.mutedContent else colors.onSurface,
            style = CareerCompassTheme.typography.bodyMedium,
            maxLines = 1,
        )
    }
}

/**
 * 한쪽 끝을 고르는 달력 대화상자.
 *
 * 달력은 UTC 자정 기준 epoch millis 로 값을 주고받아, 시계·시간대와 무관하게 「고른 날짜」만 남긴다.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FeedDeadlineDatePickerDialog(
    title: String,
    initialDate: LocalDate?,
    onDateSelected: (LocalDate) -> Unit,
    onDismiss: () -> Unit,
) {
    val pickerState = rememberDatePickerState(initialSelectedDateMillis = initialDate?.toPickerMillis())

    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(
                onClick = { pickerState.selectedDateMillis?.let { onDateSelected(it.toPickerDate()) } },
                enabled = pickerState.selectedDateMillis != null,
            ) {
                Text(text = stringResource(R.string.feed_filter_deadline_range_confirm))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(text = stringResource(R.string.feed_filter_deadline_range_cancel))
            }
        },
    ) {
        DatePicker(
            state = pickerState,
            title = {
                Text(
                    text = title,
                    modifier = Modifier.padding(start = TITLE_START_PADDING, end = TITLE_END_PADDING, top = TITLE_TOP_PADDING),
                    style = CareerCompassTheme.typography.labelMedium,
                )
            },
        )
    }
}

private fun FeedDeadlineRangeError.messageRes(): Int =
    when (this) {
        FeedDeadlineRangeError.Empty -> R.string.feed_filter_deadline_range_error_empty
        FeedDeadlineRangeError.StartAfterEnd -> R.string.feed_filter_deadline_range_error_start_after_end
    }

private fun LocalDate.toPickerMillis(): Long = atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()

private fun Long.toPickerDate(): LocalDate = Instant.ofEpochMilli(this).atZone(ZoneOffset.UTC).toLocalDate()

/** 손가락으로 누르는 칸이라 최소 터치 영역(48dp)을 그대로 높이로 쓴다. */
private val FIELD_HEIGHT = 48.dp

private val TITLE_START_PADDING = 24.dp
private val TITLE_END_PADDING = 12.dp
private val TITLE_TOP_PADDING = 16.dp

private val RANGE_DATE_FORMAT: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy.MM.dd")
