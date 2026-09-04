package com.cambridge.core.ui.component

import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cambridge.core.ui.R
import com.cambridge.core.ui.failure.FailureKind
import com.cambridge.core.ui.failure.description
import com.cambridge.core.ui.failure.display
import com.cambridge.core.ui.failure.title
import com.cambridge.core.ui.theme.CareerCompassTheme

/**
 * Displays the offline error state with a retry action and an optional offline-mode action.
 *
 * When [onOfflineClick] is `null`, the offline-mode action is not displayed.
 *
 * 문구는 스스로 짓지 않고 실패 표에서 읽는다([FailureKind.NoConnection]) — 연결 없음은 이 컴포넌트
 * 말고도 여러 화면이 말하는 사실이라, 문구를 각자 들면 같은 상황을 서로 다르게 안내하게 된다(#204).
 */
@Composable
public fun CareerCompassNetworkErrorState(
    onRetryClick: () -> Unit,
    onOfflineClick: (() -> Unit)?,
    modifier: Modifier = Modifier,
) {
    val display = FailureKind.NoConnection.display()

    CareerCompassStateLayout(
        title = display.title(),
        description = display.description(),
        modifier = modifier,
        illustration = {
            StateEmoji(R.string.core_ui_state_network_illustration)
        },
        details = null,
        actions = {
            CareerCompassButton(
                text = stringResource(R.string.core_ui_state_retry),
                onClick = onRetryClick,
                modifier = Modifier.fillMaxWidth(),
                size = CareerCompassButtonSize.Large,
            )
            if (onOfflineClick != null) {
                CareerCompassButton(
                    text = stringResource(R.string.core_ui_state_offline),
                    onClick = onOfflineClick,
                    modifier = Modifier.fillMaxWidth(),
                    variant = CareerCompassButtonVariant.Secondary,
                    size = CareerCompassButtonSize.Large,
                )
            }
        },
    )
}

/**
 * Displays an AI analysis state with indeterminate or determinate progress.
 *
 * [title] and [description] must be non-blank. A `null` [progress] renders an indeterminate
 * indicator; when supplied it must be between `0f` and `1f`, inclusive. [progressLabel] is shown
 * whenever it is supplied and must then be non-blank.
 */
@Composable
public fun CareerCompassAnalyzingState(
    title: String,
    description: String,
    progress: Float?,
    progressLabel: String?,
    modifier: Modifier = Modifier,
) {
    require(title.isNotBlank()) { "title must not be blank" }
    require(description.isNotBlank()) { "description must not be blank" }
    require(progress == null || progress in PROGRESS_RANGE) {
        "progress must be null or between 0 and 1"
    }
    require(progressLabel == null || progressLabel.isNotBlank()) {
        "progressLabel must be null or non-blank"
    }

    val colors = CareerCompassTheme.colors

    CareerCompassStateLayout(
        title = title,
        description = description,
        modifier = modifier,
        illustration = {
            if (progress == null) {
                CircularProgressIndicator(
                    modifier = Modifier.size(48.dp),
                    color = colors.primary,
                    trackColor = colors.subtleOutline,
                )
            } else {
                CircularProgressIndicator(
                    progress = { progress },
                    modifier = Modifier.size(48.dp),
                    color = colors.primary,
                    trackColor = colors.subtleOutline,
                )
            }
        },
        details =
            if (progress == null && progressLabel == null) {
                null
            } else {
                {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(CareerCompassTheme.spacing.small),
                    ) {
                        if (progress != null) {
                            LinearProgressIndicator(
                                progress = { progress },
                                modifier =
                                    Modifier
                                        .width(160.dp)
                                        .height(4.dp),
                                color = colors.primary,
                                trackColor = colors.subtleOutline,
                            )
                        }
                        if (progressLabel != null) {
                            Text(
                                text = progressLabel,
                                color = colors.mutedContent,
                                textAlign = TextAlign.Center,
                                style = CareerCompassTheme.typography.caption,
                            )
                        }
                    }
                }
            },
        actions = {},
    )
}

/**
 * Displays an empty-result state and an optional primary action.
 *
 * [title] and [description] must be non-blank. [actionText] and [onActionClick] must either both be
 * `null`, or both be supplied. A supplied [actionText] must be non-blank.
 */
@Composable
public fun CareerCompassEmptyState(
    title: String,
    description: String,
    actionText: String?,
    onActionClick: (() -> Unit)?,
    modifier: Modifier = Modifier,
) {
    require(title.isNotBlank()) { "title must not be blank" }
    require(description.isNotBlank()) { "description must not be blank" }
    require((actionText == null) == (onActionClick == null)) {
        "actionText and onActionClick must both be null or both be non-null"
    }
    require(actionText == null || actionText.isNotBlank()) {
        "actionText must be null or non-blank"
    }

    CareerCompassStateLayout(
        title = title,
        description = description,
        modifier = modifier,
        illustration = {
            StateEmoji(R.string.core_ui_state_empty_illustration)
        },
        details = null,
        actions = {
            if (actionText != null && onActionClick != null) {
                CareerCompassButton(
                    text = actionText,
                    onClick = onActionClick,
                    modifier = Modifier.fillMaxWidth(),
                    size = CareerCompassButtonSize.Large,
                )
            }
        },
    )
}

/**
 * Displays a permission-denied state with a benefits summary and settings actions.
 *
 * [title] and [description] must be non-blank. [benefits] must contain at least one item, and every
 * item must be non-blank.
 */
@Composable
public fun CareerCompassPermissionDeniedState(
    title: String,
    description: String,
    benefits: List<String>,
    onOpenSettingsClick: () -> Unit,
    onLaterClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    require(title.isNotBlank()) { "title must not be blank" }
    require(description.isNotBlank()) { "description must not be blank" }
    require(benefits.isNotEmpty() && benefits.all(String::isNotBlank)) {
        "benefits must contain at least one non-blank item"
    }

    val spacing = CareerCompassTheme.spacing

    CareerCompassStateLayout(
        title = title,
        description = description,
        modifier = modifier,
        illustration = {
            StateEmoji(R.string.core_ui_state_permission_denied_illustration)
        },
        details = {
            CareerCompassCard(
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(spacing.medium),
            ) {
                benefits.forEachIndexed { index, benefit ->
                    if (index > 0) {
                        Spacer(modifier = Modifier.height(spacing.small))
                    }
                    Text(
                        text = benefit,
                        color = CareerCompassTheme.colors.onSurface,
                        style = CareerCompassTheme.typography.bodyMedium,
                    )
                }
            }
        },
        actions = {
            CareerCompassButton(
                text = stringResource(R.string.core_ui_state_open_settings),
                onClick = onOpenSettingsClick,
                modifier = Modifier.fillMaxWidth(),
                size = CareerCompassButtonSize.Large,
            )
            CareerCompassButton(
                text = stringResource(R.string.core_ui_state_later),
                onClick = onLaterClick,
                modifier = Modifier.fillMaxWidth(),
                variant = CareerCompassButtonVariant.Secondary,
                size = CareerCompassButtonSize.Large,
            )
        },
    )
}

/**
 * Displays a maintenance state with status, refresh action, and optional contact information.
 *
 * [title], [description], and [statusLabel] must be non-blank. When supplied, [contactLabel] must
 * also be non-blank.
 *
 * [onOfflineClick] 가 `null` 이면 오프라인 모드 버튼을 그리지 않는다 — 점검 중이라도 저장해 둔
 * 스냅샷이 있는 화면만 그 길을 연다([CareerCompassNetworkErrorState] 와 같은 규칙).
 */
@Composable
public fun CareerCompassMaintenanceState(
    title: String,
    description: String,
    statusLabel: String,
    onRefreshClick: () -> Unit,
    onOfflineClick: (() -> Unit)?,
    contactLabel: String?,
    modifier: Modifier = Modifier,
) {
    require(title.isNotBlank()) { "title must not be blank" }
    require(description.isNotBlank()) { "description must not be blank" }
    require(statusLabel.isNotBlank()) { "statusLabel must not be blank" }
    require(contactLabel == null || contactLabel.isNotBlank()) {
        "contactLabel must be null or non-blank"
    }

    val colors = CareerCompassTheme.colors

    CareerCompassStateLayout(
        title = title,
        description = description,
        modifier = modifier,
        illustration = {
            StateEmoji(R.string.core_ui_state_maintenance_illustration)
        },
        details = {
            CareerCompassBadge(
                label = statusLabel,
                tone = CareerCompassBadgeTone.Error,
            )
        },
        actions = {
            CareerCompassButton(
                text = stringResource(R.string.core_ui_state_refresh),
                onClick = onRefreshClick,
                modifier = Modifier.fillMaxWidth(),
                variant = CareerCompassButtonVariant.Secondary,
                size = CareerCompassButtonSize.Large,
            )
            if (onOfflineClick != null) {
                CareerCompassButton(
                    text = stringResource(R.string.core_ui_state_offline),
                    onClick = onOfflineClick,
                    modifier = Modifier.fillMaxWidth(),
                    variant = CareerCompassButtonVariant.Secondary,
                    size = CareerCompassButtonSize.Large,
                )
            }
            if (contactLabel != null) {
                Text(
                    text = contactLabel,
                    color = colors.mutedContent,
                    textAlign = TextAlign.Center,
                    style = CareerCompassTheme.typography.caption,
                )
            }
        },
    )
}

@Composable
private fun CareerCompassStateLayout(
    title: String,
    description: String,
    modifier: Modifier,
    illustration: @Composable () -> Unit,
    details: (@Composable ColumnScope.() -> Unit)?,
    actions: @Composable ColumnScope.() -> Unit,
) {
    val colors = CareerCompassTheme.colors
    val spacing = CareerCompassTheme.spacing

    Column(
        modifier =
            modifier
                .fillMaxSize()
                .background(colors.subtleSurface)
                .padding(horizontal = spacing.large, vertical = spacing.xxLarge),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Column(
            modifier =
                Modifier
                    .weight(1f)
                    .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            illustration()
            Spacer(modifier = Modifier.height(spacing.xxLarge))
            Text(
                text = title,
                color = colors.onSurface,
                textAlign = TextAlign.Center,
                style = CareerCompassTheme.typography.headline2,
            )
            Spacer(modifier = Modifier.height(spacing.small))
            Text(
                text = description,
                color = colors.mutedContent,
                textAlign = TextAlign.Center,
                style = CareerCompassTheme.typography.bodyMedium,
            )
            if (details != null) {
                Spacer(modifier = Modifier.height(spacing.xxLarge))
                details()
            }
        }
        Spacer(modifier = Modifier.height(spacing.large))
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(spacing.small),
            content = actions,
        )
    }
}

@Composable
private fun StateEmoji(
    @StringRes illustrationResId: Int,
) {
    Text(
        text = stringResource(illustrationResId),
        modifier = Modifier.clearAndSetSemantics {},
        fontSize = 48.sp,
        lineHeight = 58.sp,
    )
}

private val PROGRESS_RANGE = 0f..1f
