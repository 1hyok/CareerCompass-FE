package com.cambridge.feature.onboarding.presentation

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.disabled
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cambridge.core.ui.component.CareerCompassButton
import com.cambridge.core.ui.component.CareerCompassButtonSize
import com.cambridge.core.ui.component.CareerCompassButtonVariant
import com.cambridge.core.ui.theme.CareerCompassTheme

/** Stateless past-application step from the onboarding flow. */
@Composable
public fun OnboardingStep4Screen(
    state: OnboardingStep4UiState,
    onEvent: (OnboardingStep4Event) -> Unit,
    modifier: Modifier = Modifier,
) {
    OnboardingStepScaffold(
        currentStep = state.currentStep,
        totalSteps = state.totalSteps,
        title = stringResource(R.string.onboarding_step4_title),
        description = stringResource(R.string.onboarding_step4_description),
        onBackClick = { onEvent(OnboardingStep4Event.BackClicked) },
        modifier = modifier,
        footerContent = {
            OnboardingStep4Footer(
                enabled = state.isInputEnabled,
                completeEnabled = state.isCompleteEnabled,
                onSkipClick = { onEvent(OnboardingStep4Event.SkipClicked) },
                onCompleteClick = { onEvent(OnboardingStep4Event.CompleteClicked) },
            )
        },
    ) {
        OnboardingStep4Content(state = state, onEvent = onEvent)
    }
}

@Composable
private fun OnboardingStep4Content(
    state: OnboardingStep4UiState,
    onEvent: (OnboardingStep4Event) -> Unit,
) {
    val spacing = CareerCompassTheme.spacing

    Column(verticalArrangement = Arrangement.spacedBy(spacing.xxLarge)) {
        UploadTarget(
            enabled = state.isUploadEnabled,
            onClick = { onEvent(OnboardingStep4Event.UploadClicked) },
        )
        OrDivider()
        DirectInputAction(
            enabled = state.isInputEnabled,
            onClick = { onEvent(OnboardingStep4Event.DirectInputClicked) },
        )
        if (state.uploadedDocuments.isNotEmpty()) {
            UploadedDocuments(
                documents = state.uploadedDocuments,
                enabled = state.isInputEnabled,
                onMenuClick = { documentId ->
                    onEvent(OnboardingStep4Event.DocumentMenuClicked(documentId))
                },
                onRetryClick = { documentId ->
                    onEvent(OnboardingStep4Event.DocumentRetryClicked(documentId))
                },
            )
        }
    }
}

@Composable
private fun UploadTarget(
    enabled: Boolean,
    onClick: () -> Unit,
) {
    val colors = CareerCompassTheme.colors
    val spacing = CareerCompassTheme.spacing
    val uploadDescription = stringResource(R.string.onboarding_step4_upload_content_description)

    Surface(
        modifier =
            Modifier
                .fillMaxWidth()
                .clip(CareerCompassTheme.shapes.largeControl)
                .clickable(
                    enabled = enabled,
                    role = Role.Button,
                    onClick = onClick,
                ).semantics(mergeDescendants = true) {
                    contentDescription = uploadDescription
                    role = Role.Button
                    if (!enabled) disabled()
                },
        shape = CareerCompassTheme.shapes.largeControl,
        color = if (enabled) colors.surface else colors.disabledContainer,
        contentColor = if (enabled) colors.onSurface else colors.disabledContent,
        border = BorderStroke(1.5.dp, colors.subtleOutline),
    ) {
        Column(
            modifier = Modifier.padding(horizontal = spacing.large, vertical = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(spacing.small),
        ) {
            Box(
                modifier =
                    Modifier
                        .size(44.dp)
                        .testTag(UPLOAD_ICON_SLOT_TAG),
                contentAlignment = Alignment.TopStart,
            ) {
                Image(
                    painter = painterResource(R.drawable.onboarding_ic_upload_document),
                    contentDescription = null,
                    modifier =
                        Modifier
                            .wrapContentSize(
                                align = Alignment.TopStart,
                                unbounded = true,
                            ).requiredSize(
                                width = 62.05.dp,
                                height = 63.05.dp,
                            ).testTag(UPLOAD_ICON_ART_TAG),
                )
            }
            Text(
                text = stringResource(R.string.onboarding_step4_upload_title),
                color = if (enabled) colors.onSurface else colors.disabledContent,
                style =
                    CareerCompassTheme.typography.labelMedium.copy(
                        fontWeight = FontWeight.SemiBold,
                    ),
            )
            Text(
                text =
                    stringResource(
                        R.string.onboarding_step4_upload_requirements,
                        ONBOARDING_MAX_APPLICATION_FILE_SIZE_MEGABYTES,
                    ),
                color = if (enabled) colors.mutedContent else colors.disabledContent,
                style =
                    CareerCompassTheme.typography.caption.copy(
                        fontSize = 12.sp,
                        lineHeight = 18.sp,
                        fontWeight = FontWeight.Normal,
                    ),
            )
        }
    }
}

@Composable
private fun OrDivider() {
    val colors = CareerCompassTheme.colors
    val spacing = CareerCompassTheme.spacing

    Row(
        modifier = Modifier.width(100.dp),
        horizontalArrangement = Arrangement.spacedBy(spacing.medium),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Spacer(
            modifier =
                Modifier
                    .weight(1f)
                    .height(1.dp)
                    .background(colors.subtleOutline),
        )
        Text(
            text = stringResource(R.string.onboarding_step4_or),
            color = colors.mutedContent,
            style =
                CareerCompassTheme.typography.caption.copy(
                    fontSize = 12.sp,
                    lineHeight = 18.sp,
                ),
        )
        Spacer(
            modifier =
                Modifier
                    .weight(1f)
                    .height(1.dp)
                    .background(colors.subtleOutline),
        )
    }
}

@Composable
private fun DirectInputAction(
    enabled: Boolean,
    onClick: () -> Unit,
) {
    val colors = CareerCompassTheme.colors
    val spacing = CareerCompassTheme.spacing
    val fontScale = LocalDensity.current.fontScale
    val useFigmaCompactSize = fontScale <= 1f
    val sizeModifier =
        if (useFigmaCompactSize) {
            Modifier.width(103.dp)
        } else {
            Modifier.fillMaxWidth()
        }

    Surface(
        modifier =
            Modifier
                .then(sizeModifier)
                .heightIn(min = 51.dp)
                .clip(CareerCompassTheme.shapes.largeControl)
                .clickable(
                    enabled = enabled,
                    role = Role.Button,
                    onClick = onClick,
                ).semantics(mergeDescendants = true) {
                    role = Role.Button
                    if (!enabled) disabled()
                },
        shape = CareerCompassTheme.shapes.largeControl,
        color = if (enabled) colors.surface else colors.disabledContainer,
        contentColor = if (enabled) colors.onSurface else colors.disabledContent,
        border = BorderStroke(1.dp, colors.subtleOutline),
    ) {
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
            horizontalArrangement =
                Arrangement.spacedBy(
                    space = if (useFigmaCompactSize) spacing.xSmall else 0.dp,
                    alignment = Alignment.CenterHorizontally,
                ),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier.size(if (useFigmaCompactSize) 16.dp else 14.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = stringResource(R.string.onboarding_step4_direct_input_icon),
                    modifier = Modifier.clearAndSetSemantics {},
                    style =
                        CareerCompassTheme.typography.bodyMedium.copy(
                            fontSize = (14f / fontScale).sp,
                            lineHeight = (16f / fontScale).sp,
                        ),
                )
            }
            Text(
                text = stringResource(R.string.onboarding_step4_direct_input),
                modifier =
                    Modifier.weight(
                        weight = 1f,
                        fill = !useFigmaCompactSize,
                    ),
                fontWeight = FontWeight.SemiBold,
                maxLines = if (useFigmaCompactSize) 1 else Int.MAX_VALUE,
                softWrap = !useFigmaCompactSize,
                style = CareerCompassTheme.typography.labelMedium,
            )
        }
    }
}

@Composable
private fun UploadedDocuments(
    documents: List<OnboardingApplicationDocument>,
    enabled: Boolean,
    onMenuClick: (String) -> Unit,
    onRetryClick: (String) -> Unit,
) {
    val spacing = CareerCompassTheme.spacing

    Column(verticalArrangement = Arrangement.spacedBy(spacing.small)) {
        Text(
            text =
                stringResource(
                    R.string.onboarding_step4_uploaded_count,
                    documents.size,
                    ONBOARDING_MAX_APPLICATION_UPLOAD_COUNT,
                ),
            modifier = Modifier.semantics { heading() },
            color = CareerCompassTheme.colors.onSurfaceVariant,
            style =
                CareerCompassTheme.typography.caption.copy(
                    fontSize = 12.sp,
                    lineHeight = 18.sp,
                ),
        )
        documents.forEach { document ->
            UploadedDocumentItem(
                document = document,
                enabled = enabled,
                onMenuClick = { onMenuClick(document.id) },
                onRetryClick = { onRetryClick(document.id) },
            )
        }
    }
}

@Composable
private fun UploadedDocumentItem(
    document: OnboardingApplicationDocument,
    enabled: Boolean,
    onMenuClick: () -> Unit,
    onRetryClick: () -> Unit,
) {
    val colors = CareerCompassTheme.colors
    val spacing = CareerCompassTheme.spacing
    val fontScale = LocalDensity.current.fontScale
    val useFigmaCompactSize = fontScale <= 1f
    val menuTouchTargetSize = 48.dp
    val cardWidthModifier =
        if (useFigmaCompactSize) {
            Modifier.width(232.dp)
        } else {
            Modifier.fillMaxWidth()
        }
    val cardHeightModifier =
        if (useFigmaCompactSize) {
            Modifier.height(63.dp)
        } else {
            Modifier.heightIn(min = 63.dp)
        }
    val menuDescription =
        stringResource(
            R.string.onboarding_step4_document_menu,
            document.fileName,
        )
    val retryDescription =
        stringResource(
            R.string.onboarding_step4_document_retry_description,
            document.fileName,
        )
    val retryModifier =
        if (document.status is OnboardingApplicationDocumentStatus.Failed) {
            Modifier
                .clickable(
                    enabled = enabled,
                    role = Role.Button,
                    onClick = onRetryClick,
                ).semantics(mergeDescendants = true) {
                    contentDescription = retryDescription
                    liveRegion = LiveRegionMode.Polite
                    role = Role.Button
                    if (!enabled) disabled()
                }
        } else {
            Modifier
        }

    Surface(
        modifier =
            Modifier
                .then(cardWidthModifier)
                .then(cardHeightModifier)
                .testTag(documentCardTag(document.id)),
        shape = CareerCompassTheme.shapes.largeControl,
        color = colors.surface,
        contentColor = colors.onSurface,
        border = BorderStroke(1.dp, colors.surfaceVariant),
    ) {
        Box(
            modifier =
                Modifier
                    .fillMaxWidth(),
        ) {
            Row(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(
                            start = spacing.medium,
                            top = 7.5.dp,
                            end = menuTouchTargetSize,
                            bottom = 7.5.dp,
                        ),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                DocumentFormatBadge(
                    documentId = document.id,
                    formatLabel = document.format.label,
                    fontScale = fontScale,
                )
                Spacer(modifier = Modifier.width(10.dp))
                Column(
                    modifier =
                        Modifier
                            .weight(1f)
                            .heightIn(min = 48.dp)
                            .testTag(documentTextTag(document.id))
                            .then(retryModifier),
                    verticalArrangement =
                        Arrangement.spacedBy(
                            space = 2.dp,
                            alignment = Alignment.CenterVertically,
                        ),
                ) {
                    Text(
                        text = document.fileName,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        color = colors.onSurface,
                        style =
                            CareerCompassTheme.typography.labelMedium.copy(
                                fontSize = 13.sp,
                                lineHeight = 19.5.sp,
                                fontWeight = FontWeight.SemiBold,
                            ),
                    )
                    DocumentStatus(
                        status = document.status,
                        enabled = enabled,
                    )
                }
            }
            Box(
                modifier =
                    Modifier
                        .size(menuTouchTargetSize)
                        .align(Alignment.CenterEnd)
                        .clickable(
                            enabled = enabled,
                            role = Role.Button,
                            onClick = onMenuClick,
                        ).semantics {
                            contentDescription = menuDescription
                            role = Role.Button
                            if (!enabled) disabled()
                        },
                contentAlignment = Alignment.CenterEnd,
            ) {
                Text(
                    text = stringResource(R.string.onboarding_step4_document_options_icon),
                    modifier =
                        Modifier
                            .padding(end = spacing.medium)
                            .clearAndSetSemantics {},
                    color = if (enabled) colors.mutedContent else colors.disabledContent,
                    style = CareerCompassTheme.typography.headline4,
                )
            }
        }
    }
}

@Composable
private fun DocumentStatus(
    status: OnboardingApplicationDocumentStatus,
    enabled: Boolean,
) {
    val colors = CareerCompassTheme.colors
    val liveRegionModifier =
        Modifier.semantics(mergeDescendants = true) {
            liveRegion = LiveRegionMode.Polite
        }

    when (status) {
        OnboardingApplicationDocumentStatus.Processing -> {
            Row(
                modifier = liveRegionModifier,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                CircularProgressIndicator(
                    modifier =
                        Modifier
                            .size(12.dp)
                            .clearAndSetSemantics {},
                    color = if (enabled) colors.actionPrimary else colors.disabledContent,
                    strokeWidth = 1.5.dp,
                )
                DocumentStatusText(
                    text = stringResource(R.string.onboarding_step4_document_processing),
                    color = if (enabled) colors.mutedContent else colors.disabledContent,
                )
            }
        }

        is OnboardingApplicationDocumentStatus.Completed -> {
            DocumentStatusText(
                text =
                    stringResource(
                        R.string.onboarding_step4_document_completed,
                        status.classifiedItemCount,
                    ),
                color = if (enabled) colors.mutedContent else colors.disabledContent,
                modifier = liveRegionModifier,
            )
        }

        is OnboardingApplicationDocumentStatus.Failed -> {
            DocumentStatusText(
                text =
                    stringResource(
                        R.string.onboarding_step4_document_failed_retry,
                        status.message,
                    ),
                color = if (enabled) colors.actionDanger else colors.disabledContent,
                modifier = liveRegionModifier,
            )
        }
    }
}

@Composable
private fun DocumentStatusText(
    text: String,
    color: androidx.compose.ui.graphics.Color,
    modifier: Modifier = Modifier,
) {
    val useFigmaCompactSize = LocalDensity.current.fontScale <= 1f
    val widthModifier =
        if (useFigmaCompactSize) {
            Modifier
        } else {
            Modifier.fillMaxWidth()
        }

    Text(
        text = text,
        modifier = modifier.then(widthModifier),
        maxLines = if (useFigmaCompactSize) 1 else Int.MAX_VALUE,
        softWrap = !useFigmaCompactSize,
        overflow = if (useFigmaCompactSize) TextOverflow.Ellipsis else TextOverflow.Clip,
        color = color,
        style = CareerCompassTheme.typography.caption,
    )
}

@Composable
private fun DocumentFormatBadge(
    documentId: String,
    formatLabel: String,
    fontScale: Float,
) {
    val colors = CareerCompassTheme.colors
    val badgeScale = fontScale.coerceAtLeast(1f)

    Box(
        modifier =
            Modifier
                .width(20.dp * badgeScale)
                .height(15.dp * badgeScale)
                .background(
                    color = colors.successContainer,
                    shape = CareerCompassTheme.shapes.control,
                ).testTag(documentFormatBadgeTag(documentId)),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = formatLabel,
            modifier = Modifier.clearAndSetSemantics {},
            color = colors.onSuccessContainer,
            maxLines = 1,
            style =
                CareerCompassTheme.typography.caption.copy(
                    fontSize = 10.sp,
                    lineHeight = 15.sp,
                    fontWeight = FontWeight.Bold,
                ),
        )
    }
}

private fun documentCardTag(documentId: String): String = "onboarding_step4_document_$documentId"

private fun documentTextTag(documentId: String): String = "onboarding_step4_document_text_$documentId"

private fun documentFormatBadgeTag(documentId: String): String = "onboarding_step4_document_format_$documentId"

private const val UPLOAD_ICON_SLOT_TAG: String = "onboarding_step4_upload_icon_slot"

private const val UPLOAD_ICON_ART_TAG: String = "onboarding_step4_upload_icon_art"

@Composable
private fun OnboardingStep4Footer(
    enabled: Boolean,
    completeEnabled: Boolean,
    onSkipClick: () -> Unit,
    onCompleteClick: () -> Unit,
) {
    val colors = CareerCompassTheme.colors
    val spacing = CareerCompassTheme.spacing
    val useFigmaCompactSize = LocalDensity.current.fontScale <= 1f

    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .background(colors.subtleSurface)
                .navigationBarsPadding()
                .padding(
                    start = spacing.large,
                    top = spacing.medium,
                    end = spacing.large,
                    bottom = spacing.large,
                ),
        horizontalArrangement = Arrangement.spacedBy(spacing.small),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (useFigmaCompactSize) {
            CareerCompassButton(
                text = stringResource(R.string.onboarding_step4_skip),
                onClick = onSkipClick,
                modifier = Modifier.weight(1f),
                variant = CareerCompassButtonVariant.Secondary,
                size = CareerCompassButtonSize.Large,
                enabled = enabled,
            )
            CareerCompassButton(
                text = stringResource(R.string.onboarding_step4_complete),
                onClick = onCompleteClick,
                modifier = Modifier.weight(2.2f),
                size = CareerCompassButtonSize.Large,
                enabled = completeEnabled,
            )
        } else {
            OnboardingStep4AdaptiveFooterButton(
                text = stringResource(R.string.onboarding_step4_skip),
                onClick = onSkipClick,
                modifier = Modifier.weight(1f),
                isPrimary = false,
                enabled = enabled,
            )
            OnboardingStep4AdaptiveFooterButton(
                text = stringResource(R.string.onboarding_step4_complete),
                onClick = onCompleteClick,
                modifier = Modifier.weight(2.2f),
                isPrimary = true,
                enabled = completeEnabled,
            )
        }
    }
}

/** Large-text fallback for the fixed-height design-system button used by the 1x Figma layout. */
@Composable
private fun OnboardingStep4AdaptiveFooterButton(
    text: String,
    onClick: () -> Unit,
    isPrimary: Boolean,
    enabled: Boolean,
    modifier: Modifier = Modifier,
) {
    val colors = CareerCompassTheme.colors
    val shape = CareerCompassTheme.shapes.control
    val containerColor =
        when {
            !enabled -> colors.disabledContainer
            isPrimary -> colors.actionPrimary
            else -> colors.surface
        }
    val contentColor =
        when {
            !enabled -> colors.disabledContent
            isPrimary -> colors.onAction
            else -> colors.onSurface
        }

    Row(
        modifier =
            modifier
                .heightIn(min = 52.dp)
                .clip(shape)
                .background(containerColor)
                .then(
                    if (!isPrimary) {
                        Modifier.border(BorderStroke(1.dp, colors.interactiveOutline), shape)
                    } else {
                        Modifier
                    },
                ).clickable(
                    enabled = enabled,
                    role = Role.Button,
                    onClick = onClick,
                ).semantics(mergeDescendants = true) {
                    role = Role.Button
                    if (!enabled) disabled()
                }.padding(horizontal = 8.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = text,
            modifier = Modifier.fillMaxWidth(),
            color = contentColor,
            textAlign = TextAlign.Center,
            softWrap = true,
            style =
                CareerCompassTheme.typography.labelMedium.copy(
                    fontSize = 16.sp,
                    lineHeight = 24.sp,
                    fontWeight = FontWeight.SemiBold,
                ),
        )
    }
}
