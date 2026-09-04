package com.careercompass.core.ui.component

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
import com.careercompass.core.ui.R
import com.careercompass.core.ui.failure.FailureDisplay
import com.careercompass.core.ui.failure.FailureKind
import com.careercompass.core.ui.failure.actionLabel
import com.careercompass.core.ui.failure.description
import com.careercompass.core.ui.failure.display
import com.careercompass.core.ui.failure.title
import com.careercompass.core.ui.theme.CareerCompassTheme

/**
 * 상태 부품이 차지하는 자리 — 화면 한 장인가, 카드·구역 안인가.
 *
 * Figma 09 Edge Cases 는 다섯 상태를 전부 화면 한 장으로 그렸지만, 앱에는 화면을 통째로 덮을 수 없는 자리가
 * 있다 — 공고 상세의 적합도 카드가 그렇다(#221). 제목·원문 보기·다른 카드가 살아 있어야 하므로 카드 한 칸만
 * 「분석 중」이어야 한다. 기본값이 [FullScreen] 이라 기존 호출처는 한 줄도 바뀌지 않는다.
 */
public enum class CareerCompassStatePresentation {
    /** 화면 한 장을 통째로 — 배경을 깔고 행동 버튼을 바닥에 붙인다. */
    FullScreen,

    /**
     * 카드·구역 안에 끼워 넣는다 — 폭만 채우고 높이는 내용만큼, 배경 없음, 행동은 본문 바로 아래.
     *
     * 화면 한 장 뼈대는 `weight(1f)` 로 본문을 가운데 띄우는데 그것은 높이가 정해진 부모에서만 성립한다.
     * 카드는 내용만큼 자라므로 뼈대를 따로 둔다.
     */
    Inline,
}

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
 *
 * [presentation] 이 [CareerCompassStatePresentation.Inline] 이면 카드 안에 들어가는 모양이 된다 — 다섯 부품
 * 중 이것만 그 자리를 여는 이유는, 「기다리는 중」은 화면의 나머지가 그대로 쓸모 있는 유일한 상태라서다
 * (실패·빈 결과·권한·점검은 화면이 더 보여 줄 것이 없다).
 */
@Composable
public fun CareerCompassAnalyzingState(
    title: String,
    description: String,
    progress: Float?,
    progressLabel: String?,
    modifier: Modifier = Modifier,
    presentation: CareerCompassStatePresentation = CareerCompassStatePresentation.FullScreen,
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
        presentation = presentation,
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
 * 사유를 특정하지 못한 실패 — Figma 09 Edge Cases 다섯 장에는 없는 여섯째 상태다(#222).
 *
 * 나머지 실패가 전부 접히는 자리라 **실제로는 가장 자주 뜨는 실패 화면**인데, 그동안 「검색 결과 없음」
 * 부품(🔍)을 돌려 썼다. 그 삽화를 보면 사용자는 자기 조건이 잘못됐다고 읽는다 — 서버가 500 을 준 것인데.
 * 삽화·문구·행동이 [CareerCompassEmptyState] 와 달라야 하는 이유가 그것이다.
 *
 * **행동 버튼의 유무는 호출자가 정한다** — 재시도해도 답이 갈리지 않는 실패가 있다(상한 초과·중복·차단,
 * #204 의 표가 그 판정을 갖는다). [actionText] 와 [onActionClick] 은 둘 다 있거나 둘 다 없다. 표의 행을
 * 그대로 그리려면 [FailureDisplay] 를 받는 판을 쓴다.
 *
 * [title] and [description] must be non-blank. A supplied [actionText] must be non-blank.
 */
@Composable
public fun CareerCompassFailureState(
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
            StateEmoji(R.string.core_ui_state_failure_illustration)
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
 * 실패 표의 한 행([FailureDisplay], #204)을 그대로 그린다.
 *
 * 버튼은 **표가 행동을 갖고**([FailureDisplay.action] ≠ 없음) **호출자가 그 행동을 받을 때**([onActionClick]
 * ≠ `null`)만 붙는다. 표가 「할 수 있는 일이 없다」고 하면 호출자가 콜백을 넘겨도 그리지 않는다 — 눌러도
 * 같은 실패를 다시 만나는 버튼을 만들지 않는다. 반대로 표에 행동이 있어도 화면이 그 길을 못 열면(프로필
 * 화면이 없는 자리 등) 콜백을 넘기지 않으면 된다.
 */
@Composable
public fun CareerCompassFailureState(
    display: FailureDisplay,
    onActionClick: (() -> Unit)?,
    modifier: Modifier = Modifier,
) {
    val actionText = display.actionLabel()
    val hasAction = actionText != null && onActionClick != null

    CareerCompassFailureState(
        title = display.title(),
        description = display.description(),
        actionText = actionText.takeIf { hasAction },
        onActionClick = onActionClick.takeIf { hasAction },
        modifier = modifier,
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
    presentation: CareerCompassStatePresentation = CareerCompassStatePresentation.FullScreen,
) {
    when (presentation) {
        CareerCompassStatePresentation.FullScreen -> {
            FullScreenStateLayout(
                title = title,
                description = description,
                modifier = modifier,
                illustration = illustration,
                details = details,
                actions = actions,
            )
        }

        CareerCompassStatePresentation.Inline -> {
            InlineStateLayout(
                title = title,
                description = description,
                modifier = modifier,
                illustration = illustration,
                details = details,
                actions = actions,
            )
        }
    }
}

@Composable
private fun FullScreenStateLayout(
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

/**
 * 카드 안 뼈대 — 배경도 `weight` 도 없다. 행동은 같은 열에 이어 붙어 비어 있으면 자리도 없다.
 *
 * 제목은 [CareerCompassTheme.typography.headline4] 다 — 카드 제목(`headline2`)보다 한 단계 작아야 카드
 * 안의 상태가 카드 자체보다 크게 말하지 않는다.
 */
@Composable
private fun InlineStateLayout(
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
                .fillMaxWidth()
                .padding(vertical = spacing.medium),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(spacing.small),
    ) {
        illustration()
        Text(
            text = title,
            color = colors.onSurface,
            textAlign = TextAlign.Center,
            style = CareerCompassTheme.typography.headline4,
        )
        Text(
            text = description,
            color = colors.mutedContent,
            textAlign = TextAlign.Center,
            style = CareerCompassTheme.typography.bodyMedium,
        )
        if (details != null) {
            details()
        }
        actions()
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
