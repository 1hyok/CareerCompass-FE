package com.careercompass.feature.feed.presentation.postingraw

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.sp
import com.careercompass.core.ui.component.CareerCompassButton
import com.careercompass.core.ui.component.CareerCompassButtonSize
import com.careercompass.core.ui.component.CareerCompassButtonVariant
import com.careercompass.core.ui.theme.CareerCompassTheme
import com.careercompass.feature.feed.presentation.R
import com.careercompass.feature.feed.presentation.shared.component.FeedTopBar

/** Stateless raw-text view of a posting, with a selectable body and an optional source link action. */
@Composable
public fun PostingRawContent(
    state: PostingRawUiState,
    onEvent: (PostingRawEvent) -> Unit,
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
            title = stringResource(R.string.feed_posting_raw_title),
            onBackClick = { onEvent(PostingRawEvent.BackClicked) },
            actions = null,
        )
        Column(
            modifier =
                Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = spacing.large, vertical = spacing.small),
            verticalArrangement = Arrangement.spacedBy(spacing.medium),
        ) {
            Text(
                text = state.title,
                modifier = Modifier.semantics { heading() },
                color = colors.onSurface,
                style = CareerCompassTheme.typography.headline2,
            )
            Text(
                text = state.sourceLabel,
                color = colors.mutedContent,
                style =
                    CareerCompassTheme.typography.caption.copy(
                        fontSize = 12.sp,
                        lineHeight = 18.sp,
                    ),
            )
            HorizontalDivider(color = colors.subtleOutline)
            SelectionContainer {
                Text(
                    text = state.rawContent,
                    color = colors.onSurface,
                    style =
                        CareerCompassTheme.typography.bodyMedium.copy(
                            lineHeight = RAW_BODY_LINE_HEIGHT.sp,
                        ),
                )
            }
        }
        if (state.originalUrl != null) {
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
                    text = stringResource(R.string.feed_posting_raw_open_original),
                    onClick = { onEvent(PostingRawEvent.OpenOriginalClicked) },
                    modifier = Modifier.fillMaxWidth(),
                    variant = CareerCompassButtonVariant.Dark,
                    size = CareerCompassButtonSize.Large,
                )
            }
        }
    }
}

/** 14sp body × 1.6 line spacing from the raw-view specification. */
private const val RAW_BODY_LINE_HEIGHT = 22.4
